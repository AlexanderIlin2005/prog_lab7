package common.requests;

public class RegisterRequest extends Request{
    private final String login;
    private final String password;

    public RegisterRequest(String name, String login, String password) {
        super(name);
        this.login = login;
        this.password = password;
    }
}