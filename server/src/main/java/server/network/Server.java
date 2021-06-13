package server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.*;
import server.network.handlers.*;
import server.model.User;
import server.util.*;
import java.io.*;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный класс, запускающий работу сервера
 */
public class Server {

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
        if(users.remove(channel) != null) {
            System.out.println("Client disconnected: " + channel.remoteAddress());
        }
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
                    .localAddress(ApplicationUtil.PORT)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new ObjectEncoder());
                            channel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(ClassLoader.getSystemClassLoader())));
                            channel.pipeline().addLast(new SignHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            System.out.println("Server started");
            DBConnection.getConnection();
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
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
     */
    private void createFolder() {
        new File(ApplicationUtil.SERVER_FOLDER).mkdir();
    }
}
