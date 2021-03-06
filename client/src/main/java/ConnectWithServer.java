import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ConnectWithServer {

    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;

    public static void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(),150*1024*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static boolean sendAuthMessage(String login, String password) {
        try {
            out.writeObject(new MessageAuthentification(login,password));
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean sendRegMessage(String login, String password) {
        try {
            out.writeObject(new MessageRegistration(login,password));
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void transportFileToCloud(String login, ArrayList<File> files){
        try {
            if(!files.isEmpty()){
                for (int i = 0; i < files.size(); i++) {
                    Path path = Paths.get(files.get(i).getAbsolutePath());
                    File file = new File(files.get(i).getAbsolutePath());
                    if(file.isDirectory()){
                        String name = path.toString().substring(58);
                        System.out.println(name);
                        out.writeObject(new FileMessage(name, true));
                        out.flush();
                        takeFilesFromPackage(file, login);
                    } else {
                        out.writeObject(new FileMessage(login, path));
                        out.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void takeFilesFromPackage(File path, String login){
        try {
            File[] filesList = path.listFiles();
            if(filesList.length > 0){
                for (File a: filesList) {
                    Path pathToFile = Paths.get(a.getAbsolutePath());
                    File ifPackage = new File(a.getAbsolutePath());
                    if(ifPackage.isDirectory()){
                        String name = pathToFile.toString().substring(58);
                        System.out.println(name);
                        out.writeObject(new FileMessage(name, true));
                        out.flush();
                        takeFilesFromPackage(ifPackage, login);
                    } else {
                        out.writeObject(new FileMessage(login, pathToFile));
                        out.flush();
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void upgradeCloudStorage(String login){
        try {
            out.writeObject(new SynchroMessage(login));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendFileRequest(ArrayList<File> files){
        try {
            if(!files.isEmpty()){
                out.writeObject(new FileRequest(files));
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendSynchroFromCloud(String login){
        try {
            out.writeObject(new CommandMessage(CommandMessage.Command.SYNCH_LOCAL, login));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendSynchroFromLocal(String login){
        try {
            out.writeObject(new CommandMessage(CommandMessage.Command.SYNCH_CLOUD, login));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendDeleteMessage(String login, ArrayList<File> array){
        try {
            out.writeObject(new DeleteInCloudMessage(login, array));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static Object readInObject() throws IOException, ClassNotFoundException {
        Object object = in.readObject();
        return  object;
    }
}
