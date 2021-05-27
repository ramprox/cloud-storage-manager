package client.network;

import client.interfaces.Callback0;
import client.interfaces.Callback1;
import client.network.handlers.*;
import client.interfaces.Callback;
import client.model.FileInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

public class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 5678;

    private Channel channel;
    private Callback0 channelActive;
    private Callback0 clientAuthorizedCallback;
    private Callback<String> authErrorCallback;
    private Callback1<FileInfo[], String> readedFileInfoFromServer;
    private Callback<FileInfo> createdDirOnServer;
    private Callback<FileInfo> createdFileOnServer;
    private Callback<FileInfo> renamedFileOnServer;
    private Callback0 deletedFileOnServer;
    private StringEncoder stringEncoder;
    private StringDecoder stringDecoder;
    private AuthInboundHandler authInboundHandler;
    private FileWalksHandler fileWalksHandler;
    private CommandHandler commandHandler;

    public Client() {
        stringEncoder = new StringEncoder();
        stringDecoder = new StringDecoder();
        authInboundHandler = new AuthInboundHandler();
        authInboundHandler.setAuthOKCallback(Client.this::authOKCallback);
        authInboundHandler.setErrorCallback(Client.this::authErrorCallback);
        authInboundHandler.setChannelActive(Client.this::channelActive);
        fileWalksHandler = new FileWalksHandler();
        fileWalksHandler.setReadedFileInfoFromServer(Client.this::readedFileInfoFromServer);
        commandHandler = new CommandHandler();
        commandHandler.setCreatedDir(Client.this::createdDirOnServer);
        commandHandler.setCreatedFile(Client.this::createdFileOnServer);
        commandHandler.setRenameFile(Client.this::renamedFileOnServer);
        commandHandler.setDeletedFile(Client.this::deletedFileOnServer);
    }

    public void setChannelActive(Callback0 channelActive) {
        this.channelActive = channelActive;
    }

    public void setDeletedFileOnServer(Callback0 deletedFileOnServer) {
        this.deletedFileOnServer = deletedFileOnServer;
    }

    public void setRenamedFileOnServer(Callback<FileInfo> renamedFileOnServer) {
        this.renamedFileOnServer = renamedFileOnServer;
    }

    public void setCreatedFileOnServer(Callback<FileInfo> createdFileOnServer) {
        this.createdFileOnServer = createdFileOnServer;
    }

    public void setCreatedDirOnServer(Callback<FileInfo> createdDirOnServer) {
        this.createdDirOnServer = createdDirOnServer;
    }

    public void setClientAuthorizedCallback(Callback0 clientAuthorizedCallback) {
        this.clientAuthorizedCallback = clientAuthorizedCallback;
    }

    public void setReadedFileInfo(Callback1<FileInfo[], String> readedFileInfoFromServer) {
        this.readedFileInfoFromServer = readedFileInfoFromServer;
    }

    public void setAuthErrorCallback(Callback<String> authErrorCallback) {
        this.authErrorCallback = authErrorCallback;
    }

    public void connect() {
        Thread thread = new Thread(() -> {
            try {
                start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }


    /**
     * Запуск соединения с сервером
     * Изначально в pipeline добавляются три обработчика:
     *     StringEncoder
     *     StringDecoder
     *     AuthInboundHandler
     *
     * Если клиент успешно проходит аутентификацию, то AuthInboundHandler удаляется.
     * Далее идет запрос на сервер на получение списка файлов в текущей директории на сервере.
     * Для этого вставляется FileWalksHandler, ответственный за декодирование списка
     * файлов от сервера (преобразование от String к FileInfo[]). После преобразования списка файлов
     * обработчик FileWalksHandler удаляется.
     *
     * Далее каждый раз при отправке на сервер команд по перемещению будет вставляться FileWalksHandler
     * При отправке команд по удалению, созданию, переименовыванию будет вставляться CommandHandler, который при
     * завершении работы тоже будет удаляться
     *
     * @throws InterruptedException
     */
    private void start() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(HOST, PORT))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            channel = socketChannel;
                            channel.pipeline().addLast("stringEncoder", stringEncoder);
                            channel.pipeline().addLast("stringDecoder", stringDecoder);
                            channel.pipeline().addLast("authInboundHandler", authInboundHandler);
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Соединение закрыто");
            group.shutdownGracefully().sync();
        }
    }

    private void channelActive() {
        if(channelActive != null) {
            channelActive.call();
        }
    }

    public void authentication(String login, String password) {
        sendMessage("signIn " + login + " " + password);
    }

    public void registration(String login, String password) {
        sendMessage("signUp " + login + " " + password);
    }

    public void createDir(String dirName) {
        channel.pipeline().addLast("commandHandler", commandHandler);
        sendMessage("mkdir " + dirName);
    }

    private void createdDirOnServer(FileInfo fileInfo) {
        channel.pipeline().remove("commandHandler");
        if(createdDirOnServer != null) {
            createdDirOnServer.call(fileInfo);
        }
    }

    public void createFile(String fileName) {
        channel.pipeline().addLast("commandHandler", commandHandler);
        sendMessage("touch " + fileName);
    }

    private void createdFileOnServer(FileInfo fileInfo) {
        channel.pipeline().remove("commandHandler");
        if(createdFileOnServer != null) {
            createdFileOnServer.call(fileInfo);
        }
    }

    public void renameFile(String oldFileName, String newFileName) {
        channel.pipeline().addLast("commandHandler", commandHandler);
        sendMessage("rename " + oldFileName + " " + newFileName);
    }

    private void renamedFileOnServer(FileInfo newFileInfo) {
        channel.pipeline().remove("commandHandler");
        if(renamedFileOnServer != null) {
            renamedFileOnServer.call(newFileInfo);
        }
    }

    public void deleteFile(String fileName) {
        channel.pipeline().addLast("commandHandler", commandHandler);
        sendMessage("delete " + fileName);
    }

    private void deletedFileOnServer() {
        channel.pipeline().remove("commandHandler");
        if(deletedFileOnServer != null) {
            deletedFileOnServer.call();
        }
    }

    public void changeServerDirectory(String path) {
        channel.pipeline().addLast("fileWalksHandler", fileWalksHandler);
        sendMessage("cd " + path);
    }

    private void sendMessage(String message) {
        if(channel != null) {
            channel.writeAndFlush(message);
        }
    }

    private void authOKCallback() {
        if(clientAuthorizedCallback != null) {
            clientAuthorizedCallback.call();
        }
        channel.pipeline().remove("authInboundHandler");
        channel.pipeline().addLast("fileWalksHandler", fileWalksHandler);
        channel.writeAndFlush("cd ~");
    }

    private void readedFileInfoFromServer(FileInfo[] fileInfos, String path) {
        if(readedFileInfoFromServer != null) {
            readedFileInfoFromServer.call(fileInfos, path);
        }
        channel.pipeline().remove("fileWalksHandler");
    }

    private void authErrorCallback(String message) {
        if(authErrorCallback != null) {
            authErrorCallback.call(message);
        }
    }

    public void disconnect() {
        channel.close();
    }
}
