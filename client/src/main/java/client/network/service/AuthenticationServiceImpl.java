package client.network.service;

import client.network.annotations.RequestHandler;
import client.network.annotations.ResponseHandler;
import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.AuthDto;
import interop.dto.DirFilesDto;
import interop.dto.Message;
import interop.dto.fileinfo.FileInfo;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class AuthenticationServiceImpl implements AuthenticationService {

    private final ServerEventsListener serverEventsListener;

    @Autowired
    public AuthenticationServiceImpl(ServerEventsListener serverEventsListener) {
        this.serverEventsListener = serverEventsListener;
    }

    @Override
    @RequestHandler(command = Command.SIGN_IN)
    public void signInRequest(AuthDto authDto) {
        request(Command.SIGN_IN, authDto);
    }

    @Override
    @RequestHandler(command = Command.SIGN_UP)
    public void signUpRequest(AuthDto authDto) {
        request(Command.SIGN_UP, authDto);
    }

    @Override
    @ResponseHandler(command = Command.SIGN_IN)
    public void signInResponse(Message message) {
        DirFilesDto dirFilesDto = (DirFilesDto) message.getData();
        String currentDir = dirFilesDto.getDirPath();
        List<FileInfo> files = dirFilesDto.getFileInfos();
        serverEventsListener.clientSigned(currentDir, files);
    }

    @Override
    @ResponseHandler(command = Command.SIGN_UP)
    public void signUpResponse(Message message) {
        signInResponse(message);
    }

    private void request(Command command, AuthDto authDto) {
        Message request = new Message();
        request.setCommand(command);
        request.setData(authDto);
        getChannel().writeAndFlush(request);
    }

    protected abstract Channel getChannel();
}
