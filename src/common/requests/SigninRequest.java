package common.requests;

public class SigninRequest extends Request{
    private final String login;
    private final String password;

    public SigninRequest(String login, String hashedpassword) {
        super("signin");
        this.login = login;
        this.password = hashedpassword;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
