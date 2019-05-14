import java.io.File;
import java.util.ArrayList;

public class DeleteInCloudMessage extends AbstractMessage{
    private String login;
    private ArrayList<File> array;

    public DeleteInCloudMessage(String login, ArrayList<File> array) {
        this.login = login;
        this.array = array;
    }

    public String getLogin() {
        return login;
    }

    public ArrayList<File> getArray() {
        return array;
    }
}
