package server;


import java.io.*;

public class MainServerApp {
    /**
     * Точка входа в приложение
     * @param args аргументы командной строки
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        new Server();
    }
}
