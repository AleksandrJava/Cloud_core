import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;



public class MessageHandler extends ChannelInboundHandlerAdapter {
 private UserLogin user;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg == null) {
            return;
        } else {
            if (msg instanceof CommandMessage) {
                switch (((CommandMessage) msg).getCommand()) {
                    case SYNCH_LOCAL:
                        ctx.writeAndFlush(new MessageGetListLocalOrCloud(ServerOtherFunction.getFilesFromCloud(((CommandMessage) msg).getMsg())));
                        break;
                    case SYNCH_CLOUD:
                        ctx.writeAndFlush(new UpdateMessageCloud(ServerOtherFunction.getFilesFromCloud(((CommandMessage) msg).getMsg())));
                        break;
                }

            } else if(msg instanceof MessageAuthentification) {
                MessageAuthentification message = (MessageAuthentification) msg;
                if (AuthService.checkHaveThisUser(message.getLogin())) {
                    user = AuthService.checkPassword(message.getLogin(), message.getPassword());
                    System.out.println(user.getLogin());
                    if (user != null) {
                        ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.AUTH_SUCCESS, message.getLogin()));
                    } else {
                        ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.AUTH_FAILED));
                    }
                } else {
                    ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.AUTH_NO_SUCH_USER));
                }

            } else if (msg instanceof MessageRegistration) {
                MessageRegistration message = (MessageRegistration) msg;
                if (AuthService.checkHaveThisUser(message.getLogin())) {
                    ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.REG_ALREADY_EXIST));
                } else {
                    AuthService.addUser(message.getLogin(), message.getPassword());
                    ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.REG_SUCCESS, message.getLogin()));
                }

            } else if (msg instanceof FileMessage) {
                FileMessage fileMessage = (FileMessage) msg;
                ServerOtherFunction.fileMessageMethod(fileMessage.getLogin(),
                        fileMessage.getFileName(),
                        fileMessage.getData(),
                        fileMessage.isDirectory());
                ctx.writeAndFlush(new SynchroMessage(ServerOtherFunction.getFilesFromCloud(fileMessage.getLogin())));

            } else if (msg instanceof SynchroMessage){
                SynchroMessage message = (SynchroMessage) msg;

                //Если убрать строчку sout, то всё работает, то есть
                //user почему-то не инициализирован
                //System.out.println(user.getLogin());
                //
                ServerOtherFunction.synchroMessageMethod(message.getLogin());

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
            } else if (msg instanceof DeleteInCloudMessage) {
                DeleteInCloudMessage message = (DeleteInCloudMessage) msg;
                for (int i = 0; i < message.getArray().size(); i++) {
                    ServerOtherFunction.deleteMethod(message.getArray().get(i).getAbsolutePath());
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

}