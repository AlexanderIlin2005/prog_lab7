package client.commands;

import client.TCPClient;
import client.utility.Console;
import common.lambdaworks.crypto.SCryptUtil;
import common.requests.SignupRequest;

import java.io.IOException;
import java.util.Scanner;

public class SignupCommand extends Command{
    private final Console console;
    private final TCPClient tcpClient;
    public SignupCommand(Console console, TCPClient tcpClient) {
        super("signup", "зарегистрироваться");
        this.tcpClient = tcpClient;
        this.console = console;
    }

    @Override
    public boolean apply(String[] arguments) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String login;
        String generatedSecuredPasswordHash;
        //String generatedSecuredPasswordHash2;

        console.print("Введите login/email: ");
        login = scanner.nextLine();

        while (true) {

            System.out.println("Введите пароль: ");
            String s1 = scanner.nextLine().trim();

            generatedSecuredPasswordHash = SCryptUtil.scrypt(s1, 32, 32, 32);

            System.out.println("Повторите пароль: ");
            String s2 = scanner.nextLine().trim();

            if (SCryptUtil.check(s2, generatedSecuredPasswordHash)) {
                console.println("Запрос регистрации отправлен на сервер");
                break;
            } else {
                console.println("Пароли не совпадают! Попробуйте еще раз");
            }
        }
        tcpClient.sendCmdRequest(new SignupRequest(login, generatedSecuredPasswordHash));
        return true;
    }
}
