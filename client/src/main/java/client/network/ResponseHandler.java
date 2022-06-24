package client.network;

import interop.dto.Message;

public interface ResponseHandler {

    void handleResponse(Message message);

    void channelActivated();

    void channelInactivated();

}
