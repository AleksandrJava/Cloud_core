import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class ServerOtherFunction {

    public static void deleteRecursively(File f) throws Exception {
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    deleteRecursively(c);
                }
            }
            if (!f.delete()) {
                throw new Exception("Delete command returned false for file: " + f);
            }
        } catch (Exception e) {
            throw new Exception("Failed to delete the folder: " + f, e);
        }
    }

    public static ArrayList getFilesFromCloud(String login){
        ArrayList<File> cloud = new ArrayList<>();
        File path = new File("server/storage/" + login);
        File[] files = path.listFiles();
        if (files.length == 0) {
            cloud.clear();
        } else {
            cloud.clear();
            for (int i = 0; i < files.length; i++) {
                cloud.add(files[i]);
            }
        }
        return cloud;
    }

    public static void fileMessageMethod(String login, String fileName, byte[] data, boolean isDirectory) {
        try {
            Path pathToNewFile = Paths.get("server/storage/" + login + File.separator + fileName);
            if (isDirectory) {
                if (Files.exists(pathToNewFile)) {
                    System.out.println("Файл с таким именем уже существует");
                } else {
                    Files.createDirectory(pathToNewFile);
                }
            } else {
                if (Files.exists(pathToNewFile)) {
                    System.out.println("Файл с таким именем уже существует");
                } else {
                    Path paths = Paths.get("server/storage/" + login);
                    if (!Files.exists(paths)) {
                        Files.createDirectory(paths);
                    }
                    Files.write(Paths.get("server/storage/" + login + File.separator + fileName), data, StandardOpenOption.CREATE);

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
