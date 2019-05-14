import java.io.File;
import java.util.ArrayList;

public class SynchroMessage extends AbstractMessage{
    private ArrayList<File> content;
    private String login;

    public SynchroMessage(ArrayList<File> storage) {
        this.content = storage;
    }
    public SynchroMessage(String login){
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public ArrayList<File> getCloudStorageContents() {
        return content;
    }
}
