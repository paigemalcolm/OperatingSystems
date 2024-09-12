import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class DirectorySynchronizer {

    public static void syncDirectories(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            System.out.println("Source directory does not exist or is not a directory.");
            return;
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // List all files in source directory
        Set<Path> sourceFiles = new HashSet<>();
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                sourceFiles.add(sourceDir.relativize(file));
                return FileVisitResult.CONTINUE;
            }
        });

        // List all files in target directory
        Set<Path> targetFiles = new HashSet<>();
        Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                targetFiles.add(targetDir.relativize(file));
                return FileVisitResult.CONTINUE;
            }
        });

        // Sync files from source to target (add or update)
        for (Path relativeSourceFile : sourceFiles) {
            Path sourceFile = sourceDir.resolve(relativeSourceFile);
            Path targetFile = targetDir.resolve(relativeSourceFile);

            if (!Files.exists(targetFile) || !Files.isSameFile(sourceFile, targetFile)) {
                if (Files.isDirectory(sourceFile)) {
                    Files.createDirectories(targetFile);
                } else {
                    System.out.println("Copying or updating file: " + sourceFile);
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        // Delete files from target that no longer exist in source
        for (Path relativeTargetFile : targetFiles) {
            Path targetFile = targetDir.resolve(relativeTargetFile);
            Path sourceFile = sourceDir.resolve(relativeTargetFile);

            if (!Files.exists(sourceFile)) {
                if (Files.isDirectory(targetFile)) {
                    System.out.println("Deleting directory: " + targetFile);
                    Files.walkFileTree(targetFile, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    System.out.println("Deleting file: " + targetFile);
                    Files.delete(targetFile);
                }
            }
        }
    }

    public static void main(String[] args) {
        Path sourceDir = Paths.get("/path/to/source");
        Path targetDir = Paths.get("/path/to/target");

        try {
            syncDirectories(sourceDir, targetDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
