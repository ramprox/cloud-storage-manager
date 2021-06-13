package server.network.handlers;

import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.FileInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.network.Server;
import server.model.User;
import server.util.ApplicationUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message request) {
        Command command = request.getCommand();
        if(command == Command.CHANGE_DIR) {
            handleChangeDir(ctx, (String) request.getData());
        } else if(command == Command.GET_DIR_SIZE) {
            handleGetDirSize(ctx, (String) request.getData());
        } else if(command == Command.CREATE_FILE) {
            handleCreateFile(ctx, (String) request.getData());
        } else if(command == Command.CREATE_DIR) {
            handleCreateDir(ctx, (String) request.getData());
        } else if(command == Command.DELETE) {
            handleDeleteFile(ctx, (String) request.getData());
        } else if(command == Command.RENAME) {
            Object[] data = (Object[]) request.getData();
            String oldFilePath = (String) data[0];
            String newFilePath = (String) data[1];
            handleRenameFile(ctx, oldFilePath, newFilePath);
        } else if(command == Command.SEARCH) {
            Object[] data = (Object[]) request.getData();
            String searchPath = (String) data[0];
            String fileName = (String) data[1];
            handleSearchFile(ctx, searchPath, fileName);
        } else {
            ctx.fireChannelRead(request);
        }
    }

    /**
     * Обработка запроса на изменение текущей директории
     * @param ctx контекст канала
     * @param path запрашиваемый путь
     */
    private void handleChangeDir(ChannelHandlerContext ctx, String path) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            String newPath = path.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString());
            List<Path> paths = Arrays.stream(Objects.requireNonNull(new File(newPath).listFiles()))
                    .map(File::toPath)
                    .collect(Collectors.toList());
            List<FileInfo> fileInfoList;
            fileInfoList = ApplicationUtil.getFileInfos(paths);
            Object[] data = new Object[2];
            data[0] = path;
            data[1] = fileInfoList;
            Message response = new Message(Command.CHANGE_DIR, data);
            ctx.writeAndFlush(response);
        } catch (IOException ex) {
            sendError(ctx, ex.getMessage());
        }
    }

    /**
     * Обработка запроса на получение размера директории
     * @param ctx контекст канала
     * @param dirPath путь к директории
     */
    private void handleGetDirSize(ChannelHandlerContext ctx, String dirPath) {
        User user = Server.getUserByChannel(ctx.channel());
        try {
            dirPath = dirPath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString());
            long dirSize = getDirSize(Paths.get(dirPath));
            FileInfo fileInfo = ApplicationUtil.getFileInfo(Paths.get(dirPath));
            Object[] data = new Object[2];
            data[0] = fileInfo;
            data[1] = dirSize;

            Message response = new Message(Command.GET_DIR_SIZE, data);
            ctx.writeAndFlush(response);
        } catch (IOException ex) {
            sendError(ctx, "Не могу вычислить размер директории. " + ex.getMessage());
        }
    }

    /**
     * Вычисление размера директории
     * @param path путь к директории
     * @return размер директории
     * @throws IOException при вычислении размера
     */
    private long getDirSize(Path path) throws IOException {
        final long[] result = {0};
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException  {
                result[0] += file.toFile().length();
                return FileVisitResult.CONTINUE;
            }
        });
        return result[0];
    }

    /**
     * Обработка команды создания нового файла
     * @param ctx контекст канала
     * @param filePath путь к новому файлу
     */
    private void handleCreateFile(ChannelHandlerContext ctx, String filePath) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path path = Paths.get(filePath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
            if(!Files.exists(path)) {
                Files.createFile(path);
                FileInfo fileInfo = ApplicationUtil.getFileInfo(path);
                Message response = new Message(Command.CREATE_FILE, fileInfo);
                ctx.writeAndFlush(response);
            }
        } catch (IOException ex) {
            sendError(ctx, ex.getMessage());
        }
    }

    /**
     * Обработка команды создания новой директории
     * @param ctx контекст канала
     * @param filePath путь к новому файлу
     */
    private void handleCreateDir(ChannelHandlerContext ctx, String filePath) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path path = Paths.get(filePath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
            if(!Files.exists(path)) {
                Files.createDirectory(path);
                FileInfo fileInfo = ApplicationUtil.getFileInfo(path);
                Message response = new Message(Command.CREATE_DIR, fileInfo);
                ctx.writeAndFlush(response);
            }
        } catch (IOException ex) {
            sendError(ctx, ex.getMessage());
        }
    }

    /**
     * Обработка команды удаления файла
     * @param ctx контекст канала
     * @param filePath путь к удаляемому файлу
     */
    private void handleDeleteFile(ChannelHandlerContext ctx, String filePath) {
        User user = Server.getUserByChannel(ctx.channel());
        Path path = Paths.get(filePath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
        try {
            if(Files.isDirectory(path)) {
                recursiveDeleteDirectory(path);
            } else {
                Files.delete(path);
            }
            Message response = new Message(Command.DELETE, filePath);
            ctx.writeAndFlush(response);
        } catch (IOException e) {
            sendError(ctx, e.getMessage());
        }
    }

    /**
     * Рекурсивное удаление директории на клиенте
     * @param path путь к удаляемой директори
     * @throws IOException при возникновении ошибок, возникающих при удалении
     */
    private void recursiveDeleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void handleRenameFile(ChannelHandlerContext ctx, String oldFilePath, String newFilePath) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path oldPath = Paths.get(oldFilePath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
            Path newPath = Paths.get(newFilePath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
            Files.move(oldPath, newPath);
            Object[] data = new Object[2];
            data[0] = oldFilePath;
            data[1] = newFilePath;
            Message response = new Message(Command.RENAME, data);
            ctx.writeAndFlush(response);
        } catch (IOException ex) {
            sendError(ctx, ex.getMessage());
        }
    }

    private void handleSearchFile(ChannelHandlerContext ctx, String searchPath, String fileName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path path = Paths.get(searchPath.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString()));
            List<FileInfo> result = findFiles(user.getHomeDir().toString(), path, fileName);
            Message response = new Message(Command.SEARCH, result);
            ctx.writeAndFlush(response);
        } catch (IOException ex) {
            sendError(ctx, ex.getMessage());
        }
    }

    private List<FileInfo> findFiles(String userHomeDir, Path startPath, String fileName) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(!startPath.equals(dir) && dir.getFileName().toString().contains(fileName)) {
                    FileInfo fileInfo = ApplicationUtil.getFileInfo(dir);
                    String fullPath = Paths.get(dir.toString().replace(userHomeDir, ApplicationUtil.USER_ROOT_SYMBOL)).toString();
                    fileInfo.setFileName(fullPath);
                    result.add(fileInfo);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(file.getFileName().toString().contains(fileName)) {
                    FileInfo fileInfo = ApplicationUtil.getFileInfo(file);
                    String fullPath = Paths.get(file.toString().replace(userHomeDir, ApplicationUtil.USER_ROOT_SYMBOL)).toString();
                    fileInfo.setFileName(fullPath);
                    result.add(fileInfo);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        Message response = new Message(Command.ERROR, message);
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        Server.unsubscribeUser(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        Server.unsubscribeUser(ctx.channel());
    }
}
