public class CloudSynchroMessage extends AbstractMessage{
    String login;

    public CloudSynchroMessage(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
