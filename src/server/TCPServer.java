package server;

import common.InfoResponce;
import common.Responce;
import common.requests.Request;
import server.commands.Command;
import server.managers.CommandManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

public class TCPServer {

    public static ThreadLocal<Boolean> currentUserIsAuthenticated = ThreadLocal.withInitial(() -> false);
    public static ThreadLocal<String> currentUserLogin = ThreadLocal.withInitial(() -> "");

    private static final Logger logger = Logger.getLogger(TCPServer.class.getName());
    private static FileHandler fileHandler;

    private ServerSocket serverSocket;
    //private Socket clientSocket;
    private CommandManager commandManager;
    private BufferedReader reader;

    private List<Socket> clientSockets = new ArrayList<>();

    public TCPServer(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void connect() throws IOException {
        String host;
        int port;
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                //logger.log(Level.INFO, "Введите порт сервера:");
                System.out.print("Введите порт сервера:");
                port = Integer.valueOf(sc.nextLine());
                //System.out.println("порт введен");
                serverSocket = new ServerSocket(port);
                break;

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Неверный формат ввода", e);
            }
        }
        logger.log(Level.INFO, "Запущен сервер на порту " + port);

        // Создание FileHandler для записи логов в файл
        try {
            fileHandler = new FileHandler("src/server/log/server.log", true);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при создании FileHandler", e);
        }
    }

    public void waitForConnection() throws IOException {
        while (true) {
            Thread clientThread = new Thread(() -> {
                //boolean currentUserIsAuthentificated = false;
                //String currentUserLogin = "";

                currentUserIsAuthenticated.set(false);
                currentUserLogin.set("");


                //Thread.currentThread().
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    logger.log(Level.INFO, "Получено новое подключение: " + clientSocket.getInetAddress() + " " + clientSocket.getPort());
                    listenCommands(clientSocket);
                } catch (IOException e) {
                    //e.printStackTrace();
                    //отправить информационное сообщение всем пользователям
                    clientSockets.remove(clientSocket);

                } finally{
                    try {
                        // Закрытие сокета
                        clientSocket.close();
                        //sendToAllClients();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            clientThread.start();
        }
    }

    private void sendToAllClients() {
        Responce responce = new Responce();
        responce.addString("Какой-то пользователь был подключен к серверу");

        for (Socket socket : clientSockets) {
            try {
                System.out.println(socket);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(responce);
                out.flush();
                System.out.println("вроде как отправили, что " + responce.getContent());
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void listenCommands(Socket clientSocket) throws IOException {
        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

        try {
            while (true) {
                //sendToAllClients();
                Request request = (Request) in.readObject();
                String cmdName = request.getName();
                logger.log(Level.INFO, "Получен новый запрос: " + cmdName);
                Command command = commandManager.getCommands().get(cmdName);
                Responce response;
                if (currentUserIsAuthenticated.get()) {
                    response = command.apply(request);
                } else {
                    response = new Responce();
                    if (cmdName.equals("signin") || cmdName.equals("signup")){
                        response = command.apply(request);
                    } else response.addString("Не авторизованным пользователям запрещено выполнять команды! Войдите или зарегистрируйтесь");

                }
                out.writeObject(response);
                out.flush();
                logger.log(Level.INFO, "Отправлен ответ: " + response.getContent());
            }
        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            Responce response = new Responce();
            response.addString("Неверный формат введенных команд. Вы отключены от сервера. Используйте корректный клиент");
            out.writeObject(response);
            out.flush();
            logger.log(Level.INFO, "Закончена связь с текущим пользователем");
            // Новое подключение:
            //waitForConnection();
        }
    }
}