package client.network.service;

import client.network.annotations.RequestHandler;
import client.network.annotations.ResponseHandler;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.DirFilesDto;
import interop.dto.Message;
import interop.dto.RenameFileDto;
import interop.dto.SearchFileDto;
import interop.dto.fileinfo.FileInfo;
import io.netty.channel.Channel;

import java.util.List;

public abstract class SimpleFileCommandServiceImpl implements SimpleFileCommandService {

    private final ServerEventsListener serverEventsListener;

    protected SimpleFileCommandServiceImpl(ServerEventsListener serverEventsListener) {
        this.serverEventsListener = serverEventsListener;
    }

    protected abstract Channel getChannel();

    @Override
    @RequestHandler(command = Command.CHANGE_DIR)
    public void changeDirRequest(String path) {
        Message message = new Message();
        message.setCommand(Command.CHANGE_DIR);
        message.setData(path);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.CHANGE_DIR)
    public void changeDirResponse(Message message) {
        DirFilesDto data = (DirFilesDto) message.getData();
        String currentDir = data.getDirPath();
        List<FileInfo> fileInfoList = data.getFileInfos();
        serverEventsListener.currentDirChanged(currentDir, fileInfoList);
    }

    @Override
    @RequestHandler(command = Command.GET_DIR_SIZE)
    public void getDirSizeRequest(Object... args) {
        String dirName = (String) args[0];
        Message message = new Message();
        message.setCommand(Command.GET_DIR_SIZE);
        message.setData(dirName);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.GET_DIR_SIZE)
    public void getDirSizeResponse(Message message) {
        FileInfo fileInfo = (FileInfo) message.getData();
        serverEventsListener.viewDirSizeOnServer(fileInfo);
    }

    @Override
    @RequestHandler(command = Command.CREATE_FILE)
    public void createFileRequest(String filePath) {
        createFile(Command.CREATE_FILE, filePath);
    }

    @Override
    @ResponseHandler(command = Command.CREATE_FILE)
    public void createFileResponse(Message message) {
        FileInfo fileInfo = (FileInfo) message.getData();
        serverEventsListener.fileCreated(fileInfo);
    }

    @Override
    @RequestHandler(command = Command.CREATE_DIR)
    public void createDirRequest(String filePath) {
        createFile(Command.CREATE_DIR, filePath);
    }

    @Override
    @ResponseHandler(command = Command.CREATE_DIR)
    public void createDirResponse(Message message) {
        this.createFileResponse(message);
    }

    @Override
    @RequestHandler(command = Command.DELETE)
    public void deleteFileRequest(String filePath) {
        Message message = new Message();
        message.setCommand(Command.DELETE);
        message.setData(filePath);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.DELETE)
    public void deleteFileResponse(Message message) {
        String deletedFilePath = (String) message.getData();
        serverEventsListener.fileDeleted(deletedFilePath);
    }

    @Override
    @RequestHandler(command = Command.RENAME)
    public void renameFileRequest(String oldFilePath, String newFilePath) {
        Message message = new Message();
        message.setCommand(Command.RENAME);
        RenameFileDto data = new RenameFileDto(oldFilePath, newFilePath);
        message.setData(data);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.RENAME)
    public void renameFileResponse(Message message) {
        RenameFileDto data = (RenameFileDto) message.getData();
        String oldFilePath = data.getOldFileName();
        String newFilePath = data.getNewFileName();
        serverEventsListener.fileRenamed(oldFilePath, newFilePath);
    }

    @Override
    @RequestHandler(command = Command.SEARCH)
    public void searchFileRequest(String startPath, String fileName) {
        Message message = new Message();
        message.setCommand(Command.SEARCH);
        SearchFileDto searchFileDto = new SearchFileDto(startPath, fileName);
        message.setData(searchFileDto);
        getChannel().writeAndFlush(message);
    }

    @Override
    @ResponseHandler(command = Command.SEARCH)
    public void searchFileResponse(Message message) {
        serverEventsListener.foundedFilesReceived((List<FileInfo>) message.getData());
    }

    private void createFile(Command command, String path) {
        Message message = new Message();
        message.setCommand(command);
        message.setData(path);
        getChannel().writeAndFlush(message);
    }
}
