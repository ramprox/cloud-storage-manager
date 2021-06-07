package server.handlers;

import interop.model.fileinfo.RegularFile;
import interop.model.responses.UploadResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.Server;
import server.model.User;
import java.io.FileOutputStream;

/**
 * Класс обработчика, ответственный за запись приходящих от клиента байтов в файлы
 */
public class FileWriterHandler extends ChannelInboundHandlerAdapter {

    private final RegularFile[] uploadingList; // список файлов
    private FileOutputStream fos;              //
    private User user;
    private long curPosForFile;                // текущая позиция для текуще загружаемого файла
    private long size;                         // размер текущего загружаемого файла
    private long generalSize;                  // общий размер файлов
    private int curFileInfo;                   // индекс текущего файла в списек файлов
    private long generalCurPos;                // позиция в общем загружаемом списке

    public FileWriterHandler(RegularFile[] uploadingList) {
        this.uploadingList = uploadingList;
        curFileInfo = 0;
        size = uploadingList[curFileInfo].getSize();
        for(RegularFile file : uploadingList) {
            generalSize += file.getSize();
        }
    }

    /**
     * При добавлении в канал полчает пользователя
     * @param ctx контекст канала
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        user = Server.getUserByChannel(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] data = getBytes((ByteBuf)msg);
        int readedBytesFromData = 0;
        while(readedBytesFromData != data.length) {
            if(curPosForFile == 0) {
                String path = user.getCurrentDir().resolve(uploadingList[curFileInfo].getFileName()).toString();
                fos = new FileOutputStream(path);
            }
            long needBytesForCurrentFile = size - curPosForFile;
            int willBeReaded = (int)Math.min(needBytesForCurrentFile, data.length - readedBytesFromData);
            fos.write(data, readedBytesFromData, willBeReaded);
            readedBytesFromData += willBeReaded;
            curPosForFile += willBeReaded;
            if(curPosForFile == size) {
                curPosForFile = 0;
                fos.close();
                curFileInfo++;
                if(curFileInfo < uploadingList.length) {
                    size = uploadingList[curFileInfo].getSize();
                }
            }
        }
        generalCurPos += data.length;

        double percentUpload = generalCurPos * 1.0 / generalSize;
        ByteBuf buf = ctx.alloc().directBuffer();
        buf.writeDouble(percentUpload);
        ctx.writeAndFlush(buf).addListener(future -> {
            if(percentUpload == 1.0) {
                ctx.pipeline().remove(FileWriterHandler.this);
            }
        });
    }

    /**
     * Преобразование из ByteBuf в byte[]
     * @param buf буфер ByteBuf
     * @return массив byte[]
     */
    private byte[] getBytes(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        return data;
    }

    /**
     * Исключения в канале
     * @param ctx контекст канала
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(fos != null) {
            fos.close();
        }
        cause.printStackTrace();
    }
}
