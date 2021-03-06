public class MessageRegistration extends AbstractMessage {
    private String login;
    private String password;

    public MessageRegistration(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
