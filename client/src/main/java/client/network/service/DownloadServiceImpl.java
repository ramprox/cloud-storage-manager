package client.network.service;

import client.events.DownloadDoneEvent;
import client.network.annotations.RequestHandler;
import client.network.annotations.ResponseHandler;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.*;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;
import interop.model.DownloadOperation;
import io.netty.channel.Channel;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class DownloadServiceImpl implements DownloadService {

    private final ServerEventsListener serverEventsListener;

    private final ApplicationEventPublisher publisher;

    private final Queue<DownloadOperation> operations = new LinkedList<>();

    public DownloadServiceImpl(ServerEventsListener serverEventsListener,
                               ApplicationEventPublisher publisher) {
        this.serverEventsListener = serverEventsListener;
        this.publisher = publisher;
    }

    protected abstract Channel getChannel();

    @Override
    @RequestHandler(command = Command.DOWNLOAD)
    public void downloadRequest(String sourcePath, List<String> files, String destination) {
        DownloadOperation downloadOperation = new DownloadOperation();
        downloadOperation.setDestination(destination);
        downloadOperation.setSource(Paths.get(sourcePath));
        operations.add(downloadOperation);

        SurfaceFilesDto surfaceFilesDto = new SurfaceFilesDto(sourcePath, files);
        Message message = new Message();
        message.setCommand(Command.DOWNLOAD);
        message.setData(surfaceFilesDto);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.DOWNLOAD)
    public void downloadResponse(Message message) {
        DeepFilesDto deepFilesDtoData = (DeepFilesDto) message.getData();
        List<FileInfo> filteredFiles = filterFiles(deepFilesDtoData);

        DownloadOperation operation = operations.element();
        if(filteredFiles.size() == 0) {
            operations.poll();
            serverEventsListener.uploadDone(operation.getDestination());
            return;
        }
        filteredFiles.forEach(fileInfo -> {
            Message request = new Message();
            request.setCommand(Command.DOWNLOADING);
            Path path = Paths.get(deepFilesDtoData.getDestination(), fileInfo.getFileName());
            request.setData(path.toString());
            getChannel().writeAndFlush(request);
        });
    }

    @Override
    @ResponseHandler(command = Command.DOWNLOADING)
    public void downloading(Message message) throws Exception {
        FileChunkDto data = (FileChunkDto) message.getData();
        DownloadOperation operation = operations.element();
        Path relativizeServerPath = operation.getSource().relativize(Paths.get(data.getPath()));
        Path fullClientPath = Paths.get(operation.getDestination(), relativizeServerPath.toString());
        byte[] content = data.getContent();
        Files.write(fullClientPath, content, StandardOpenOption.APPEND);
        operation.setCurrentSize(operation.getCurrentSize() + content.length);
        if(operation.getCurrentSize() == operation.getTotalSize()) {
            serverEventsListener.downloadDone(operation.getDestination());
            publisher.publishEvent(new DownloadDoneEvent(this, operation.getDestination()));
        } else {
            double percent = operation.getCurrentSize() * 1.0 / operation.getTotalSize();
            serverEventsListener.progressDownload(percent, data.getPath());
        }
    }

    private List<FileInfo> filterFiles(DeepFilesDto deepFilesDtoData) {
        DownloadOperation operation = operations.element();
        return deepFilesDtoData.getFiles()
                .stream()
                .filter(wrapper(fileInfo -> filterFiles(fileInfo, operation)))
                .collect(Collectors.toList());
    }

    private boolean filterFiles(FileInfo fileInfo, DownloadOperation operation) throws IOException {
        boolean result = false;
        Path fullPath = Paths.get(operation.getDestination(), fileInfo.getFileName());
        if (fileInfo.getType() == FileType.DIR) {
            Files.createDirectories(fullPath);
        } else {
            Files.deleteIfExists(fullPath);
            Files.createFile(fullPath);
            if (fileInfo.getSize() > 0) {
                operation.setTotalSize(operation.getTotalSize() + fileInfo.getSize());
                result = true;
            }
        }
        return result;
    }

    private <T> Predicate<T> wrapper(ThrowablePredicate<T> predicate) {
        return object -> {
            try {
                return predicate.test(object);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }
        };
    }
}
