import java.io.File;

public class FileInStorage {
    private String name;
    private File path;
    private long size;



    private boolean choose;

    public FileInStorage(String name, File path, long size, boolean choose) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.choose = choose;
    }

    public String getName() {
        return name;
    }

    public File getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public boolean isChoose() {
        return choose;
    }
    public void setChoose(boolean choose) {
        this.choose = choose;
    }
}
