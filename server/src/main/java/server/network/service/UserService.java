package server.network.service;

import interop.model.DownloadOperation;
import io.netty.channel.Channel;
import server.model.User;

import java.nio.file.Path;
import java.util.Queue;

public interface UserService {

    User getUserByLogin(String login);

    User getUserByChannel(Channel channel);

    void subscribeUser(Channel channel, User user);

    void unsubscribeUser(Channel channel);

    Path getHomeDir(Channel channel);

    Path convertRequestedPathToLocal(String path, Channel channel);

    String convertLocalPathToClient(Path path, Path homeDir);

    Queue<DownloadOperation> getQueueByChannel(Channel channel);

}
