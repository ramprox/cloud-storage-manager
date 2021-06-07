package server.handlers;

import interop.model.requests.ChangeDir;
import interop.model.responses.*;
import io.netty.channel.*;
import server.Server;
import server.model.User;
import server.util.Conversations;
import java.io.File;
import java.nio.file.*;

/**
 * Класс обработчика, ответственный за обработку запроса перехода в другую директорию на сервере
 */
public class ChangeDirHandler extends SimpleChannelInboundHandler<ChangeDir> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChangeDir changeDir) throws Exception {
        User user = Server.getUserByChannel(ctx.channel());
        String tmpPath = changeDir.getPath();
        Path newPath = Paths.get(Server.SERVER_FOLDER, tmpPath);
        if(Files.exists(newPath)) {
            user.setCurrentDir(newPath);
            File[] files = newPath.toFile().listFiles();
            ChangeDirResp response = new ChangeDirResp(user.getPrompt(), Conversations.getFileInfos(files));
            ctx.writeAndFlush(response);
        } else {
            ErrorResponse response = new ErrorResponse(newPath + " is not exist!");
            ctx.writeAndFlush(response);
        }
    }
}
