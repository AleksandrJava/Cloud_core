import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


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
                        ctx.writeAndFlush("User exist/" + message.getLogin());
                        System.out.println(message.getLogin() +" " + message.getPassword());
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
                }
                AuthService.disconnect();
            }
        }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        AuthService.disconnect();
        ctx.close();
    }


}