package server.network.service;

import interop.model.DownloadOperation;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.model.ChannelAttributes;
import server.model.User;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Channel, ChannelAttributes> channelAttributes = new ConcurrentHashMap<>();

    private final String rootFolder;

    private final String rootSymbol;

    @Autowired
    public UserServiceImpl(@Value("${rootFolder}") String rootFolder,
                           @Value("${userRootSymbol}") String rootSymbol) {
        this.rootFolder = rootFolder;
        this.rootSymbol = rootSymbol;
    }

    @Override
    public User getUserByLogin(String login) {
        for(ConcurrentHashMap.Entry<Channel, ChannelAttributes> entry : channelAttributes.entrySet()) {
            if(entry.getValue().getUser().getLogin().equals(login)) {
                return entry.getValue().getUser();
            }
        }
        return null;
    }

    @Override
    public User getUserByChannel(Channel channel) {
        return channelAttributes.get(channel).getUser();
    }

    @Override
    public void subscribeUser(Channel channel, User user) {
        channelAttributes.put(channel, new ChannelAttributes(user));
    }

    @Override
    public void unsubscribeUser(Channel channel) {
        channelAttributes.remove(channel);
    }

    @Override
    public Path getHomeDir(Channel channel) {
        User user = getUserByChannel(channel);
        return Paths.get(rootFolder, user.getLogin()).toAbsolutePath();
    }

    @Override
    public Path convertRequestedPathToLocal(String path, Channel channel) {
        return Paths.get(path.replace(rootSymbol, getHomeDir(channel).toString()));
    }

    @Override
    public String convertLocalPathToClient(Path path, Path homeDir) {
        return rootSymbol + homeDir.relativize(path);
    }

    @Override
    public Queue<DownloadOperation> getQueueByChannel(Channel channel) {
        return channelAttributes.get(channel).getOperations();
    }
}
