package server.network.controller;

import interop.Command;
import interop.ThrowableConsumer;
import interop.dto.*;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;
import interop.model.DownloadOperation;
import interop.service.FileInfoService;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import server.annotations.RequestHandler;
import server.network.service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Queue;
import java.util.function.Consumer;

@Controller
public class UploadController {

    private final FileInfoService fileInfoService;

    private final UserService userService;

    @Autowired
    public UploadController(FileInfoService fileInfoService,
                            UserService userService) {
        this.fileInfoService = fileInfoService;
        this.userService = userService;
    }

    @RequestHandler(command = Command.UPLOAD)
    public Message uploadRequest(Message message, Channel channel) {
        DeepFilesDto deepFilesDtoData = (DeepFilesDto) message.getData();
        DownloadOperation operation = filterFiles(deepFilesDtoData, channel);
        if (operation.getTotalSize() > 0) {
            Queue<DownloadOperation> operations = userService.getQueueByChannel(channel);
            operations.add(operation);
            Message response = new Message();
            response.setCommand(Command.READY_UPLOAD);
            return response;
        }
        Message response = new Message();
        response.setCommand(Command.UPLOAD_DONE);
        response.setData(deepFilesDtoData.getDestination());
        return response;
    }

    private DownloadOperation filterFiles(DeepFilesDto deepFilesDtoData, Channel channel) {
        DownloadOperation operation = new DownloadOperation();
        String destination = deepFilesDtoData.getDestination();
        operation.setDestination(deepFilesDtoData.getDestination());
        deepFilesDtoData.getFiles().forEach(wrapper(fileInfo -> {
            String path = Paths.get(destination, fileInfo.getFileName()).toString();
            fileInfo.setFileName(path);
            filterFiles(fileInfo, channel, operation);
        }));
        return operation;
    }

    private void filterFiles(FileInfo fileInfo, Channel channel, DownloadOperation operation) throws IOException {
        String path = Paths.get(fileInfo.getFileName()).toString();
        Path fullPath = userService.convertRequestedPathToLocal(path, channel);
        if (fileInfo.getType() == FileType.DIR) {
            Files.createDirectories(fullPath);
        } else {
            Files.deleteIfExists(fullPath);
            Files.createFile(fullPath);
            if (fileInfo.getSize() > 0) {
                operation.setTotalSize(operation.getTotalSize() + fileInfo.getSize());
            }
        }
        sendCreateFileMessage(fullPath, path, channel);
    }

    private void sendCreateFileMessage(Path fullPath, String sendPath, Channel channel) {
        FileInfo fileInfo = fileInfoService.getFileInfo(fullPath);
        fileInfo.setFileName(sendPath);
        Message message = new Message();
        message.setCommand(fileInfo.getType() == FileType.DIR ? Command.CREATE_DIR : Command.CREATE_FILE);
        message.setData(fileInfo);
        channel.writeAndFlush(message);
    }

    private <T> Consumer<T> wrapper(ThrowableConsumer<T> consumer) {
        return object -> {
            try {
                consumer.accept(object);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @RequestHandler(command = Command.UPLOADING)
    public Message uploading(Message message, Channel channel) {
        FileChunkDto fileChunkDto = (FileChunkDto) message.getData();
        Path path = userService.convertRequestedPathToLocal(fileChunkDto.getPath(), channel);
        try {
            Files.write(path, fileChunkDto.getContent(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Queue<DownloadOperation> operations = userService.getQueueByChannel(channel);
        DownloadOperation operation = operations.element();
        operation.setCurrentSize(operation.getCurrentSize() + fileChunkDto.getContent().length);
        Message response = new Message();
        if (operation.getCurrentSize() != operation.getTotalSize()) {
            response.setCommand(Command.PERCENT_UPLOAD);
            double percentUpload = operation.getCurrentSize() * 1.0 / operation.getTotalSize();
            ProgressInfoDto progressInfoDto = new ProgressInfoDto();
            progressInfoDto.setCurrentFilePath(fileChunkDto.getPath());
            progressInfoDto.setTotalPercentage(percentUpload);
            response.setData(progressInfoDto);
        } else {
            operations.poll();
            response.setCommand(Command.UPLOAD_DONE);
            response.setData(operation.getDestination());
        }
        return response;
    }
}
