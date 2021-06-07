package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class SendFilesHandler extends ChunkedWriteHandler {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        File file = (File)msg;
        ChunkedStream stream = new ChunkedStream(new FileInputStream(file));
        super.write(ctx, stream, promise);
    }
}
