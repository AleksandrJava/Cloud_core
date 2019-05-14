import java.io.File;
import java.util.ArrayList;

public class MessageGetListLocalOrCloud extends AbstractMessage {

    private ArrayList<File> content;
    private String login;

    public MessageGetListLocalOrCloud(ArrayList<File> storage) {
        this.content = storage;
    }
    public MessageGetListLocalOrCloud(String login){
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public ArrayList<File> getCloudStorageContents() {
        return content;
    }

}
