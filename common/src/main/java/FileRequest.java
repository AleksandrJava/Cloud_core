import java.io.File;
import java.util.ArrayList;

public class FileRequest extends AbstractMessage{
    private ArrayList<File> files;

    public FileRequest(ArrayList<File> files) {
        this.files = files;
    }

    public ArrayList<File> getFiles() {
        return files;
    }

}
