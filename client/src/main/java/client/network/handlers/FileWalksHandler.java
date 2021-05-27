package client.network.handlers;

import client.interfaces.Callback1;
import client.network.Client;
import client.model.FileInfo;
import client.model.FileNameType;
import client.model.FileType;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

@ChannelHandler.Sharable
public class FileWalksHandler extends ChannelInboundHandlerAdapter {
    private Callback1<FileInfo[], String> readedFileInfoFromServer;

    public void setReadedFileInfoFromServer(Callback1<FileInfo[], String> readedFileInfoFromServer) {
        this.readedFileInfoFromServer = readedFileInfoFromServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = String.valueOf(msg);
        decodeFileInfo(ctx, message);
    }

    private void decodeFileInfo(ChannelHandlerContext ctx, String message) {
        String[] args = message.split("\r\n", 2);
        int length = Integer.parseInt(args[0]);
        FileInfo[] fileInfos = new FileInfo[length];
        args = args[1].split("\r\n", 2);
        String currentPath = args[0];
        String[] files = args[1].split("\r\n");
        for (int i = 0; i < length; i++) {
            String[] filedata = files[i].split(" ");
            String filename = filedata[0];
            FileType type = filedata[1].equals("dir") ? FileType.Directory : FileType.File;
            FileNameType fileNameType = new FileNameType(filename, type);
            Date date = new Date(Long.parseLong(filedata[2]));
            if (type == FileType.File) {
                long size = Long.parseLong(filedata[3]);
                fileInfos[i] = new FileInfo(fileNameType, size, date);
            } else {
                fileInfos[i] = new FileInfo(fileNameType, null, date);
            }
        }
        if(readedFileInfoFromServer != null) {
            readedFileInfoFromServer.call(fileInfos, currentPath);
        }
    }
}
