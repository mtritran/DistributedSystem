package com.example.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class SenderGUI extends Application {
    private TextField portField;
    private TextField filePathField;
    private TextArea logArea;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Sender");

        // Tạo layout chính
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Tạo GridPane cho các controls
        GridPane controlsGrid = new GridPane();
        controlsGrid.setHgap(10);
        controlsGrid.setVgap(10);
        controlsGrid.setPadding(new Insets(0, 0, 10, 0));

        // Dòng Port
        Label portLabel = new Label("Port:");
        portField = new TextField();
        controlsGrid.add(portLabel, 0, 0);
        controlsGrid.add(portField, 1, 0);

        // Dòng Select File
        Label fileLabel = new Label("Select File:");
        filePathField = new TextField();
        filePathField.setEditable(false);
        Button browseButton = new Button("Browse");
        controlsGrid.add(fileLabel, 0, 1);
        controlsGrid.add(filePathField, 1, 1);
        controlsGrid.add(browseButton, 2, 1);

        // Nút Send
        Button sendButton = new Button("Send");
        sendButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setColumnSpan(sendButton, 2);
        controlsGrid.add(sendButton, 1, 2);

        // Khu vực log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);

        // Thêm các thành phần vào layout chính
        mainLayout.getChildren().addAll(controlsGrid, logArea);

        // Xử lý sự kiện nút Browse
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        // Xử lý sự kiện nút Send
        sendButton.setOnAction(e -> {
            String port = portField.getText().trim();
            String filePath = filePathField.getText().trim();

            if (port.isEmpty() || filePath.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Please enter a valid port and select a file.");
                alert.showAndWait();
                return;
            }

            logArea.appendText("Sending file: " + filePath + " on port " + port + "...\n");
            // Thêm logic xử lý gửi file
        });

        // Tạo scene và hiển thị
        Scene scene = new Scene(mainLayout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}