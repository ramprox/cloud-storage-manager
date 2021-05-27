package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.Server;
import server.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class CommandInboundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
        Server.unsubscribeUser(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
        ctx.close();
        Server.unsubscribeUser(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handleCommands(ctx, String.valueOf(msg));
    }

    private void handleCommands(ChannelHandlerContext ctx, String command) {
        if(command.startsWith("cd ")) {
            String[] args = command.split(" ");
            String newPath = args[1];
            changeDir(ctx, newPath);
        } else if(command.startsWith("mkdir ")) {
            String[] args = command.split(" ");
            String dirName = args[1];
            mkdir(ctx, dirName);
        } else if(command.startsWith("touch ")) {
            String[] args = command.split(" ");
            String fileName = args[1];
            touchFile(ctx, fileName);
        } else if(command.startsWith("rename ")) {
            String[] args = command.split(" ");
            String oldFileName = args[1];
            String newFileName = args[2];
            rename(ctx, oldFileName, newFileName);
        } else if(command.startsWith("delete ")) {
            String[] args = command.split(" ");
            String fileName = args[1];
            delete(ctx, fileName);
        }
    }

    private void changeDir(ChannelHandlerContext ctx, String path) {
        User user = Server.getUserByChannel(ctx.channel());
        if(path.equals("..")) {
            Path parentDir = user.getCurrentDir().getParent();
            if(!user.getCurrentDir().equals(user.getHomeDir())) {
                user.setCurrentDir(parentDir);
                sendDirInfo(ctx, user);
            }
        } else if(path.equals("~")) {
            user.setCurrentDir(user.getHomeDir());
            sendDirInfo(ctx, user);
        } else {
            Path newPath = user.getCurrentDir().resolve(Paths.get(path));
            if(Files.isDirectory(newPath)) {
                user.setCurrentDir(newPath);
                sendDirInfo(ctx, user);
            } else {
                ctx.channel().writeAndFlush("/error: " + path + " is not a directory or not exist");
            }
        }
    }

    private void sendDirInfo(ChannelHandlerContext ctx, User user) {
        StringBuilder sb = new StringBuilder();
        Path currentDir = user.getCurrentDir();
        File[] files = currentDir.toFile().listFiles();
        int length = files != null ? files.length : 0;
        sb.append(Long.toString(length)).append("\r\n");
        sb.append(user.getPrompt()).append("\r\n");
        if(files != null) {
            for(File file : files) {
                sb.append(file.getName()).append(" ");
                boolean isDir = Files.isDirectory(file.toPath());
                if(isDir) {
                    sb.append("dir").append(" ");
                    sb.append(file.lastModified()).append("\r\n");
                } else {
                    sb.append("file").append(" ");
                    sb.append(file.lastModified()).append(" ");
                    sb.append(file.length()).append("\r\n");
                }
            }
        }
        ctx.channel().writeAndFlush(sb.toString());
    }

    private void mkdir(ChannelHandlerContext ctx, String dirName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path tempPath = user.getCurrentDir().resolve(Paths.get(dirName));
            Files.createDirectory(user.getCurrentDir().resolve(Paths.get(dirName)));
            File file = tempPath.toFile();
            ctx.channel().writeAndFlush("mkdirOK " + file.getName() + " dir " + file.lastModified());
        } catch (IOException e) {
            ctx.channel().writeAndFlush("/error " + e.getMessage());
        }
    }

    private void touchFile(ChannelHandlerContext ctx, String fileName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path tempPath = user.getCurrentDir().resolve(Paths.get(fileName));
            Files.createFile(user.getCurrentDir().resolve(Paths.get(fileName)));
            File file = tempPath.toFile();
            ctx.channel().writeAndFlush("touchOK " + file.getName() + " file " + file.length() + " " + file.lastModified());
        } catch (IOException e) {
            ctx.channel().writeAndFlush("/error " + e.getMessage());
        }
    }

    private void rename(ChannelHandlerContext ctx, String oldFileName, String newFileName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path oldPath = user.getCurrentDir().resolve(oldFileName);
            Path newPath = user.getCurrentDir().resolve(newFileName);
            File file = Files.move(oldPath, newPath).toFile();
            String newFileInfo = getFileInfo(file);
            String result = "renameOK " + newFileInfo;
            ctx.channel().writeAndFlush(result);
        } catch (IOException e) {
            ctx.channel().writeAndFlush("/error " + e.getMessage());
        }
    }

    private String getFileInfo(File file) {
        String result = file.getName() + " ";
        if(file.isDirectory()) {
            result += "dir " + file.lastModified();
        } else {
            result += "file " + file.length() + " " + file.lastModified();
        }
        return result;
    }

    private void delete(ChannelHandlerContext ctx, String fileName) {
        try {
            User user = Server.getUserByChannel(ctx.channel());
            Path filePath = user.getCurrentDir().resolve(fileName);
            if(Files.isDirectory(filePath)) {
                Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(filePath);
            }
            ctx.channel().writeAndFlush("deleteOK");
        } catch (IOException e) {
            ctx.channel().writeAndFlush("/error " + e.getMessage());
        }
    }
}
