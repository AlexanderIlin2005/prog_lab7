package server.commands;

import common.Responce;
import common.requests.Request;
import common.requests.SigninRequest;
import server.managers.UserManager;

import java.io.IOException;

public class SigninCommand extends Command{


    private final UserManager userManager;

    public SigninCommand(UserManager userManager){
        super("signin", "войти");
        this.userManager = userManager;
    }

    public Responce applySp(SigninRequest request) {
        boolean result = userManager.authenticateUser(request.getLogin(), request.getPassword());
        Responce res = new Responce();
        if (result) {
            res.addString("Вход на сервер произошел успешно");
        } else res.addString("Неверные логин и/или пароль. В доступе отказано");
        return res;
    }

    @Override
    public Responce apply(Request request) throws IOException {
        return applySp((SigninRequest) request);
    }
}
