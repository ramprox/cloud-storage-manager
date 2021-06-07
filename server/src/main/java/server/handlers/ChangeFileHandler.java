package server.handlers;

import interop.interfaces.FileNameReq;
import interop.model.fileinfo.Directory;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.FileType;
import interop.model.fileinfo.RegularFile;
import interop.model.requests.DirSizeReq;
import interop.model.requests.fileoperation.CreateFile;
import interop.model.requests.fileoperation.DeleteFile;
import interop.model.requests.fileoperation.FileOperation;
import interop.model.requests.fileoperation.RenameFile;
import interop.model.responses.DirSizeResp;
import interop.model.responses.ErrorResponse;
import interop.model.responses.fileoperation.CreateFileResp;
import interop.model.responses.fileoperation.DeleteFileResp;
import interop.model.responses.fileoperation.RenameFileResp;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.Server;
import server.model.User;
import server.util.Conversations;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Класс обработчика, ответственный за обработку запросов по изменению файлов (создание, переименование, удаление)
 * и получение размера файлов
 */
public class ChangeFileHandler extends SimpleChannelInboundHandler<FileNameReq> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FileNameReq request) throws IOException {
        String fileName = request.getFileName();
        if(request instanceof CreateFile) {
            CreateFile createFile = (CreateFile)request;
            FileType type = createFile.getType();
            handleCreateFile(ctx, fileName, type);
        } else if(request instanceof RenameFile) {
            RenameFile renameFile = (RenameFile)request;
            String oldFileName = renameFile.getFileName();
            String newFileName = renameFile.getNewFileName();
            handleRenameFile(ctx, oldFileName, newFileName);
        } else if(request instanceof DeleteFile) {
            String deletedFileName = request.getFileName();
            handleDeleteFile(ctx, deletedFileName);
        } else if(request instanceof DirSizeReq) {
            handleDirSizeReq(ctx, request.getFileName());
        }
    }

    /**
     * Обработка запроса создания файла
     * @param ctx контекст канала
     * @param fileName имя создаваемого файла
     * @param type тип создаваемого файла
     */
    private void handleCreateFile(ChannelHandlerContext ctx, String fileName, FileType type) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path currentDir = user.getCurrentDir();
            FileInfo fileInfo;
            if(type.equals(FileType.DIR)) {
                File result = Files.createDirectory(currentDir.resolve(fileName)).toFile();
                BasicFileAttributes attr = Files.readAttributes(result.toPath(), BasicFileAttributes.class);
                long createDate = attr.creationTime().toMillis();
                fileInfo = new Directory(result.getName(), result.lastModified(), createDate);
            } else {
                File result = Files.createFile(currentDir.resolve(fileName)).toFile();
                BasicFileAttributes attr1 = Files.readAttributes(result.toPath(), BasicFileAttributes.class);
                long createDate = attr1.creationTime().toMillis();
                fileInfo = new RegularFile(result.getName(), result.lastModified(), result.length(), createDate);
            }
            CreateFileResp response = new CreateFileResp(fileInfo);
            ctx.writeAndFlush(response);
        } catch (IOException ex) {
            ErrorResponse error = new ErrorResponse(ex.getMessage());
            ctx.writeAndFlush(error);
        }
    }

    /**
     * Обработка запросо по переименованию файла
     * @param ctx контекст канала
     * @param oldFileName старое имя файла
     * @param newFileName новое имя файла
     */
    private void handleRenameFile(ChannelHandlerContext ctx, String oldFileName, String newFileName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path currentDir = user.getCurrentDir();
            Path oldPath = currentDir.resolve(oldFileName);

            FileInfo oldFileInfo;
            File oldFile = oldPath.toFile();
            BasicFileAttributes attr1 = Files.readAttributes(oldPath, BasicFileAttributes.class);
            long createDate1 = attr1.creationTime().toMillis();
            if(Files.isDirectory(oldPath)) {
                oldFileInfo = new Directory(oldFile.getName(), oldFile.lastModified(), createDate1);
            } else {
                oldFileInfo = new RegularFile(oldFile.getName(), oldFile.lastModified(), oldFile.length(), createDate1);
            }

            Path newPath = currentDir.resolve(newFileName);
            Files.move(oldPath, newPath);

            File newFile = newPath.toFile();
            FileInfo newFileInfo;
            BasicFileAttributes attr2 = Files.readAttributes(newPath, BasicFileAttributes.class);
            long createDate2 = attr2.creationTime().toMillis();
            if(Files.isDirectory(newPath)) {
                newFileInfo = new Directory(newFile.getName(), newFile.lastModified(), createDate2);
            } else {
                newFileInfo = new RegularFile(newFile.getName(), newFile.lastModified(), newFile.length(), createDate2);
            }
            RenameFileResp response = new RenameFileResp(oldFileInfo, newFileInfo);
            ctx.writeAndFlush(response);
        } catch (IOException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            ctx.writeAndFlush(errorResponse);
        }
    }

    /**
     * Обработка запроса по удалению файла
     * @param ctx контекст канала
     * @param fileName имя удаляемого файла
     */
    private void handleDeleteFile(ChannelHandlerContext ctx, String fileName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path filePath = user.getCurrentDir().resolve(fileName);
            File file = filePath.toFile();
            FileInfo fileInfo;
            BasicFileAttributes attr1 = Files.readAttributes(filePath, BasicFileAttributes.class);
            long createDate = attr1.creationTime().toMillis();
            if(Files.isDirectory(filePath)) {
                deleteDir(filePath);
                fileInfo = new Directory(file.getName(), file.lastModified(), createDate);
            } else {
                Files.delete(filePath);
                fileInfo = new RegularFile(file.getName(), file.lastModified(), file.length(), createDate);
            }
            DeleteFileResp response = new DeleteFileResp(fileInfo);
            ctx.writeAndFlush(response);
        } catch (IOException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            ctx.writeAndFlush(errorResponse);
        }
    }

    /**
     * Рекурсивное удаление директории
     * @param dirPath путь к директории
     * @throws IOException
     */
    private void deleteDir(Path dirPath) throws IOException {
        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
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

    /**
     * Обработка запроса получения размера директории
     * @param ctx контекст канала
     * @param fileName имя директории
     * @throws IOException
     */
    private void handleDirSizeReq(ChannelHandlerContext ctx, String fileName) throws IOException {
        User user = Server.getUserByChannel(ctx.channel());
        Path filePath = Paths.get(user.getCurrentDir().toString(), fileName);
        FileInfo fileInfo = Conversations.getFileInfo(filePath.toFile());
        long size = getDirSize(filePath);
        DirSizeResp response = new DirSizeResp(fileInfo, size);
        ctx.writeAndFlush(response);
    }

    /**
     * Вычисление размера директории
     * @param path путь к директории
     * @return размер директории
     * @throws IOException
     */
    private long getDirSize(Path path) throws IOException {
        final long[] result = {0};
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result[0] += file.toFile().length();
                return FileVisitResult.CONTINUE;
            }
        });
        return result[0];
    }
}
