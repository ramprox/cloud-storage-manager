package client.network.handlers;

import client.interfaces.Callback;
import client.interfaces.Callback0;
import client.network.Client;
import client.model.FileInfo;
import client.model.FileNameType;
import client.model.FileType;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

@ChannelHandler.Sharable
public class CommandHandler extends ChannelInboundHandlerAdapter {

    private Callback<FileInfo> createdDir;
    private Callback<FileInfo> createdFile;
    private Callback<FileInfo> renameFile;
    private Callback0 deletedFile;

    public void setDeletedFile(Callback0 deletedFile) {
        this.deletedFile = deletedFile;
    }

    public void setRenameFile(Callback<FileInfo> renameFile) {
        this.renameFile = renameFile;
    }

    public void setCreatedDir(Callback<FileInfo> createdDir) {
        this.createdDir = createdDir;
    }

    public void setCreatedFile(Callback<FileInfo> createdFile) {
        this.createdFile = createdFile;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String command = String.valueOf(msg);
        if(command.startsWith("mkdirOK ")) {
            String[] args = command.split(" ", 2);
            String fileInfo = args[1];
            args = fileInfo.split(" ");
            FileInfo fileInfo1 = new FileInfo(new FileNameType(args[0], FileType.Directory),
                    null, new Date(Long.parseLong(args[2])));
            if(createdDir != null) {
                createdDir.call(fileInfo1);
            }
        } else if(command.startsWith("touchOK ")) {
            String[] args = command.split(" ", 2);
            String fileInfo = args[1];
            args = fileInfo.split(" ");
            FileInfo fileInfo1 = new FileInfo(new FileNameType(args[0], FileType.File),
                    Long.parseLong(args[2]), new Date(Long.parseLong(args[3])));
            if(createdFile != null) {
                createdFile.call(fileInfo1);
            }
        } else if(command.startsWith("renameOK ")) {
            String[] args = command.split(" ", 2);
            String newStringFileInfo = args[1];
            FileInfo newFileInfo = getFileInfo(newStringFileInfo);
            if(renameFile != null) {
                renameFile.call(newFileInfo);
            }
        } else if(command.equals("deleteOK")) {
            if(deletedFile != null) {
                deletedFile.call();
            }
        }
    }

    private FileInfo getFileInfo(String fileInfo) {
        String[] args = fileInfo.split(" ");
        String fileName = args[0];
        String fileType = args[1];
        FileInfo fileInfo1;
        if(fileType.equals("dir")) {
            FileNameType fileNameType = new FileNameType(fileName, FileType.Directory);
            fileInfo1 = new FileInfo(fileNameType, null, new Date(Long.parseLong(args[2])));
        } else {
            FileNameType fileNameType = new FileNameType(fileName, FileType.File);
            fileInfo1 = new FileInfo(fileNameType, Long.parseLong(args[2]), new Date(Long.parseLong(args[3])));
        }
        return fileInfo1;
    }
}
