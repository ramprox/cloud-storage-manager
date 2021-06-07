package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.*;
import server.handlers.*;
import server.model.User;
import server.util.DBConnection;
import java.io.*;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный класс, запускающий работу сервера
 */
public class Server {

    private static final int PORT = 5678;
    public static final String SERVER_FOLDER = "storage";

    private static final ConcurrentHashMap<Channel, User> users = new ConcurrentHashMap<>();

    public static User getUserByLogin(String login) {
        for(ConcurrentHashMap.Entry<Channel, User> entry : users.entrySet()) {
            if(entry.getValue().getLogin().equals(login)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static User getUserByChannel(Channel channel) {
        return users.get(channel);
    }

    public static void subscribeUser(Channel channel, User user) {
        users.put(channel, user);
    }

    public static void unsubscribeUser(Channel channel) {
        users.remove(channel);
        System.out.println("Client disconnected: " + channel.remoteAddress());
    }

    /**
     * Запуск сервера
     */
    public Server() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            createFolder();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(PORT)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new ObjectEncoder());
                            channel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                            channel.pipeline().addLast(new SignHandler());
                            channel.pipeline().addLast(new ChangeDirHandler());
                            channel.pipeline().addLast(new ChangeFileHandler());
                            channel.pipeline().addLast(new UploadHandler());
                            channel.pipeline().addLast(new DownloadHandler());
                            channel.pipeline().addLast(new SearchHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            System.out.println("Server started");
            DBConnection.getConnection();
            future.channel().closeFuture().sync();
        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            System.out.println("Соединение с базой данных разорвано");
        } finally {
            System.out.println("Server closed");
            DBConnection.closeConnection();
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    /**
     * Создание корневой директории сервера
     * @throws IOException может возникнуть при неудачном создании директории
     */
    private void createFolder() throws IOException {
        new File(SERVER_FOLDER).mkdir();
    }
}
