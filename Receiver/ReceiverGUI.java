import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ReceiverGUI {
    private JFrame frame;
    private JTextField senderIpField;
    private JTextField sharedPortField;
    private JTextField finalPathField;
    private JTextArea logsArea;

    public ReceiverGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("P2P File Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Sender's IP:"), gbc);

        senderIpField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        mainPanel.add(senderIpField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Shared Port:"), gbc);

        sharedPortField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        mainPanel.add(sharedPortField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Save to:"), gbc);

        finalPathField = new JTextField("No directory selected");
        finalPathField.setEditable(false);
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.weightx = 1.0;
        mainPanel.add(finalPathField, gbc);

        JButton browseButton = new JButton("Browse");
        gbc.gridx = 2; gbc.gridy = 2;
        gbc.weightx = 0.0;
        mainPanel.add(browseButton, gbc);

        JButton downloadButton = new JButton("Download");
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(downloadButton, gbc);

        logsArea = new JTextArea(10, 40);
        logsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logsArea);
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane, gbc);

        frame.add(mainPanel);

        browseButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (folderChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                finalPathField.setText(folderChooser.getSelectedFile().getAbsolutePath());
            }
        });

        downloadButton.addActionListener(e -> startDownload());

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void startDownload() {
        String senderIp = senderIpField.getText().trim();
        String sharedPort = sharedPortField.getText().trim();
        String savePath = finalPathField.getText();

        if (senderIp.isEmpty() || sharedPort.isEmpty() || savePath.equals("No directory selected")) {
            JOptionPane.showMessageDialog(frame,
                    "Please fill in all fields and select a save location.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                int port = Integer.parseInt(sharedPort);
                appendLog("Connecting to " + senderIp + ":" + port + "...");

                try (Socket socket = new Socket(senderIp, port)) {
                    appendLog("Connected to sender.");

                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    appendLog("Receiving file: " + fileName);
                    appendLog("File size: " + formatFileSize(fileSize));

                    File saveDir = new File(savePath);
                    if (!saveDir.exists()) {
                        saveDir.mkdirs();
                    }

                    File receivedFile = new File(saveDir, fileName);
                    try (FileOutputStream fos = new FileOutputStream(receivedFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytesRead = 0;

                        while ((bytesRead = dis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            final int progress = (int) ((totalBytesRead * 100) / fileSize);
                            appendLog("Progress: " + progress + "%");
                        }

                        appendLog("File received successfully: " + receivedFile.getAbsolutePath());
                    }
                }
            } catch (NumberFormatException ex) {
                appendLog("Error: Invalid port number");
                JOptionPane.showMessageDialog(frame,
                        "Please enter a valid port number",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                appendLog("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logsArea.append(message + "\n");
            logsArea.setCaretPosition(logsArea.getDocument().getLength());
        });
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
            ReceiverGUI receiver = new ReceiverGUI();
            receiver.show();
        });
    }
}