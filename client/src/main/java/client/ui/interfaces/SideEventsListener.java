package client.ui.interfaces;

import interop.dto.fileinfo.FileInfo;

import java.nio.file.Path;

/**
 * Интерферйс для обработки событий мыши или клавиатуры с SideController
 */
public interface SideEventsListener {

    void changeDir(FileInfo fileInfo);

    void driveChanged(String newPath);

    void sizeClicked(Path path);

    void searchFile(String fileName);

}
