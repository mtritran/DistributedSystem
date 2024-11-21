import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SenderGUI {
    private JFrame frame;
    private JTextField portField;
    private JLabel selectedFileLabel;
    private JTextArea logsArea;
    private File selectedFile;
    private ServerSocket serverSocket;
    private boolean isServerRunning;

    public SenderGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("P2P File Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Port:"), gbc);

        portField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        mainPanel.add(portField, gbc);

        JButton selectFileButton = new JButton("Select File");
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        mainPanel.add(selectFileButton, gbc);

        selectedFileLabel = new JLabel("No file selected");
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        mainPanel.add(selectedFileLabel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton startServerButton = new JButton("Start Server");
        JButton stopServerButton = new JButton("Stop Server");
        stopServerButton.setEnabled(false);

        buttonPanel.add(startServerButton);
        buttonPanel.add(stopServerButton);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        mainPanel.add(buttonPanel, gbc);

        logsArea = new JTextArea(10, 40);
        logsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logsArea);
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane, gbc);

        frame.add(mainPanel);

        selectFileButton.addActionListener(e -> selectFile());

        startServerButton.addActionListener(e -> {
            if (startServer()) {
                startServerButton.setEnabled(false);
                stopServerButton.setEnabled(true);
                portField.setEnabled(false);
            }
        });

        stopServerButton.addActionListener(e -> {
            stopServer();
            startServerButton.setEnabled(true);
            stopServerButton.setEnabled(false);
            portField.setEnabled(true);
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopServer();
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            selectedFileLabel.setText(selectedFile.getName() + " (" + formatFileSize(selectedFile.length()) + ")");
        }
    }

    private boolean startServer() {
        if (selectedFile == null) {
            showError("Please select a file first.");
            return false;
        }

        String portText = portField.getText().trim();
        if (portText.isEmpty()) {
            showError("Please enter a port number.");
            return false;
        }

        try {
            int port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                showError("Please enter a valid port number (1024-65535).");
                return false;
            }

            isServerRunning = true;
            new Thread(this::runServer).start();
            return true;
        } catch (NumberFormatException e) {
            showError("Please enter a valid port number.");
            return false;
        }
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(portField.getText().trim()));
            appendLog("Server started on port " + portField.getText());
            appendLog("Waiting for connection...");

            while (isServerRunning) {
                try (Socket clientSocket = serverSocket.accept()) {
                    appendLog("Client connected from: " + clientSocket.getInetAddress());
                    sendFile(clientSocket);
                } catch (IOException e) {
                    if (isServerRunning) {
                        appendLog("Error with client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (isServerRunning) {
                appendLog("Server error: " + e.getMessage());
                showError("Failed to start server: " + e.getMessage());
            }
        }
    }

    private void sendFile(Socket clientSocket) {
        try {
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            dos.writeUTF(selectedFile.getName());
            dos.writeLong(selectedFile.length());

            try (FileInputStream fis = new FileInputStream(selectedFile);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesSent = 0;
                long fileSize = selectedFile.length();

                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;

                    final int progress = (int) ((totalBytesSent * 100) / fileSize);
                    appendLog("Sending progress: " + progress + "%");
                }

                bos.flush();
                appendLog("File sent successfully!");
            }
        } catch (IOException e) {
            appendLog("Error sending file: " + e.getMessage());
        }
    }

    private void stopServer() {
        isServerRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                appendLog("Server stopped.");
            } catch (IOException e) {
                appendLog("Error stopping server: " + e.getMessage());
            }
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logsArea.append(message + "\n");
            logsArea.setCaretPosition(logsArea.getDocument().getLength());
        });
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    private String formatFileSize(long size) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SenderGUI sender = new SenderGUI();
            sender.show();
        });
    }
}