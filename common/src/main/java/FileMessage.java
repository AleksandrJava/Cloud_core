import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileMessage extends AbstractMessage{
    private String login;
    private String name;
    private byte[] arr;
    private boolean isDirectory;
    private boolean isEmpty;
    private ArrayList<Path> filesInPackage;

    public FileMessage(Path path) throws IOException {
        name = path.getFileName().toString();
        arr = Files.readAllBytes(path);
        this.isDirectory = false;
        this.isEmpty = false;
    }
    public FileMessage(String fileName, boolean isDirectory, boolean isEmpty){
        this.name = fileName;
        this.isDirectory = isDirectory;
        this.isEmpty = isEmpty;
    }
    public FileMessage(String login, Path path) throws IOException{
        name = path.getFileName().toString();
        arr = Files.readAllBytes(path);
        this.login = login;
    }
    public FileMessage(String fileName, boolean isDirectory){
        this.isDirectory = isDirectory;
        this.name = fileName;
    }

    public String getFileName() {
        return name;
    }

    public byte[] getData() {
        return arr;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public String getLogin() {
        return login;
    }
}
