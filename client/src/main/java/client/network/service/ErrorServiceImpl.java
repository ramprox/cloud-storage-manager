package client.network.service;

import client.network.annotations.ResponseHandler;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ErrorServiceImpl implements ErrorService {

    private final ServerEventsListener serverEventsListener;

    @Autowired
    public ErrorServiceImpl(ServerEventsListener serverEventsListener) {
        this.serverEventsListener = serverEventsListener;
    }

    @Override
    @ResponseHandler(command = Command.ERROR)
    public void errorResponse(Message message) {
        serverEventsListener.errorReceived((String) message.getData());
    }
}
