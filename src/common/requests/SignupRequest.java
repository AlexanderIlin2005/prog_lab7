package common.requests;

public class SignupRequest extends Request{
    private final String login;
    private final String password;

    public SignupRequest(String login, String hashedpassword) {
        super("signup");
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
