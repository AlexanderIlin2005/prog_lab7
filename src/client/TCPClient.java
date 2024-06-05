package client;

import common.InfoResponce;
import common.Responce;
import common.requests.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.*;


public class TCPClient {

    Socket clientSocket;
    BufferedReader reader;

    ObjectOutputStream out;
    ObjectInputStream in;

    public TCPClient(){}
    public void connect(){
        String host;
        int port;
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("Введите адрес сервера:");
                host = sc.nextLine();
                System.out.println("Введите порт сервера:");
                port = Integer.valueOf(sc.nextLine());
                clientSocket = new Socket(host, port);
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                break;

            }  catch (UnknownHostException e) {
                System.out.println("Неизвестный хост");
            } catch (IOException e) {
                System.out.println("Неверный формат ввода");
            }
        }
        System.out.println("Выполнено подключение к серверу "+ host + ":" + port);

        // вызов метода receiveResponse()
        Thread receiveThread = new Thread(this::receiveResponce);

        receiveThread.start();
        //receiveResponce();
    }

    public void sendCmdRequest(Request request) throws IOException {
        try {
            out.writeObject(request);
            out.flush();
            System.out.println("Запрос успешно отправлен на сервер");
        } catch (IOException e) {
            System.out.println("Связь с сервером прервана. Попробуйте подключиться снова");
            throw new IOException(e);
        }


        //receiveResponce();
        //receiveInfoResponce();
    }


    /*
    private void receiveInfoResponce() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                Responce responce = (InfoResponce) in.readObject();
                System.out.println(responce.getContent());
            } catch (IOException | ClassNotFoundException e) {
                System.out.println();
            }
        });

        try {
            future.get(10, TimeUnit.MILLISECONDS); // Указываем время ожидания в миллисекундах
        } catch (TimeoutException e) {
            future.cancel(true); // Прекращаем выполнение метода
            //System.out.println("Метод прерван по таймауту");
        } catch (InterruptedException | ExecutionException e) {
            // Обработка исключений
        }

        executor.shutdown();
    }

     */



    private void receiveResponce() {
        while (true) {
            try {
                System.out.println(">>");
                Responce responce = (Responce) in.readObject();
                System.out.println(responce.getContent());
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                throw new RuntimeException();
            }
        }
    }
}
