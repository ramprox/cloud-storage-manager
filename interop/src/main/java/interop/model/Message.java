package interop.model;

import interop.Command;

import java.io.Serializable;

public class Message implements Serializable {
    private Command command;
    private Object data;

    public Message(Command command, Object data) {
        this.command = command;
        this.data = data;
    }

    public Command getCommand() {
        return command;
    }

    public Object getData() {
        return data;
    }
}
