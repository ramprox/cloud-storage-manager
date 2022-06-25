package server.model;

import interop.model.DownloadOperation;

import java.util.LinkedList;
import java.util.Queue;

public class ChannelAttributes {

    private final User user;

    private final Queue<DownloadOperation> operations = new LinkedList<>();

    public ChannelAttributes(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Queue<DownloadOperation> getOperations() {
        return operations;
    }

}
