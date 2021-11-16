package server.network.handlers;

import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.FileInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import server.network.service.ChunkedFileReader;
import server.network.Server;
import server.model.User;
import server.util.ApplicationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class DownloadHandler extends ChunkedWriteHandler {

    private List<File> uploadingFiles;

    /**
     * Обработка передачи сообщения
     * @param ctx контекст канала
     * @param msg если msg является объектом типа ChunkedFileReader, то осуществляется передача файлов,
     *            если нет - передача следующему Handler в цепочке
     * @param promise объект типа Promise
     * @throws Exception может возникнуть при передаче файлов
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof ChunkedFileReader) {
            super.write(ctx, msg, promise);
        } else {
            ctx.writeAndFlush(msg);
        }
    }

    /**
     * Обработка чтения сообщения
     * @param ctx контекст канала
     * @param msg если msg содержит команды связанные с Download, то происходит обработка
     *            если нет - передача следующему Handler в цепочке
     * @throws Exception может произойти при обработке команд связанных с Download
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof Message) {
            Message request = (Message) msg;
            Command command = request.getCommand();
            if(command == Command.DOWNLOAD) {
                User user = Server.getUserByChannel(ctx.channel());
                String filePath = (String) request.getData();
                Path downloadingPath = Paths.get(filePath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
                handleDownloadCommand(ctx, downloadingPath);
            } else if(command == Command.READY_DOWNLOAD) {
                for(File file : uploadingFiles) {
                    write(ctx, new ChunkedFileReader(new ChunkedStream(new FileInputStream(file), ApplicationUtil.LOAD_BUFFER_SIZE)), ctx.newPromise());
                }
                flush(ctx);
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Обработка команды Command.DOWNLOAD
     * @param ctx контекст канала
     * @param downloadingPath путь откуда будут передаваться файлы
     */
    private void handleDownloadCommand(ChannelHandlerContext ctx, Path downloadingPath) {
        try {
            List<File> fullListForServer = getFullList(downloadingPath);                      // полный список для сервера
            List<FileInfo> fullListForClient =
                    getFullListForClient(fullListForServer, downloadingPath.getParent());     // полный список для клиента

            uploadingFiles = fullListForServer.stream()                                       // фильтрация списка
                    .filter(file -> !file.isDirectory() && file.length() != 0)                // происходит удаление из списка файлов
                    .collect(Collectors.toList());                                            // имеющих тип директории и файлов с нулевым размером
            Message request = new Message(Command.DOWNLOAD, fullListForClient);
            ctx.writeAndFlush(request);
        } catch (IOException ex) {
            Message errorResponse = new Message(Command.ERROR, ex.getMessage());
            ctx.writeAndFlush(errorResponse);
        }
    }

    /**
     * Получение полного списка отправляемых файлов
     * @param path путь к передаваемому файлу
     * @return полный список передаваемых файлов, включая директории и файлы с нулевым размером
     * @throws IOException может произойти при построении списка
     */
    private List<File> getFullList(Path path) throws IOException {
        List<File> result = new LinkedList<>();
        if(Files.isDirectory(path)) {
            result.addAll(getFullListForServerFromDirectory(path));
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
    private List<File> getFullListForServerFromDirectory(Path path) throws IOException {
        List<File> result = new LinkedList<>();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                result.add(dir.toFile());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                result.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    /**
     * Получение полного списка для клиента
     * @param fullListForServer полный список передаваемых файлов
     * @param parentPath путь к родительской папке передаваемого файла
     * @return список типа List<FileInfo> для передачи клиенту
     * @throws IOException может возникнуть при формировании списка
     */
    private List<FileInfo> getFullListForClient(List<File> fullListForServer, Path parentPath) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        for(File file: fullListForServer) {
            FileInfo fileInfo = ApplicationUtil.getFileInfo(file.toPath());
            String path = parentPath.relativize(file.toPath()).toString();
            fileInfo.setFileName(path);
            result.add(fileInfo);
        }
        return result;
    }
}
