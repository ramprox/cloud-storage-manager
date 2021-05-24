package client.MainWindow.fileinfos;

public class FileNameType {
    private String name;
    private FileType type;

    public FileNameType(String name, FileType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public FileType getType() {
        return type;
    }
}
