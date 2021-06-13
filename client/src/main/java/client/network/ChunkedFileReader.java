package client.network;

import interop.Command;
import interop.model.Message;
import io.netty.buffer.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.*;

/**
 * Класс, являющийся оболочкой над ChunkedStream
 */
public class ChunkedFileReader implements ChunkedInput<Message> {

    private final ChunkedStream stream;

    public ChunkedFileReader(ChunkedStream stream) {
        this.stream = stream;
    }


    @Override
    public boolean isEndOfInput() throws Exception {
        return stream.isEndOfInput();
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }

    @Override
    public Message readChunk(ChannelHandlerContext ctx) throws Exception {
        return readChunk(ctx.alloc());
    }

    /**
     * Добавление команды UPLOADING в качестве заголовка сообщения
     * @param byteBufAllocator выделитель памяти
     * @return объект типа Message, содержащий массив прочитанных байтов из файла
     * @throws Exception может произойти при чтении байтов из файла
     */
    @Override
    public Message readChunk(ByteBufAllocator byteBufAllocator) throws Exception {
        ByteBuf buf = stream.readChunk(byteBufAllocator);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        return new Message(Command.UPLOADING, data);
    }

    @Override
    public long length() {
        return stream.length();
    }

    @Override
    public long progress() {
        return stream.length();
    }
}
