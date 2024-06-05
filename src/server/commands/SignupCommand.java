package server.commands;

import common.Responce;
import common.requests.Request;
import common.requests.SigninRequest;
import common.requests.SignupRequest;
import server.managers.UserManager;

import java.io.IOException;

public class SignupCommand extends Command{


    private final UserManager userManager;

    public SignupCommand(UserManager userManager){
        super("signup", "войти");
        this.userManager = userManager;
    }

    public Responce applySp(SignupRequest request) {
        boolean result = userManager.registerUser(request.getLogin(), request.getPassword());
        Responce res = new Responce();
        if (result) {
            res.addString("Регистрация прошла успешно");
        } else res.addString("Пользователь с такими логином и паролем уже есть");
        return res;
    }

    @Override
    public Responce apply(Request request) throws IOException {
        return applySp((SignupRequest) request);
    }
}

