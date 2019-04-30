import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class ConnectWithServer {

    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;

    public static void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(),20*1024*1024);
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
    public static Object readInObject() throws IOException, ClassNotFoundException {
        Object object = in.readObject();
        return  object;
    }
}
