package client.network.handlers;

import client.ui.interfaces.Presentable;
import client.network.Client;
import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.FileType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class DownloadHandler extends SimpleChannelInboundHandler<Message> {

    private Path destinationDir;                  // путь к директории куда загружаются файлы
    private Queue<FileInfo> downloadingFiles;     // очередь загружаемых файлов с ненулевым размером
    private FileOutputStream fos;
    private long fullQueueSize;                   // общий размер файлов
    private long generalCurPos;                   // позиция в общем загружаемом списке
    private long curPosForFile;                   // текущая позиция для текуще загружаемого файла

    public void setDestinationDir(Path destinationDir) {
        this.destinationDir = destinationDir;
    }

    /**
     * Обработка ответов от сервера
     * @param ctx контекст канала
     * @param response ответ от сервера
     * @throws Exception может произойти при обработке ответа
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message response) throws Exception {
        Command command = response.getCommand();
        if(command == Command.DOWNLOAD) {
            List<FileInfo> fileInfoList = (List<FileInfo>) response.getData();
            handleDownloadResponse(ctx, fileInfoList);
        } else if(command == Command.DOWNLOADING) {
            handleDownloadingCommand((byte[])response.getData());
        } else {
            ctx.fireChannelRead(response);
        }
    }

    /**
     * Обработка ответа от сервера Command.DOWNLOAD. Сервер отправляет данное сообщение в ответ на запрос клиента
     * по команде Command.DOWNLOAD. Ответ содержит полный список файлов для загрузки из сервера включая директории
     * и файлы с нулевым размером
     * @param ctx контекст канала
     * @param fileInfoList полный список файлов
     * @throws IOException может возникнуть при создании директорий и пустых файлов
     */
    private void handleDownloadResponse(ChannelHandlerContext ctx, List<FileInfo> fileInfoList) throws IOException {
        createDirsAndZeroSizeFiles(fileInfoList);                                                      // создание директорий и пустых файлов
        Queue<FileInfo> filteredFiles = fileInfoList.stream()                                          // фильтрация полного списка
                .filter(fileInfo -> fileInfo.getType() != FileType.DIR && fileInfo.getSize() > 0)      // из списка удаляются файлы имеющие тип директории
                .collect(Collectors.toCollection(ArrayDeque::new));                                    // и файлы с нулевым размером
        if(filteredFiles.size() != 0) {
            downloadingFiles = filteredFiles;
            fullQueueSize = curPosForFile = generalCurPos = 0;
            for(FileInfo file : downloadingFiles) {
                fullQueueSize += file.getSize();
            }
            Message response = new Message(Command.READY_DOWNLOAD, null);
            ctx.writeAndFlush(response);
        } else {
            Presentable presentable = Client.getPresentable();
            presentable.downloadDone(destinationDir.toString());
        }
    }

    /**
     * Создание директорий и файлов с нулевым размером
     * @param files список загружаемых файлов
     * @throws IOException может возникнуть при создании файлов
     */
    private void createDirsAndZeroSizeFiles(List<FileInfo> files) throws IOException {
        for(FileInfo fileInfo : files) {
            Path path = destinationDir.resolve(fileInfo.getFileName());
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
     * Обработка сообщения с командой Command.DOWNLOADING. Сообщение будет содержать байты для записи в файл
     * @param data массив байт для записи в файл
     * @throws IOException сожет возникнуть при записи в файл
     */
    private void handleDownloadingCommand(byte[] data) throws IOException {

        FileInfo fileInfo = downloadingFiles.peek();
        String fileName = fileInfo.getFileName();
        String path = destinationDir.resolve(fileName).toString();
        if(curPosForFile == 0) {
            fos = new FileOutputStream(path);
        }
        fos.write(data);
        fos.flush();
        curPosForFile += data.length;
        if(curPosForFile == fileInfo.getSize()) {
            curPosForFile = 0;
            downloadingFiles.poll();
            fos.close();
        }
        generalCurPos += data.length;
        if(generalCurPos < fullQueueSize) {
            double percentUpload = generalCurPos * 1.0 / fullQueueSize;
            Presentable presentable = Client.getPresentable();
            presentable.progressDownload(percentUpload, destinationDir.resolve(downloadingFiles.peek().getFileName()).toString());
        } else {
            Presentable presentable = Client.getPresentable();
            presentable.downloadDone(destinationDir.toString());
        }
    }

    /**
     * Происходит при ошибках в канале
     * @param ctx контекст канала
     * @param cause исключение - причина ошибки
     * @throws Exception может произойти при ошибках в канале
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(fos != null) {
            fos.close();
        }
        Presentable presentable = Client.getPresentable();
        presentable.exceptionCaught(cause);
    }
}
