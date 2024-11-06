import java.io.IOException;
import java.nio.file.*;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import static java.nio.file.StandardWatchEventKinds.*;

public class FileSynchronizer extends Thread {
    private String sourceDir;
    private String destDir;
    private WatchService watchService;
    private boolean running = true;
    private JLabel statusLabel;

    public FileSynchronizer(String sourceDir, String destDir, JLabel statusLabel) {
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.statusLabel = statusLabel;
    }

    @Override
    public void run() {
        try {
            // Perform initial synchronization
            copyInitialFiles();

            // Update status label to indicate initial sync is complete
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Monitoring for changes..."));

            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(sourceDir);
            path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            while (running) {
                WatchKey key = watchService.take(); // Wait for a watch key to be available
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path fileName = (Path) event.context();

                    // Perform synchronization based on the event kind
                    if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                        copyFile(fileName);
                    } else if (kind == ENTRY_DELETE) {
                        deleteFile(fileName);
                    }
                }
                key.reset(); // Reset the key to receive further watch events
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void copyInitialFiles() throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        Path destPath = Paths.get(destDir);

        Files.walk(sourcePath).forEach(source -> {
            Path destination = destPath.resolve(sourcePath.relativize(source));
            try {
                if (Files.isDirectory(source)) {
                    if (!Files.exists(destination)) {
                        Files.createDirectory(destination);
                    }
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied initial file: " + source + " to " + destination);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void copyFile(Path fileName) {
        Path sourcePath = Paths.get(sourceDir, fileName.toString());
        Path destPath = Paths.get(destDir, fileName.toString());
        try {
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied: " + sourcePath + " to " + destPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFile(Path fileName) {
        Path destPath = Paths.get(destDir, fileName.toString());
        try {
            Files.deleteIfExists(destPath);
            System.out.println("Deleted: " + destPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopWatching() {
        running = false;
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
