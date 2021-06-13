package server.network.handlers;

import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.*;
import io.netty.channel.*;
import server.network.Server;
import server.model.User;
import server.util.ApplicationUtil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class UploadHandler extends SimpleChannelInboundHandler<Message> {

    private Queue<FileInfo> uploadingFiles;     // очередь загружаемых файлов с ненулевым размером
    private FileOutputStream fos;
    private User user;
    private long fullQueueSize;                 // общий размер файлов
    private long generalCurPos;                 // позиция в общем загружаемом списке
    private long curPosForFile;
    private String curDir;                      // Директория в которую копируются файлы

    /**
     * При добавлении в канал полчает пользователя
     * @param ctx контекст канала
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        user = Server.getUserByChannel(ctx.channel());
    }

    /**
     * Обработка чтения данных из канала
     * @param ctx контекст канала
     * @param request принятое сообщение
     * @throws Exception может возникнуть при чтении атрибутов файла, при открытии FileOutputStream для записи байтов
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message request) throws Exception {
        Command command = request.getCommand();
        if(command == Command.UPLOAD) {
            handleCommandUpload(ctx, request);
        } else if(command == Command.UPLOADING) {
            handleCommandUploading(ctx, request);
        } else {
            ctx.fireChannelRead(request);
        }
    }

    private void handleCommandUpload(ChannelHandlerContext ctx, Message requestResponse) throws IOException {
        Object[] data = (Object[])requestResponse.getData();
        curDir = ((String) data[0]);
        List<FileInfo> fullList = (List<FileInfo>) data[1];
        createDirsAndZeroSizeFiles(curDir, fullList);
        Queue<FileInfo> filteredFiles = fullList.stream()
                .filter(fileInfo -> fileInfo.getType() != FileType.DIR && fileInfo.getSize() > 0)
                .collect(Collectors.toCollection(ArrayDeque::new));
        if(filteredFiles.size() != 0) {
            uploadingFiles = filteredFiles;
            fullQueueSize = curPosForFile = generalCurPos = 0;
            for(FileInfo file : uploadingFiles) {
                fullQueueSize += file.getSize();
            }
            Message response = new Message(Command.READY_UPLOAD, null);
            ctx.writeAndFlush(response);
        } else {
            Object[] dataResponse = new Object[2];
            dataResponse[0] = curDir;
            String curDirPath = curDir.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString());
            List<Path> paths = Arrays.stream(Objects.requireNonNull(new File(curDirPath).listFiles()))
                    .map(file -> Paths.get(curDirPath).resolve(file.getName()))
                    .collect(Collectors.toList());
            dataResponse[1] = ApplicationUtil.getFileInfos(paths);
            Message response = new Message(Command.UPLOAD_DONE, data);
            ctx.writeAndFlush(response);
        }
    }

    /**
     * Создание директорий и файлов с нулевым размером
     * @param curDir директория куда загружаются файлы
     * @param files список загружаемых файлов
     * @throws IOException может возникнуть при создании файлов
     */
    private void createDirsAndZeroSizeFiles(String curDir, List<FileInfo> files) throws IOException {
        for(FileInfo fileInfo : files) {
            Path path = Paths.get(curDir.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString())).resolve(fileInfo.getFileName());
            if(fileInfo.getType() == FileType.DIR) {
                path.toFile().mkdirs();
            } else if(fileInfo.getType() == FileType.FILE) {
                if(fileInfo.getSize() == 0) {
                    Files.deleteIfExists(path);
                    Files.createFile(path);
                }
            }
        }
    }

    /**
     * Запись принятых байтов в файлы
     * @param ctx контекст канала
     * @param requestResponse
     * @throws IOException
     */
    private void handleCommandUploading(ChannelHandlerContext ctx, Message requestResponse) throws IOException {
        byte[] data = (byte[])requestResponse.getData();

        FileInfo fileInfo = uploadingFiles.peek();
        String fileName = fileInfo.getFileName();
        String path = user.getCurrentDir().resolve(fileName).toString();
        if(curPosForFile == 0) {
            fos = new FileOutputStream(path);
        }
        fos.write(data);
        fos.flush();
        curPosForFile += data.length;
        if(curPosForFile == fileInfo.getSize()) {
            curPosForFile = 0;
            uploadingFiles.poll();
            fos.close();
        }

        generalCurPos += data.length;
        if(generalCurPos < fullQueueSize) {
            Object[] responseData = new Object[2];
            double percentUpload = generalCurPos * 1.0 / fullQueueSize;
            responseData[0] = percentUpload;
            responseData[1] = uploadingFiles.peek().getFileName();
            Message percentResponse = new Message(Command.PERCENT_UPLOAD, responseData);
            ctx.writeAndFlush(percentResponse);
        } else {
            Object[] response = new Object[2];
            response[0] = curDir;
            String curDirPath = curDir.replace(ApplicationUtil.USER_ROOT_SYMBOL, user.getHomeDir().toString());
            List<Path> paths = Arrays.stream(Objects.requireNonNull(new File(curDirPath).listFiles()))
                    .map(file -> Paths.get(curDirPath).resolve(file.getName()))
                    .collect(Collectors.toList());
            response[1] = ApplicationUtil.getFileInfos(paths);
            Message percentResponse = new Message(Command.UPLOAD_DONE, response);
            ctx.writeAndFlush(percentResponse);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(fos != null) {
            fos.close();
        }
        Message response = new Message(Command.ERROR, cause.getMessage());
        ctx.writeAndFlush(response);
    }
}
