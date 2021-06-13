package client.network;

import client.interfaces.*;
import client.network.handlers.*;
import client.network.handlers.SignHandler;
import client.utils.ApplicationUtil;
import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Главный класс, ответственный за взаимодействие с сервером
 */
public class Client {

    private Channel channel;
    private static Presentable presentable;

    public static Presentable getPresentable() {
        return presentable;
    }

    public Client(Presentable presentable) {
        Client.presentable = presentable;
    }

    /**
     * Соединение с сервером
     */
    public void connect() {
        Thread thread = new Thread(() -> {
            try {
                start();
            } catch (InterruptedException | IOException e) {
                presentable.exceptionCaught(e);
            }
        });
        thread.start();
    }

    /**
     * Запуск соединения с сервером
     *
     * @throws InterruptedException ошибки прерывания
     */
    private void start() throws InterruptedException, IOException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(ApplicationUtil.HOST, ApplicationUtil.PORT))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            channel = socketChannel;
                            channel.pipeline().addLast(new ObjectEncoder());
                            channel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(ClassLoader.getSystemClassLoader())));
                            channel.pipeline().addLast(new SignHandler());
                            channel.pipeline().addLast(new ErrorHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    // -------------------- Отправка запросов на сервер --------------------

    /**
     * Запрос на аутентификацию
     * @param login логин
     * @param password пароль
     */
    public void authentication(String login, String password) {
        String[] data = new String[2];
        data[0] = login;
        data[1] = password;
        Message request = new Message(Command.SIGN_IN, data);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на регистрацию
     * @param login логин
     * @param password пароль
     */
    public void registration(String login, String password) {
        String[] data = new String[2];
        data[0] = login;
        data[1] = password;
        Message request = new Message(Command.SIGN_UP, data);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на изменение текущей директории
     * @param path путь к новой директории
     */
    public void changeDir(String path) {
        Message request = new Message(Command.CHANGE_DIR, path);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на создание нового файла
     * @param filePath путь нового файла
     */
    public void createFile(String filePath) {
        Message request = new Message(Command.CREATE_FILE, filePath);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на создание новой директории на сервере
     * @param filePath путь к новой директории
     */
    public void createDir(String filePath) {
        Message request = new Message(Command.CREATE_DIR, filePath);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на переименование файла
     * @param oldFilePath путь к старому файлу
     * @param newFilePath путь к новому файлу
     */
    public void renameFile(String oldFilePath, String newFilePath) {
        Object[] data = new Object[2];
        data[0] = oldFilePath;
        data[1] = newFilePath;
        Message request = new Message(Command.RENAME, data);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на удаление файла
     * @param filePath имя удаляемого файла
     */
    public void deleteFile(String filePath) {
        Message request = new Message(Command.DELETE, filePath);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на загрузку файла на удаленный сервер
     * @param path путь на клиенте к загружаемому файлу
     * @param serverCurrentPath путь на сервере куда необходимо загрузить файл
     */
    public void uploadFile(Path path, Path serverCurrentPath) {
        try {
            List<File> fullListForClient = getFullListForClient(path);
            List<FileInfo> fullListForServer = getFullListForServer(fullListForClient, path.getParent());

            List<File> listRegularFilesForClient = fullListForClient.stream()
                    .filter(file -> !file.isDirectory() && file.length() != 0)
                    .collect(Collectors.toList());

            UploadHandler uploadHandler = channel.pipeline().get(UploadHandler.class);
            uploadHandler.setUploadingFiles(listRegularFilesForClient);
            Object[] data = new Object[2];
            data[0] = serverCurrentPath.toString();
            data[1] = fullListForServer;
            Message request = new Message(Command.UPLOAD, data);
            channel.writeAndFlush(request);
        } catch (IOException ex) {
            presentable.exceptionCaught(ex);
        }
    }

    /**
     * Получение полного списка загружаемых файлов на сервер включая директории и файлы снулевым размером
     * @param path путь к файлу
     * @return полный список файлов для загрузки на сервер
     * @throws IOException ошибки при чтении атрибутов
     */
    private List<File> getFullListForClient(Path path) throws IOException {
        List<File> result = new LinkedList<>();
        if(Files.isDirectory(path)) {
            result.addAll(getFullListForClientFromDirectory(path));
        } else {
            result.add(path.toFile());
        }
        return result;
    }

    /**
     * Добавление в список для клиента и сервера
     * @param path путь к файлу
     * @throws IOException ошибки при чтении атрибутов
     */
    private List<File> getFullListForClientFromDirectory(Path path) throws IOException {
        List<File> result = new LinkedList<>();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                result.add(dir.toFile());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    private List<FileInfo> getFullListForServer(List<File> listForClient, Path relativePath) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        for(File file: listForClient) {
            result.add(getFromFile(relativePath, file.toPath()));
        }
        return result;
    }

    private FileInfo getFromFile(Path relativePath, Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        FileType type = attr.isDirectory() ? FileType.DIR : FileType.FILE;
        String fileName = relativePath.relativize(path).toString();
        if(fileName.equals("")) {
            fileName = path.getFileName().toString();
        }
        long size = type == FileType.DIR ? -1L : attr.size();
        long lastModified = attr.lastModifiedTime().toMillis();
        long createDate = attr.creationTime().toMillis();
        return new FileInfo(type, fileName, size, lastModified, createDate);
    }

    /**
     * Запрос на загрузку файла из удаленного сервера
     * @param clientDir путь на стороне клиента куда необходимо загрузить файлы
     * @param serverFilePath путь к файлу на сервере
     */
    public void downloadFile(Path clientDir, String serverFilePath) {
        DownloadHandler downloadHandler = channel.pipeline().get(DownloadHandler.class);
        downloadHandler.setDestinationDir(clientDir);
        Message request = new Message(Command.DOWNLOAD, serverFilePath);
        channel.writeAndFlush(request);
    }

    /**
     * запрос на получение размера директории
     * @param dirName путь к директории
     */
    public void viewDirSize(String dirName) {
        Message request = new Message(Command.GET_DIR_SIZE, dirName);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на поиск файла
     * @param fileName имя файла
     */
    public void searchFile(String startPath, String fileName) {
        Object[] data = new Object[2];
        data[0] = startPath;
        data[1] = fileName;
        Message request = new Message(Command.SEARCH, data);
        channel.writeAndFlush(request);
    }

    /**
     * Отключение от сервера
     */
    public void disconnect() {
        channel.close();
    }
}
