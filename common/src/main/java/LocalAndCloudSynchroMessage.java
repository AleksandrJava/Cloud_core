public class LocalAndCloudSynchroMessage extends AbstractMessage {
    String login;

    public LocalAndCloudSynchroMessage(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
