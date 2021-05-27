package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import server.handlers.AuthInboundHandler;
import server.model.User;
import server.service.DBConnection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5678;

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
    }

    /**
     * Запуск сервера
     * Изначально в pipeline добавляются три обработчика:
     *     StringEncoder
     *     StringDecoder
     *     AuthInboundHandler
     *
     *  Если клиент успешно проходит аутентификацию, то AuthInboundHandler удаляется и вместо
     *  него добавляется CommandInboundHandler
     */
    public Server() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(PORT)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast("stringEncoder", new StringEncoder());
                            channel.pipeline().addLast("stringDecoder", new StringDecoder());
                            channel.pipeline().addLast("authInboundHandler", new AuthInboundHandler());
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
}
