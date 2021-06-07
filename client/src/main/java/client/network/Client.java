package client.network;

import client.interfaces.*;
import client.network.handlers.*;
import client.network.handlers.SignHandler;
import interop.model.fileinfo.*;
import interop.model.requests.*;
import interop.model.requests.fileoperation.*;
import interop.model.requests.sign.*;
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

    private static final String HOST = "localhost";
    private static final int PORT = 5678;

    private Channel channel;
    private static Presentable presentable;
    private UploadHandler uploadHandler;
    private DownloadHandler downloadHandler;

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
            } catch (InterruptedException e) {
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
    private void start() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(HOST, PORT))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            channel = socketChannel;
                            channel.pipeline().addLast(new ObjectEncoder());
                            channel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                            channel.pipeline().addLast(new SignHandler());
                            channel.pipeline().addLast(new ChangeDirHandler());
                            channel.pipeline().addLast(new FileChangeHandler());
                            uploadHandler = new UploadHandler();
                            downloadHandler = new DownloadHandler();
                            channel.pipeline().addLast(uploadHandler);
                            channel.pipeline().addLast(downloadHandler);
                            channel.pipeline().addLast(new SearchHandler());
                            channel.pipeline().addLast(new ErrorHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect().addListener(future -> presentable.channelActivated()).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            presentable.exceptionCaught(e);
        } finally {
            System.out.println("Соединение закрыто");
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
        SignIn request = new SignIn(login, password);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на регистрацию
     * @param login логин
     * @param password пароль
     */
    public void registration(String login, String password) {
        SignUp request = new SignUp(login, password);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на изменение текущей директории
     * @param path путь к новой директории
     */
    public void changeDir(String path) {
        ChangeDir request = new ChangeDir(path);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на создание нового файла
     * @param fileName имя файла
     * @param fileType тип файла (директория или обычный файл)
     */
    public void createFile(String fileName, FileType fileType) {
        CreateFile request = new CreateFile(fileName, fileType);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на переименование файла
     * @param oldFileName старое название файла
     * @param newFileName новое название файла
     */
    public void renameFile(String oldFileName, String newFileName) {
        RenameFile request = new RenameFile(oldFileName, newFileName);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на удаление файла
     * @param fileName имя удаляемого файла
     */
    public void deleteFile(String fileName) {
        DeleteFile request = new DeleteFile(fileName);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на загрузку файла на удаленный сервер
     * @param path путь к загружаемому файлу
     */
    public void uploadFile(Path path) {
        List<File> fullListForClient = new LinkedList<>();
        List<FileInfo> fullListForServer = new LinkedList<>();


        writeInListsFileInfo(path, fullListForClient, fullListForServer);

        List<File> listRegularFilesForClient = fullListForClient.stream()
                .filter(file -> !file.isDirectory() && file.length() != 0)
                .collect(Collectors.toList());

        uploadHandler.setUploadingFiles(listRegularFilesForClient);
        UploadRequest request = new UploadRequest(fullListForServer);
        channel.writeAndFlush(request);
    }

    /**
     * Заполнение списка файлов для клиента и для сервера
     * @param path путь к файлу
     * @param listForClient список для клиента
     * @param listForServer список для сервера
     */
    private void writeInListsFileInfo(Path path, List<File> listForClient, List<FileInfo> listForServer) {
        try {
            if(Files.isDirectory(path)) {
                addToListForDirectory(path.toAbsolutePath(), listForClient, listForServer);
            } else {
                File file = path.toFile();
                BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                listForServer.add(new RegularFile(file.getName(), file.lastModified(), file.length(), createDate));
                listForClient.add(new File(file.getAbsolutePath()));
            }
        } catch (IOException ex) {
            presentable.errorReceived(ex.getMessage());
        }
    }

    /**
     * Добавление в список для клиента и сервера
     * @param path путь к файлу
     * @param listForClient список для клиента
     * @param listForServer список для сервера
     * @throws IOException ошибки при чтении атрибутов
     */
    private void addToListForDirectory(Path path, List<File> listForClient, List<FileInfo> listForServer) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                File file = dir.toFile();
                BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                FileInfo fileInfo = new Directory(path.getFileName().resolve(path.relativize(dir)).toString(),
                        file.lastModified(), createDate);
                listForServer.add(fileInfo);
                listForClient.add(new File(dir.toFile().getAbsolutePath()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                FileInfo fileInfo = new RegularFile(path.getFileName().resolve(path.relativize(file)).toString(),
                        file.toFile().lastModified(), file.toFile().length(), createDate);
                listForServer.add(fileInfo);
                listForClient.add(new File(file.toFile().getAbsolutePath()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Запрос на загрузку файла из удаленного сервера
     * @param clientDir текущий путь на стороне клиента
     * @param serverFileName имя файла на сервере
     */
    public void downloadFile(Path clientDir, String serverFileName) {
        downloadHandler.setCurrentClientDir(clientDir);
        DownloadRequest request = new DownloadRequest(serverFileName);
        channel.writeAndFlush(request);
    }

    /**
     * запрос на получение размера директории
     * @param dirName имя директории
     */
    public void viewDirSize(String dirName) {
        DirSizeReq request = new DirSizeReq(dirName);
        channel.writeAndFlush(request);
    }

    /**
     * Запрос на поиск файла
     * @param fileName имя файла
     */
    public void searchFile(String fileName) {
        SearchRequest request = new SearchRequest(fileName);
        channel.writeAndFlush(request);
    }

    /**
     * Отключение от сервера
     */
    public void disconnect() {
        channel.close();
    }
}
