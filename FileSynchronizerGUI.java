import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.*;

public class FileSynchronizerGUI extends JFrame implements ActionListener {
    private JTextField sourceField;
    private JTextField destinationField;
    private JButton sourceButton;
    private JButton destinationButton;
    private JButton startButton;
    private JLabel statusLabel;
    private JTextArea messageArea;
    private JScrollPane scrollPane;

    public FileSynchronizerGUI() {
        setTitle("Java File Synchronizer");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel for directory selection and status
        JPanel topPanel = new JPanel(new GridLayout(3, 3));

        sourceField = new JTextField();
        destinationField = new JTextField();

        sourceButton = new JButton("Select Source");
        destinationButton = new JButton("Select Destination");
        startButton = new JButton("Start Sync");

        sourceButton.addActionListener(this);
        destinationButton.addActionListener(this);
        startButton.addActionListener(this);

        statusLabel = new JLabel("Status: Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Add components to the top panel
        topPanel.add(new JLabel("Source Directory:"));
        topPanel.add(sourceField);
        topPanel.add(sourceButton);

        topPanel.add(new JLabel("Destination Directory:"));
        topPanel.add(destinationField);
        topPanel.add(destinationButton);

        topPanel.add(startButton);
        topPanel.add(statusLabel);

        // Text area for displaying messages
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        scrollPane = new JScrollPane(messageArea);

        // Add panels to the frame
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sourceButton) {
            selectDirectory(sourceField);
        } else if (e.getSource() == destinationButton) {
            selectDirectory(destinationField);
        } else if (e.getSource() == startButton) {
            startSynchronization();
        }
    }

    private void selectDirectory(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startSynchronization() {
        String sourceDir = sourceField.getText();
        String destDir = destinationField.getText();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select both directories.");
            return;
        }
        startButton.setEnabled(false);
        statusLabel.setText("Status: Synchronizing...");
        messageArea.setText(""); // Clear previous messages

        FileSynchronizer synchronizer = new FileSynchronizer(sourceDir, destDir);
        synchronizer.execute();
    }

    // Inner class for synchronization logic using SwingWorker
    class FileSynchronizer extends SwingWorker<Void, String> {
        private String sourceDir;
        private String destDir;

        public FileSynchronizer(String sourceDir, String destDir) {
            this.sourceDir = sourceDir;
            this.destDir = destDir;
        }

        @Override
        protected Void doInBackground() throws Exception {
            copyInitialFiles();
            return null;
        }

        private void copyInitialFiles() {
            Path sourcePath = Paths.get(sourceDir);
            Path destPath = Paths.get(destDir);

            try {
                Files.walk(sourcePath).forEach(source -> {
                    Path destination = destPath.resolve(sourcePath.relativize(source));
                    try {
                        if (Files.isDirectory(source)) {
                            if (!Files.exists(destination)) {
                                Files.createDirectory(destination);
                                publish("Created directory: " + destination.toString());
                            }
                        } else {
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                            publish("Copied file: " + source.toString() + " to " + destination.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        publish("Error copying: " + source.toString());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                publish("Error during file synchronization.");
            }
        }

        @Override
        protected void process(java.util.List<String> chunks) {
            for (String message : chunks) {
                messageArea.append(message + "\n");
            }
        }

        @Override
        protected void done() {
            statusLabel.setText("Status: Synchronization complete.");
            startButton.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileSynchronizerGUI gui = new FileSynchronizerGUI();
            gui.setVisible(true);
        });
    }
}