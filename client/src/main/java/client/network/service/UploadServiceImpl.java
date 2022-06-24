package client.network.service;

import client.network.annotations.RequestHandler;
import client.network.annotations.ResponseHandler;
import client.network.model.UploadOperation;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.ProgressInfoDto;
import interop.model.LoadingFiles;
import interop.dto.Message;
import interop.dto.DeepFilesDto;
import interop.service.ChunkedFileReader;
import interop.service.FileInfoService;
import io.netty.channel.Channel;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedStream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class UploadServiceImpl implements UploadService {

    private final Queue<UploadOperation> operations = new LinkedList<>();

    private final FileInfoService fileInfoService;

    private final ServerEventsListener serverEventsListener;

    private final int loadBufferSize;

    public UploadServiceImpl(FileInfoService fileInfoService,
                             ServerEventsListener serverEventsListener,
                             int loadBufferSize) {
        this.fileInfoService = fileInfoService;
        this.serverEventsListener = serverEventsListener;
        this.loadBufferSize = loadBufferSize;
    }

    protected abstract Channel getChannel();

    @Override
    @RequestHandler(command = Command.UPLOAD)
    public void uploadRequest(String sourcePath, List<String> fileNames, String destinationPath) {
        LoadingFiles files = fileInfoService.formFilesLists(sourcePath, fileNames);
        List<String> nonZeroSizeRelativeFilePaths = files.getSenderPaths();
        if(nonZeroSizeRelativeFilePaths.size() > 0) {
            UploadOperation operation = new UploadOperation(sourcePath, nonZeroSizeRelativeFilePaths, destinationPath);
            operations.add(operation);
        }
        Message message = new Message();
        message.setCommand(Command.UPLOAD);
        DeepFilesDto deepFilesDtoData =
                new DeepFilesDto(destinationPath, files.getRecipientFiles());
        message.setData(deepFilesDtoData);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.READY_UPLOAD)
    public void readyUpload(Message message) throws IOException {
        UploadOperation operation = operations.poll();
        if (operation != null) {
            for (String path : operation.getFiles()) {
                Path destination = Paths.get(operation.getDestination(), path);
                Path fullSenderPath = Paths.get(operation.getSourcePath(), path);
                sendFile(fullSenderPath, destination);
            }
            getChannel().flush();
        }
    }

    private void sendFile(Path fullPath, Path destination) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(fullPath.toFile());
        ChunkedStream chunkedStream = new ChunkedStream(inputStream, loadBufferSize);
        ChunkedInput<Message> fileReader = new ChunkedFileReader(chunkedStream, destination, Command.UPLOADING);
        getChannel().write(fileReader);
    }

    @Override
    @ResponseHandler(command = Command.PERCENT_UPLOAD)
    public void percentUpload(Message message) {
        ProgressInfoDto progressInfoDto = (ProgressInfoDto) message.getData();
        serverEventsListener.progressUpload(progressInfoDto.getTotalPercentage(), progressInfoDto.getCurrentFilePath());
    }

    @Override
    @ResponseHandler(command = Command.UPLOAD_DONE)
    public void uploadDone(Message message) {
        String path = (String) message.getData();
        serverEventsListener.uploadDone(path);
    }
}
