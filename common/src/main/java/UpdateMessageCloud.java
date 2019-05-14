import java.io.File;
import java.util.ArrayList;

public class UpdateMessageCloud extends AbstractMessage {
    private ArrayList<File> content;
    private String login;

    public UpdateMessageCloud(ArrayList<File> storage) {
        this.content = storage;
    }
    public UpdateMessageCloud(String login){
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public ArrayList<File> getCloudStorageContents() {
        return content;
    }

}
