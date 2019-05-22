public class CommandMessage extends AbstractMessage{
    public enum Command{
        AUTH_SUCCESS,
        AUTH_FAILED,
        AUTH_NO_SUCH_USER,

        REG_ALREADY_EXIST,
        REG_SUCCESS,

        SYNCH_LOCAL,
        SYNCH_CLOUD
    }

    private Command command;
    private String msg;

    public CommandMessage (Command command, String msg) {
        this.command = command;
        this.msg = msg;
    }

    public CommandMessage (Command commandType) {
        this.command = commandType;
    }


    public String getMsg() {
        return msg;
    }

    public Command getCommand() {
        return command;
    }
}
