package interop.dto;

import interop.Command;

import java.io.Serializable;

public class Message implements Serializable {

    private Command command;

    private Object data;

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
