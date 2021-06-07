package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.fileinfo.RegularFile;
import interop.model.responses.ErrorResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.FileOutputStream;
import java.nio.file.Path;

/**
 * Класс обработчика ответственный за загрузку файлов на клиент
 */
public class FileWriterHandler extends ChannelInboundHandlerAdapter {
    
    private final RegularFile[] uploadingList;    // список файлов имеющих ненулевой размер
    private FileOutputStream fos;                 //
    private final Path currentPath;               // текущая директория клиента
    private long curPosForFile;                   // текущая позиция в загружаемом файле
    private long size;                            // размер текущего загружаемого файла
    private long generalSize;                     // общий размер файлов
    private int curIndexInUploadingList;          // индекс текущего загружаемого файла
    private long generalCurPos;                   // позиция в общем потоке байт
    private final Presentable presentable;        // ссылка на объект класса, получающего уведомления о ходе загрузки

    /**
     * Конструктор
     * @param uploadingList список файлов имеющих ненулевой размер
     * @param currentPath текущий путь клиента
     */
    public FileWriterHandler(RegularFile[] uploadingList, Path currentPath) {
        this.uploadingList = uploadingList;
        this.currentPath = currentPath;
        curIndexInUploadingList = 0;
        size = uploadingList[curIndexInUploadingList].getSize();
        for(RegularFile file : uploadingList) {
            generalSize += file.getSize();
        }
        presentable = Client.getPresentable();
    }

    /**
     * В методе происходит распределение байтов по соответствующим файлам
     * @param ctx контекст
     * @param msg сообщение в виде ByteBuf
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] data = getBytes((ByteBuf)msg);
        int readedBytesFromData = 0;
        while(readedBytesFromData != data.length) {
            if(curPosForFile == 0) {
                String path = currentPath.resolve(uploadingList[curIndexInUploadingList].getFileName()).toString();
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
                curIndexInUploadingList++;
                if(curIndexInUploadingList < uploadingList.length) {
                    size = uploadingList[curIndexInUploadingList].getSize();
                }
            }
        }
        generalCurPos += data.length;
        double downloadPercent = generalCurPos * 1.0 / generalSize;
        if(downloadPercent == 1.0) {
            ctx.channel().pipeline().remove(this);
        }
        presentable.progressDownload(downloadPercent);
    }

    /**
     * Запись всех доступных байтов из ByteBuf в массив байтов
     * @param buf копирумый ByteBuf
     * @return массив скопированных байтов
     */
    private byte[] getBytes(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        return data;
    }

    /**
     * Происходит при ошибках загрузки
     * @param ctx контекст
     * @param cause 
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(fos != null) {
            fos.close();
        }
        ErrorResponse response = new ErrorResponse(cause.getMessage());
        ctx.fireChannelRead(response);
    }
}
