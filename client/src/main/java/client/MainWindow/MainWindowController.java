package client.MainWindow;

import client.DialogWindows.Connection.ConnectionStage;
import client.MainWindow.fileinfos.*;
import client.MainWindow.handlers.InboundMessageHandler;
import client.MainWindow.table.FileNameCellFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.beans.binding.Bindings;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;

public class MainWindowController {

    private String host;
    private int port;
    private ChannelFuture channelFuture;
    private Path currentDir;
    @FXML
    private TableView<FileInfo> clientTable;
    @FXML
    private TableColumn<FileInfo, FileNameType> fileName;
    @FXML
    private TableColumn<FileInfo, Long> fileSize;
    @FXML
    private TableColumn<FileInfo, String> fileDate;
    @FXML
    private ComboBox<File> clientDrives;
    @FXML
    private Label clientPath;

    public final String parentDir = "[ . . ]";

    private Set<FileInfo> treeSet = new TreeSet<>();

    public void init() {
        fileName.setCellValueFactory(new PropertyValueFactory<>("fileNameType"));
        fileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        fileDate.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        fileName.setCellFactory(new FileNameCellFactory<>());
        currentDir = Paths.get(System.getProperty("user.home"));
        invalidateTable();
        clientTable.setOnMouseClicked(event -> {
            if(event.getClickCount() == 2) {
                FileInfo fileInfo = clientTable.getSelectionModel().getSelectedItem();
                if (fileInfo != null) {
                    if(fileInfo.getFileNameType().getName().equals(parentDir)) {
                        changeDirToParent();
                    } else if(fileInfo.getFileNameType().getType().equals(FileType.Directory)) {
                        setCurrentDirectory(fileInfo.getFileNameType().getName());
                    }
                }
            }
        });
        File[] paths = File.listRoots();
        clientDrives.setItems(FXCollections.observableArrayList(paths));
        clientDrives.getSelectionModel().select(currentDir.getRoot().toFile());
        clientPath.prefWidthProperty().bind(Bindings.divide(clientTable.widthProperty(), 1));
        clientPath.setText(currentDir.toString());
    }

    private void changeDirToParent() {
        Path parentPath = currentDir.getParent();
        if(parentPath != null) {
            setCurrentDirectory(parentPath.toString());
        }

    }

    private void setCurrentDirectory(String path) {
        Path tempPath = Paths.get(path);
        currentDir = currentDir.resolve(tempPath);
        invalidateTable();
        clientPath.setText(currentDir.toString());
        treeSet = new TreeSet<>();
    }

    private void invalidateTable() {
        treeSet.clear();
        if(currentDir.getParent() != null) {
            treeSet.add(new FileInfo(new FileNameType(parentDir, FileType.Directory), null, null));
        }
        File[] filesInCurDir = currentDir.toFile().listFiles();
        if(filesInCurDir != null) {
            for (File file : filesInCurDir) {
                FileInfo fileInfo;
                if (Files.isDirectory(file.toPath())) {
                    fileInfo = new FileInfo(new FileNameType(file.getName(), FileType.Directory), null, new Date(file.lastModified()));
                } else {
                    fileInfo = new FileInfo(new FileNameType(file.getName(), FileType.File), file.length(), new Date(file.lastModified()));
                }
                treeSet.add(fileInfo);
            }
        }
        clientTable.getItems().clear();
        clientTable.getItems().addAll(treeSet);
        clientTable.refresh();
    }

    @FXML
    public void exitClickAction(ActionEvent actionEvent) {
        closeConnection();
    }

    public void closeConnection() {
        if(channelFuture != null) {
            channelFuture.channel().close();
        }
        System.exit(0);
    }

    @FXML
    public void connectClickAction(ActionEvent actionEvent) throws IOException {
        ConnectionStage connectionStage = new ConnectionStage();
        connectionStage.showAndWait();
        if(connectionStage.getDialogResult() == ButtonType.OK) {
            host = connectionStage.getIPAddress();
            port = connectionStage.getPort();
            new Thread(() -> {
                try {
                    connect();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void connect() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new StringEncoder(),         // out-1
                                    new StringDecoder(),         // in-1
                                    new InboundMessageHandler()  // in-2
                            );
                        }
                    });
            channelFuture = bootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public void clientDriveChanged(ActionEvent actionEvent) {
        File file = clientDrives.getSelectionModel().getSelectedItem();
        if(file != null) {
            setCurrentDirectory(file.toString());
        }
    }
}
