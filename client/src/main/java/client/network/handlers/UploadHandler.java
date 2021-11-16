package client.network.handlers;

import client.ui.interfaces.Presentable;
import client.network.service.ChunkedFileReader;
import client.network.Client;
import client.utils.ApplicationUtil;
import interop.Command;
import interop.model.Message;
import interop.model.fileinfo.FileInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class UploadHandler extends ChunkedWriteHandler {

    private List<File> uploadingFiles;

    public void setUploadingFiles(List<File> uploadingFiles) {
        this.uploadingFiles = uploadingFiles;
    }

    /**
     * Обработка передачи сообщения
     * @param ctx контекст канала
     * @param msg если msg является объектом типа ChunkedFileReader, то осуществляется передача файлов,
     *            если нет - передача следующему Handler в цепочке
     * @param promise объект типа Promise
     * @throws Exception может возникнуть при передаче файлов
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof ChunkedFileReader) {
            super.write(ctx, msg, promise);
        } else {
            ctx.writeAndFlush(msg);
        }
    }

    /**
     * Обработка чтения сообщения
     * @param ctx контекст канала
     * @param msg если msg содержит команды связанные с Upload, то происходит обработка
     *            если нет - передача следующему Handler в цепочке
     * @throws Exception может произойти при обработке команд связанных с Upload
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof Message) {
            Message response = (Message)msg;
            Command command = response.getCommand();
            if(command == Command.READY_UPLOAD) {
                for(File file : uploadingFiles) {
                    write(ctx, new ChunkedFileReader(new ChunkedStream(new FileInputStream(file),
                            ApplicationUtil.LOAD_BUFFER_SIZE)), ctx.newPromise());
                }
                flush(ctx);
            } else if (command == Command.PERCENT_UPLOAD) {
                Presentable presentable = Client.getPresentable();
                Object[] data = (Object[]) response.getData();
                double percent = (double) data[0];
                String fileName = (String) data[1];
                presentable.progressUpload(percent, fileName);
            } else if(command == Command.UPLOAD_DONE) {
                Object[] data = (Object[]) response.getData();
                String currentdir = (String) data[0];
                List<FileInfo> fileInfoList = (List<FileInfo>) data[1];
                Presentable presentable = Client.getPresentable();
                presentable.uploadDone(currentdir, fileInfoList);
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }


}
