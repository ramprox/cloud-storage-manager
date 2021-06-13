package interop.model;

import interop.Command;

import java.io.Serializable;

/**
 * Класс для обмена сообщениями между сервером и клиентом
 * поле command содержит команду
 * поле data содержит данные для обмена
 */
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
