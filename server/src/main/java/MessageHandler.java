import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



public class MessageHandler extends ChannelInboundHandlerAdapter {
 private UserLogin user;
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
                        //инициализируем
                        user = new UserLogin(message.getLogin());

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
                ServerOtherFunction.fileMessageMethod(fileMessage.getLogin(),
                        fileMessage.getFileName(),
                        fileMessage.getData(),
                        fileMessage.isDirectory());
                ctx.writeAndFlush(new SynchroMessage(ServerOtherFunction.getFilesFromCloud(fileMessage.getLogin())));

            } else if (msg instanceof SynchroMessage){
                SynchroMessage message = (SynchroMessage) msg;

                //Если убрать строчку сиаута, то всё работае
                System.out.println(user.getLogin());
                //
                Path paths = Paths.get("server/storage/" + message.getLogin());
                if(!Files.exists(paths)){
                    Files.createDirectory(paths);
                }

                ctx.writeAndFlush(new SynchroMessage(ServerOtherFunction.getFilesFromCloud(message.getLogin())));
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
                ctx.writeAndFlush(new MessageGetListLocalOrCloud(ServerOtherFunction.getFilesFromCloud(message.getLogin())));
            } else if (msg instanceof CloudSynchroMessage){
                CloudSynchroMessage message = (CloudSynchroMessage) msg;
                ctx.writeAndFlush(new UpdateMessageCloud(ServerOtherFunction.getFilesFromCloud(message.getLogin())));
            } else if (msg instanceof DeleteInCloudMessage) {
                DeleteInCloudMessage message = (DeleteInCloudMessage) msg;
                for (int i = 0; i < message.getArray().size(); i++) {
                    File fileToDelete = new File(message.getArray().get(i).getAbsolutePath());
                    if (fileToDelete.isDirectory()) {
                        ServerOtherFunction.deleteRecursively(fileToDelete);
                    } else {
                        fileToDelete.delete();
                    }
                }
                message.getArray().clear();

                if (message.getArray().isEmpty()) {
                    ctx.writeAndFlush(new SynchroMessage(ServerOtherFunction.getFilesFromCloud(message.getLogin())));
                } else {
                    ctx.writeAndFlush("DeletionFailure");
                }
            }

        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        AuthService.disconnect();
        ctx.close();
    }

    public class UserLogin {
        private String login;

        public UserLogin(String login) {
            this.login = login;
        }

        public String getLogin() {
            return login;
        }

    }



}