package server.network.controller;

import interop.Command;
import interop.dto.DirFilesDto;
import interop.dto.Message;
import interop.dto.RenameFileDto;
import interop.dto.SearchFileDto;
import interop.dto.fileinfo.FileInfo;
import interop.service.FileInfoService;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import server.annotations.RequestHandler;
import server.exceptions.HandleException;
import server.network.service.UserService;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Controller
public class SimpleFileCommandController {

    private final FileInfoService fileInfoService;

    private final UserService userService;

    @Autowired
    public SimpleFileCommandController(FileInfoService fileInfoService, UserService userService) {
        this.fileInfoService = fileInfoService;
        this.userService = userService;
    }

    @RequestHandler(command = Command.CHANGE_DIR)
    public Message changeDir(Message message, Channel channel) {
        try {
            String path = (String) message.getData();
            Path newPath = userService.convertRequestedPathToLocal(path, channel);
            List<FileInfo> fileInfoList = fileInfoService.getFileInfos(newPath)
                    .stream()
                    .peek(fileInfo -> {
                        String fileName = Paths.get(fileInfo.getFileName()).getFileName().toString();
                        fileInfo.setFileName(fileName);
                    }).collect(Collectors.toList());
            Message response = new Message();
            response.setCommand(Command.CHANGE_DIR);
            DirFilesDto dirFilesDto = new DirFilesDto(path, fileInfoList);
            response.setData(dirFilesDto);
            return response;
        } catch (IOException ex) {
            throw new HandleException(ex.getMessage());
        }
    }

    @RequestHandler(command = Command.GET_DIR_SIZE)
    public Message getDirSize(Message message, Channel channel) {
        try {
            String path = (String) message.getData();
            Path newPath = userService.convertRequestedPathToLocal(path, channel);
            long dirSize = fileInfoService.getDirSize(newPath);
            FileInfo fileInfo = fileInfoService.getFileInfo(newPath);
            String fileName = Paths.get(fileInfo.getFileName()).getFileName().toString();
            fileInfo.setFileName(fileName);
            fileInfo.setSize(dirSize);
            Message response = new Message();
            response.setCommand(Command.GET_DIR_SIZE);
            response.setData(fileInfo);
            return response;
        } catch (IOException ex) {
            throw new HandleException("Не могу вычислить размер директории. " + ex.getMessage());
        }
    }

    @RequestHandler(command = Command.CREATE_FILE)
    public Message createFile(Message message, Channel channel) {
        return create(message, channel, path -> {
            try {
                return Files.createFile(path);
            } catch (IOException ex) {
                throw new HandleException(ex.getMessage());
            }
        });
    }

    @RequestHandler(command = Command.CREATE_DIR)
    public Message createDir(Message message, Channel channel) {
        return create(message, channel, path -> {
            try {
                return Files.createDirectory(path);
            } catch (IOException ex) {
                throw new HandleException(ex.getMessage());
            }
        });
    }

    @RequestHandler(command = Command.DELETE)
    public Message deleteFile(Message message, Channel channel) {
        String path = (String) message.getData();
        Path newPath = userService.convertRequestedPathToLocal(path, channel);
        try {
            if(Files.isDirectory(newPath)) {
                recursiveDeleteDirectory(newPath);
            } else {
                Files.delete(newPath);
            }
            Message response = new Message();
            response.setCommand(Command.DELETE);
            response.setData(path);
            return response;
        } catch (IOException ex) {
            throw new HandleException(ex.getMessage());
        }
    }

    @RequestHandler(command = Command.RENAME)
    public Message renameFile(Message message, Channel channel) {
        try {
            RenameFileDto renameFileDtoDate = (RenameFileDto) message.getData();
            String oldFilePath = renameFileDtoDate.getOldFileName();
            String newFilePath = renameFileDtoDate.getNewFileName();
            Path oldPath = userService.convertRequestedPathToLocal(oldFilePath, channel);
            Path newPath = userService.convertRequestedPathToLocal(newFilePath, channel);
            Files.move(oldPath, newPath);
            Message response = new Message();
            response.setCommand(Command.RENAME);
            RenameFileDto renameFileDto = new RenameFileDto(oldFilePath, newFilePath);
            response.setData(renameFileDto);
            return response;
        } catch (IOException ex) {
            throw new HandleException(ex.getMessage());
        }
    }

    @RequestHandler(command = Command.SEARCH)
    public Message searchFile(Message message, Channel channel) {
        try {
            SearchFileDto searchFileDto = (SearchFileDto) message.getData();
            Path startPath = userService.convertRequestedPathToLocal(searchFileDto.getStartPath(), channel);
            List<FileInfo> result = findFiles(userService.getHomeDir(channel), startPath, searchFileDto.getFileName());
            Message response = new Message();
            response.setCommand(Command.SEARCH);
            response.setData(result);
            return response;
        } catch (IOException ex) {
            throw new HandleException(ex.getMessage());
        }
    }

    private List<FileInfo> findFiles(Path userHomeDir, Path startPath, String fileName) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileName);
        List<FileInfo> result = new LinkedList<>();
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return find(dir);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                return find(file);
            }

            private FileVisitResult find(Path path) {
                Path fileName = path.getFileName();
                if(matcher.matches(fileName)) {
                    FileInfo fileInfo = fileInfoService.getFileInfo(path);
                    String fullPath = userService.convertLocalPathToClient(path, userHomeDir);
                    fileInfo.setFileName(fullPath);
                    result.add(fileInfo);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    private Message create(Message message, Channel channel, UnaryOperator<Path> operator) {
        String path = (String) message.getData();
        Path newPath = userService.convertRequestedPathToLocal(path, channel);
        operator.apply(newPath);
        FileInfo fileInfo = fileInfoService.getFileInfo(newPath);
        fileInfo.setFileName(path);
        Message response = new Message();
        response.setCommand(message.getCommand());
        response.setData(fileInfo);
        return response;
    }

    /**
     * Рекурсивное удаление директории
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
}
