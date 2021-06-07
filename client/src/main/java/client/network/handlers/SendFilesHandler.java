package client.network.handlers;

import client.interfaces.Presentable;
import client.network.Client;
import interop.model.responses.UploadResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class SendFilesHandler extends ChunkedWriteHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        File file = (File)msg;
        ChunkedStream stream = new ChunkedStream(new FileInputStream(file));
        super.write(ctx, stream, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf)msg;
        double percent = buf.readDouble();
        buf.release();
        UploadResponse response = new UploadResponse(percent);
        ctx.fireChannelRead(response);
    }
}
