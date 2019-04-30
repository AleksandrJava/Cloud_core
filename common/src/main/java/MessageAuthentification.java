public class MessageAuthentification extends AbstractMessage {
    private String login;
    private String password;

    public MessageAuthentification(String login, String password) {
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
