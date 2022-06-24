package interop.model;

import interop.dto.fileinfo.FileInfo;

import java.util.LinkedList;
import java.util.List;

public class LoadingFiles {

    private final List<String> senderPaths = new LinkedList<>();

    private final List<FileInfo> recipientFiles = new LinkedList<>();

    public List<String> getSenderPaths() {
        return senderPaths;
    }

    public List<FileInfo> getRecipientFiles() {
        return recipientFiles;
    }
}
