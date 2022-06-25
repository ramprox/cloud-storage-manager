package client.network.service;

import client.ui.interfaces.ServerEventsListener;
import interop.Command;
import interop.dto.DirFilesDto;
import interop.dto.Message;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;
import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class SimpleFileCommandServiceImplTest {

    private SimpleFileCommandServiceImpl simpleFileCommandService;

    @Mock
    private Channel channel;

    @Mock
    private ServerEventsListener serverEventsListener;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        simpleFileCommandService = new SimpleFileCommandServiceImpl(serverEventsListener) {
            @Override
            protected Channel getChannel() {
                return channel;
            }
        };
    }

    @Test
    public void changeDirRequestTest() {
        Message expectedMessage = new Message();
        String expectedPath = "~/test";
        expectedMessage.setCommand(Command.CHANGE_DIR);
        expectedMessage.setData(expectedPath);
        simpleFileCommandService.changeDirRequest(expectedPath);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(channel).writeAndFlush(captor.capture());
        Message message = (Message) captor.getValue();
        assertEquals(expectedMessage.getCommand(), message.getCommand());
        assertEquals(expectedMessage.getData(), message.getData());
    }

    @Test
    public void changeDirResponseTest() {
        String expectedDir = "~/test";
        List<FileInfo> expectedFileInfos = Arrays.asList(
                new FileInfo(FileType.FILE, "1.txt", 20, 987654321, 123456789),
                new FileInfo(FileType.FILE, "2.txt", 40, 777888999, 112233445)
        );
        Message message = new Message();
        message.setCommand(Command.CHANGE_DIR);
        DirFilesDto data = new DirFilesDto(expectedDir, new ArrayList<>(expectedFileInfos));
        message.setData(data);
        ArgumentCaptor<String> curDirCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<FileInfo>> fileInfosCaptor = ArgumentCaptor.forClass(List.class);

        simpleFileCommandService.changeDirResponse(message);

        verify(serverEventsListener).currentDirChanged(curDirCaptor.capture(), fileInfosCaptor.capture());
        assertEquals(expectedDir, curDirCaptor.getValue());
        List<FileInfo> resultFileInfos = fileInfosCaptor.getValue();
        assertEquals(expectedFileInfos.size(), resultFileInfos.size());
        for(int i = 0; i < expectedFileInfos.size(); i++) {
            assertEquals(expectedFileInfos.get(i), resultFileInfos.get(i));
        }
    }

}
