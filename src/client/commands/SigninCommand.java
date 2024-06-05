package client.commands;

import client.TCPClient;
import client.utility.Console;
import common.lambdaworks.crypto.SCryptUtil;
import common.requests.SigninRequest;

import java.io.IOException;
import java.util.Scanner;

public class SigninCommand extends Command {

    private final Console console;
    private final TCPClient tcpClient;
    public SigninCommand(Console console, TCPClient tcpClient) {
        super("signin", "войти");
        this.tcpClient = tcpClient;
        this.console = console;
    }

    @Override
    public boolean apply(String[] arguments) throws IOException {

        Scanner scanner = new Scanner(System.in);
        console.print("Введите login/email: ");
        String login = scanner.nextLine();

        System.out.print("Введите пароль: ");
        String generatedSecuredPasswordHash = SCryptUtil.scrypt(scanner.nextLine(), 32, 32, 32);
        console.println("Запрос входа отправлен на сервер");
        tcpClient.sendCmdRequest(new SigninRequest(login, generatedSecuredPasswordHash));
        return true;
    }
}
