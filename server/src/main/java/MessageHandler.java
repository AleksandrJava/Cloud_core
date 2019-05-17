import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;



public class MessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        } else {
            if (msg instanceof MessageAuthentification) {
                MessageAuthentification message = (MessageAuthentification) msg;
                AuthService.connect();
                if (AuthService.checkHaveThisUser(message.getLogin())) {
                    if (AuthService.checkPassword(message.getLogin(), message.getPassword())) {
                        ctx.writeAndFlush("UserExist/" + message.getLogin());
                    } else {
                        ctx.writeAndFlush("WrongPassword");
                    }
                } else {
                    ctx.writeAndFlush("UserNoExist");
                }
                AuthService.disconnect();
            } else if (msg instanceof MessageRegistration) {
                MessageRegistration message = (MessageRegistration) msg;
                AuthService.connect();
                if (AuthService.checkHaveThisUser(message.getLogin())) {
                    ctx.writeAndFlush("userAlreadyRegistered");
                } else {
                    AuthService.addUser(message.getLogin(), message.getPassword());
                    ctx.writeAndFlush("registrationIsSuccessful");
                }
                AuthService.disconnect();
            } else if (msg instanceof FileMessage) {
                FileMessage fileMessage = (FileMessage) msg;
                System.out.println("name is " + fileMessage.getFileName());
                Path pathToNewFile = Paths.get("server/storage/" + fileMessage.getLogin() + File.separator + fileMessage.getFileName());
                if (fileMessage.isDirectory()) {
                    if (Files.exists(pathToNewFile)) {
                        System.out.println("Файл с таким именем уже существует");
                    } else {
                        Files.createDirectory(pathToNewFile);
                    }
                } else {
                    if (Files.exists(pathToNewFile)) {
                        System.out.println("Файл с таким именем уже существует");
                    } else {
                        Path paths = Paths.get("server/storage/" + fileMessage.getLogin());
                        if(!Files.exists(paths)){
                            Files.createDirectory(paths);
                        }
                        Files.write(Paths.get("server/storage/" + fileMessage.getLogin() + File.separator + fileMessage.getFileName()), fileMessage.getData(), StandardOpenOption.CREATE);
                    }

                }
                ctx.writeAndFlush(new SynchroMessage(getFilesFromCloud(fileMessage.getLogin())));


            } else if (msg instanceof SynchroMessage){
                SynchroMessage message = (SynchroMessage) msg;

                Path paths = Paths.get("server/storage/" + message.getLogin());
                if(!Files.exists(paths)){
                    Files.createDirectory(paths);
                }

                ctx.writeAndFlush(new SynchroMessage(getFilesFromCloud(message.getLogin())));
            } else if (msg instanceof FileRequest) {
                FileRequest message = (FileRequest) msg;

                for (int i = 0; i < message.getFiles().size(); i++) {
                    File file = new File(message.getFiles().get(i).getAbsolutePath());
                    Path path = Paths.get(message.getFiles().get(i).getAbsolutePath());
                    try {
                        if (file.isDirectory()) {
                            if (file.listFiles().length == 0) {
                                ctx.writeAndFlush(new FileMessage(file.getName(), true, true));
                            } else {
                                ctx.writeAndFlush(new FileMessage(file.getName(), true, false));
                            }
                        } else {
                            ctx.writeAndFlush(new FileMessage(path));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            } else if (msg instanceof LocalAndCloudSynchroMessage){
                LocalAndCloudSynchroMessage message = (LocalAndCloudSynchroMessage) msg;
                ctx.writeAndFlush(new MessageGetListLocalOrCloud(getFilesFromCloud(message.getLogin())));
            } else if (msg instanceof CloudSynchroMessage){
                CloudSynchroMessage message = (CloudSynchroMessage) msg;
                ctx.writeAndFlush(new UpdateMessageCloud(getFilesFromCloud(message.getLogin())));
            } else if (msg instanceof DeleteInCloudMessage) {
                DeleteInCloudMessage message = (DeleteInCloudMessage) msg;
                for (int i = 0; i < message.getArray().size(); i++) {
                    File fileToDelete = new File(message.getArray().get(i).getAbsolutePath());
                    if (fileToDelete.isDirectory()) {
                        deleteRecursively(fileToDelete);
                    } else {
                        fileToDelete.delete();
                    }
                }
                message.getArray().clear();

                if (message.getArray().isEmpty()) {
                    ctx.writeAndFlush(new SynchroMessage(getFilesFromCloud(message.getLogin())));
                } else {
                    ctx.writeAndFlush("DeletionFailure");
                }
            }

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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        AuthService.disconnect();
        ctx.close();
    }


}