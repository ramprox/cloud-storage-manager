package server.network.controller;

import interop.Command;
import interop.dto.DeepFilesDto;
import interop.model.LoadingFiles;
import interop.dto.Message;
import interop.dto.SurfaceFilesDto;
import interop.service.ChunkedFileReader;
import interop.service.FileInfoService;
import io.netty.channel.Channel;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.annotations.RequestHandler;
import server.network.service.UserService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DownloadController {

    private final FileInfoService fileInfoService;

    private final int chunkSize;

    private final UserService userService;

    @Autowired
    public DownloadController(FileInfoService fileInfoService,
                              @Value("${chunkSize}") int chunkSize,
                              UserService userService) {
        this.fileInfoService = fileInfoService;
        this.chunkSize = chunkSize;
        this.userService = userService;
    }

    @RequestHandler(command = Command.DOWNLOAD)
    public Message downloadRequest(Message message, Channel channel) {
        SurfaceFilesDto operation = (SurfaceFilesDto) message.getData();
        String source = operation.getSourcePath();
        Path sourceFullPath = userService.convertRequestedPathToLocal(source, channel);
        LoadingFiles files = fileInfoService.formFilesLists(sourceFullPath.toString(), operation.getFileNames());
        Message response = new Message();
        DeepFilesDto responseData = new DeepFilesDto(source, files.getRecipientFiles());
        response.setCommand(Command.DOWNLOAD);
        response.setData(responseData);
        return response;
    }

    @RequestHandler(command = Command.DOWNLOADING)
    public Message downloading(Message message, Channel channel) {
        try {
            String path = (String) message.getData();
            Path fullPath = userService.convertRequestedPathToLocal(path, channel);
            sendFile(fullPath, Paths.get(path), channel);
            return null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendFile(Path fullPath, Path destination, Channel channel) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(fullPath.toFile());
        ChunkedStream chunkedStream = new ChunkedStream(inputStream, chunkSize);
        ChunkedInput<Message> fileReader = new ChunkedFileReader(chunkedStream, destination, Command.DOWNLOADING);
        channel.writeAndFlush(fileReader);
    }
}
