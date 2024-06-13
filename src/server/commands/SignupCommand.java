package server.commands;

import common.Responce;
import common.requests.Request;
import common.requests.SigninRequest;
import common.requests.SignupRequest;
import server.managers.UserManager;

import java.io.IOException;

import static server.TCPServer.currentUserIsAuthenticated;
import static server.TCPServer.currentUserLogin;

public class SignupCommand extends Command{


    private final UserManager userManager;

    public SignupCommand(UserManager userManager){
        super("signup", "войти");
        this.userManager = userManager;
    }

    public Responce applySp(SignupRequest request) {
        Responce res = new Responce();
        boolean result = userManager.registerUser(request.getLogin(), request.getPassword());
        if (result) {
            res.addString("Регистрация прошла успешно");
            currentUserIsAuthenticated.set(true);
            currentUserLogin.set(request.getLogin());
        } else res.addString("Пользователь с такими логином и паролем уже есть");
        return res;
    }

    @Override
    public Responce apply(Request request) throws IOException {
        return applySp((SignupRequest) request);
    }
}

