package server;

import common.Responce;
import common.requests.Request;
import server.commands.Command;
import server.managers.CommandManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.random;

public class TCPServer {

    public static ThreadLocal<Boolean> currentUserIsAuthenticated = ThreadLocal.withInitial(() -> false);

    public static ThreadLocal<Boolean> currentUserOnTimeout = ThreadLocal.withInitial(() -> false);
    public static ThreadLocal<String> currentUserLogin = ThreadLocal.withInitial(() -> "");

    private static final Logger logger = Logger.getLogger(TCPServer.class.getName());
    private static FileHandler fileHandler;

    private ServerSocket serverSocket;
    //private Socket clientSocket;
    private CommandManager commandManager;
    private BufferedReader reader;

    private List<ModifiedClientSocket> clientSockets = new ArrayList<ModifiedClientSocket>();

    // Создание пула потоков для чтения запросов
    private ForkJoinPool forkJoinPool;

    // Создание пула потоков для обработки запросов
    private ExecutorService commandExecutor = Executors.newCachedThreadPool();

    // Создание пула потоков для отправки ответов
    private ExecutorService responseExecutor = Executors.newCachedThreadPool();


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



    /*
    private void sendToAllClients(String message) {
        Responce responce = new Responce();
        responce.addString(message);

        for (ModifiedClientSocket socket : clientSockets) {
            try {
                System.out.println(socket);
                socket.writeObject(responce);

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

     */


    private class SendToClientsTask extends RecursiveAction {
        private static final int THRESHOLD = 2;

        private int start;
        private int end;
        private Responce responce;

        public SendToClientsTask(int start, int end, Responce responce) {
            this.start = start;
            this.end = end;
            this.responce = responce;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                for (int i = start; i < end; i++) {
                    ModifiedClientSocket socket = clientSockets.get(i);
                    try {
                        socket.writeObject(responce);
                        Thread.sleep(1);
                    } catch (IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } else {
                int middle = start + (end - start) / 2;
                SendToClientsTask task1 = new SendToClientsTask(start, middle, responce);
                SendToClientsTask task2 = new SendToClientsTask(middle, end, responce);
                invokeAll(task1, task2);
            }
        }
    }


    private void sendToAllClients(String message) {
        Responce responce = new Responce();
        responce.addString(message);

        int availableProcessors = Runtime.getRuntime().availableProcessors();

        if (clientSockets.size() <= availableProcessors) {
            SendToClientsTask task = new SendToClientsTask(0, clientSockets.size(), responce);
            task.compute();
        } else {
            int middle = clientSockets.size() / 2;
            SendToClientsTask task1 = new SendToClientsTask(0, middle, responce);
            SendToClientsTask task2 = new SendToClientsTask(middle, clientSockets.size(), responce);

            task1.compute();
            task2.compute();
        }
    }


/*

    public void waitForConnection() throws IOException {
        while (true) {
            Runnable clientTask = () -> {
                currentUserIsAuthenticated.set(false);
                currentUserLogin.set("");
                ModifiedClientSocket clientSocket = null;
                try {
                    clientSocket = new ModifiedClientSocket(serverSocket.accept());
                    clientSockets.add(clientSocket);
                    sendToAllClients("Неизвестный пользователь подключился к серверу");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    logger.log(Level.INFO, "Получено новое подключение: " + clientSocket.getSocket().getInetAddress() + " " +
                            clientSocket.getSocket().getPort());
                    listenCommands(clientSocket);
                } catch (IOException e) {
                    clientSockets.remove(clientSocket);

                } finally{
                    try {
                        // Закрытие сокета
                        clientSocket.getSocket().close();
                        //sendToAllClients();
                        if (!currentUserLogin.get().isEmpty()){
                            sendToAllClients("Пользователь " + currentUserLogin.get() + " отключился от сервера");
                        } else {
                            sendToAllClients("Неизвестный пользователь отключился от сервера");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread clientThread = new Thread(clientTask);
            clientThread.start();
        }
    }


    private void listenCommands(ModifiedClientSocket clientSocket) throws IOException {

        try { while (true) {
            Request request = (Request) clientSocket.readObject();
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
                    if (!currentUserLogin.get().isEmpty()){
                        sendToAllClients("Пользователь " + currentUserLogin.get() + " прошел авторизацию на сервере");
                    }
                } else response.addString("Не авторизованным пользователям запрещено выполнять команды! Войдите или зарегистрируйтесь");

            }

            clientSocket.writeObject(response);
            logger.log(Level.INFO, "Отправлен ответ: " + response.getContent());
        }
        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            Responce response = new Responce();
            response.addString("Неверный формат введенных команд. Вы отключены от сервера. Используйте корректный клиент");
            clientSocket.writeObject(response);
            logger.log(Level.INFO, "Закончена связь с текущим пользователем");

        }
    }

 */


    /*
    public void waitForConnection() throws IOException {
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            executor.execute(() -> {
                currentUserIsAuthenticated.set(false);
                currentUserLogin.set("");
                ModifiedClientSocket clientSocket = null;
                try {
                    clientSocket = new ModifiedClientSocket(serverSocket.accept());
                    clientSockets.add(clientSocket);
                    sendToAllClients("Неизвестный пользователь подключился к серверу");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    logger.log(Level.INFO, "Получено новое подключение: " + clientSocket.getSocket().getInetAddress() + " " +
                            clientSocket.getSocket().getPort());
                    listenCommands(clientSocket);
                } catch (IOException e) {
                    clientSockets.remove(clientSocket);
                } finally {
                    try {
                        clientSocket.getSocket().close();
                        if (!currentUserLogin.get().isEmpty()) {
                            sendToAllClients("Пользователь " + currentUserLogin.get() + " отключился от сервера");
                        } else {
                            sendToAllClients("Неизвестный пользователь отключился от сервера");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

     */

    /*
    private void listenCommands(ModifiedClientSocket clientSocket) throws IOException {

        try {
            while (true) {
                Request request = (Request) clientSocket.readObject();
                String cmdName = request.getName();
                logger.log(Level.INFO, "Получен новый запрос: " + cmdName);
                Command command = commandManager.getCommands().get(cmdName);

                Responce response;
                if (currentUserIsAuthenticated.get()) {
                    response = command.apply(request);
                } else {
                    response = new Responce();
                    if (cmdName.equals("signin") || cmdName.equals("signup")) {
                        response = command.apply(request);
                        if (!currentUserLogin.get().isEmpty()) {
                            sendToAllClients("Пользователь " + currentUserLogin.get() + " прошел авторизацию на сервере");
                        }
                    } else {
                        response.addString("Не авторизованным пользователям запрещено выполнять команды! Войдите или зарегистрируйтесь");
                    }
                }

                clientSocket.writeObject(response);
                logger.log(Level.INFO, "Отправлен ответ: " + response.getContent());
            }
        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            Responce response = new Responce();
            response.addString("Неверный формат введенных команд. Вы отключены от сервера. Используйте корректный клиент");
            clientSocket.writeObject(response);
            logger.log(Level.INFO, "Закончена связь с текущим пользователем");
        }
    }

     */


    public void waitForConnection() throws IOException {
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            executor.execute(() -> {
                currentUserIsAuthenticated.set(false);
                currentUserLogin.set("");
                ModifiedClientSocket clientSocket = null;
                try {
                    //new Timer("random timer " + random());
                    clientSocket = new ModifiedClientSocket(serverSocket.accept());
                    clientSockets.add(clientSocket);
                    sendToAllClients("Неизвестный пользователь подключился к серверу");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    logger.log(Level.INFO, "Получено новое подключение: " + clientSocket.getSocket().getInetAddress() + " " +
                            clientSocket.getSocket().getPort());
                    listenCommands(clientSocket);
                } catch (IOException e) {
                    clientSockets.remove(clientSocket);
                } finally {
                    try {
                        clientSockets.remove(clientSocket);
                        clientSocket.getSocket().close();
                        if (!currentUserLogin.get().isEmpty()) {
                            sendToAllClients("Пользователь " + currentUserLogin.get() + " отключился от сервера" + (currentUserOnTimeout.get() ? " по таймауту" : ""));
                        } else {
                            sendToAllClients("Неизвестный пользователь отключился от сервера" + (currentUserOnTimeout.get() ? " по таймауту" : ""));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void listenCommands(ModifiedClientSocket clientSocket) throws IOException {

        try {
            while (true) {
                Request request = (Request) clientSocket.readObject();
                String cmdName = request.getName();
                logger.log(Level.INFO, "Получен новый запрос: " + cmdName);
                Command command = commandManager.getCommands().get(cmdName);

                Responce response;
                if (currentUserIsAuthenticated.get()) {
                    response = command.apply(request);
                } else {
                    response = new Responce();
                    if (cmdName.equals("signin") || cmdName.equals("signup")) {
                        response = command.apply(request);
                        if (!currentUserLogin.get().isEmpty()) {
                            sendToAllClients("Пользователь " + currentUserLogin.get() + " прошел авторизацию на сервере");
                        }
                    } else {
                        response.addString("Не авторизованным пользователям запрещено выполнять команды! Войдите или зарегистрируйтесь");
                    }
                }

                clientSocket.writeObject(response);
                logger.log(Level.INFO, "Отправлен ответ: " + response.getContent());
            }

        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            //e.printStackTrace();
            Responce response = new Responce();
            response.addString("Вы были отключены от сервера за долгое бездействие. Требуется переподключение");
            clientSocket.writeObject(response);
            currentUserOnTimeout.set(true);
            logger.log(Level.INFO, "Закончена связь с текущим пользователем");
        }
    }
    /*
    class ConnectionTimer {
        private Timer timer;

        public ConnectionTimer(int timeout, Runnable onTimeout) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    onTimeout.run();
                }
            }, timeout);
        }

        public void resetTimer() {
            timer.cancel();
            timer = new Timer(true);
        }

        public void stopTimer() {
            timer.cancel();
        }
    }

     */


    
}





//2000 - 68000





