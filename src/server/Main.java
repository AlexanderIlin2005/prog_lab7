package server;

import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

import BaseModel.City;
import server.managers.CollectionManager;
import server.managers.CommandManager;
import server.managers.UserManager;
import server.utility.StandardConsole;
import server.commands.*;


import static java.lang.System.exit;


public class Main {



    public static void main(String[] args) throws IOException {
        //ssh -p 2222 s381032@helios.cs.ifmo.ru -L 5432:localhost:5432
        //System.out.println("Количество ядер: " + Runtime.getRuntime().availableProcessors());
        Scanner sc = new Scanner(System.in);
        StandardConsole console = new StandardConsole();

        Connection connection = null;
        try {
            String url = "jdbc:postgresql://localhost:5432/studs";
            String user = "s381032";
            String password = "aIERPRFWna2YH9th";
            //Class.forName("org.postgresql.Driver"); // загружаем драйвер для PostgreSQL
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(false);
            System.out.println("Подключено к БД");
        } catch (Exception e) {
            System.out.println("Ошибка соединения с базой данных: " + e.getMessage());
        }

        CollectionManager collectionManager = new CollectionManager(console, connection);
        collectionManager.loadCollection();

        //City.updateNextId(collectionManager);
        //collectionManager.validateAll(console);
        System.out.println(28);

        UserManager userManager = new UserManager(connection);

        CommandManager commandManager = new CommandManager() {{
            register("clear", new Clear(console, collectionManager));
            register("count_by_meters_above_sea_level", new CountByMetersAboveSeaLevel(console, collectionManager));
            register("exit", new Exit(console));
            register("group_counting_by_area", new GroupCountingByArea(console, collectionManager));
            register("info", new Info(console, collectionManager));
            register("insert", new Insert(console, collectionManager));
            register("min_by_population", new MinByPopulation(console, collectionManager));
            register("remove_greater_key", new RemoveGreaterKey(console, collectionManager));
            register("remove_key", new RemoveKey(console, collectionManager));
            register("replace_if_greater", new ReplaceIfGreater(console, collectionManager));
            register("replace_if_lower", new ReplaceIfLower(console, collectionManager));
            register("show", new Show(console, collectionManager));
            register("update_id", new UpdateId(console, collectionManager));
            register("signin", new SigninCommand(userManager));
            register("signup", new SignupCommand(userManager));
        }};

        TCPServer server = new TCPServer(commandManager);

        Thread inputThread = new Thread(() -> {
            while (true) {
                String input = sc.nextLine();
                if (input.equals("exit")) {
                    System.out.println("exit");
                    collectionManager.saveCollection();
                    exit(0);
                } else if (input.startsWith("save")) {
                    collectionManager.saveCollection();
                }
            }
        });

        Thread serverThread = new Thread(() -> {
            try {
                server.connect();
                server.waitForConnection(); // ожидание нового пользователя
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        inputThread.start();


    }


}