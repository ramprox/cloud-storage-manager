package client.interfaces;

import client.controllers.SideController;

/**
 * Интерферйс для MainWindowController для обработки событий мыши или клавиатуры с SideController
 */
public interface SideEventsProcessable {
    void changeDir(SideController controller, String newPath);
    void driveChanged(String newPath);
    void sizeClicked(SideController controller, String dirPath);
    void searchFile(SideController controller, String fileName);
}
