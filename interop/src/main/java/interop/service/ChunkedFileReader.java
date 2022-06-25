package interop.service;

import interop.Command;
import interop.dto.Message;
import interop.dto.FileChunkDto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedStream;

import java.nio.file.Path;

/**
 * Класс, являющийся оболочкой над ChunkedStream
 */
public class ChunkedFileReader implements ChunkedInput<Message> {

    private final ChunkedStream stream;

    private final Path path;

    private final Command command;

    public ChunkedFileReader(ChunkedStream stream, Path path, Command command) {
        this.stream = stream;
        this.path = path;
        this.command = command;
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
        Message message = new Message();
        message.setCommand(command);
        FileChunkDto fileChunkDto = new FileChunkDto(path.toString(), data);
        message.setData(fileChunkDto);
        return message;
    }

    @Override
    public long length() {
        return stream.length();
    }

    @Override
    public long progress() {
        return stream.progress();
    }
}
