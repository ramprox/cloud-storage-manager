package server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.exceptions.HandleException;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Главный класс, запускающий работу сервера
 */
@Service
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final ServerBootstrap serverBootstrap;

    private final String rootFolder;

    private Thread thread;

    public Server(ServerBootstrap serverBootstrap, @Value("${rootFolder}") String rootFolder) {
       this.serverBootstrap = serverBootstrap;
       this.rootFolder = rootFolder;
    }

    /**
     * Запуск
     */
    public void start() {
        thread = Thread.currentThread();
        createFolder();
        starting();
    }

    private void starting() {
        try {
            ChannelFuture future = serverBootstrap.bind().sync();
            int port = ((InetSocketAddress)future.channel().localAddress()).getPort();
            logger.info("Сервер запустился на порту: {}", port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            logger.info("Работа сервера прервана");
        } finally {
            serverBootstrap.config().group().shutdownGracefully();
            serverBootstrap.config().childGroup().shutdownGracefully();
            logger.info("Сервер остановлен");
        }
    }

    /**
     * Остановка
     */
    @PreDestroy
    public void close() {
        thread.interrupt();
    }

    /**
     * Создание корневой директории
     */
    private void createFolder() {
        try {
            Path rootFolderPath = Paths.get(rootFolder);
            if(!Files.exists(rootFolderPath)) {
                Files.createDirectory(rootFolderPath);
            }
        } catch (IOException ex) {
            logger.error("Не могу создать директорию {}. {}", rootFolder, ex.getMessage());
            throw new HandleException(ex.getMessage());
        }
    }

}
