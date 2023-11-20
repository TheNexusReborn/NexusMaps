package com.thenexusreborn.gamemaps;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class FileHelper {
    public static void createFileIfNotExists(Path path) {
        createDirectoryIfNotExists(path.getParent());
        
        if (Files.notExists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static Path subPath(Path parent, String... child) {
        return FileSystems.getDefault().getPath(parent.toString(), child);
    }
    
    public static void createDirectoryIfNotExists(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static Path downloadFile(String downloadUrl, Path downloadDir, String fileName, boolean userAgent) {
        try {
            Path targetFile = FileSystems.getDefault().getPath(downloadDir.toString(), fileName);
            if (downloadUrl.startsWith("file://")) {
                Files.copy(Path.of(downloadUrl.replace("file://", "")), targetFile, REPLACE_EXISTING);
            } else {
                URL url = new URL(downloadUrl);
                Path tmpFile = FileSystems.getDefault().getPath(downloadDir.toString(), fileName + ".tmp");
                if (Files.exists(tmpFile)) {
                    Files.delete(tmpFile);
                }
                Files.createFile(tmpFile);
                URLConnection connection = url.openConnection();
                if (userAgent) {
                    connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
                }
                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream()); FileOutputStream out = new FileOutputStream(tmpFile.toFile())) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer, 0, 1024)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                Files.move(tmpFile, targetFile, REPLACE_EXISTING);
            }
            return targetFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void copyFolder(Path src, Path dest) {
        try {
            Files.walkFileTree(src, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    Files.createDirectories(dest.resolve(src.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.copy(file, dest.resolve(src.relativize(file)), REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    public static void deleteDirectory(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            String osName = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            if (osName.contains("windows")) {
                cmd = new String[]{"powershell.exe", "Remove-Item", "-Path", "'" + directory.toAbsolutePath() + "'", "-r", "-fo"};
            } else if (osName.contains("ubuntu") || osName.contains("linux")) {
                cmd = new String[]{"rm", "-rf", directory.toAbsolutePath().toString()};
            } else {
                cmd = null;
            }
    
            try {
                Process process = new ProcessBuilder().command(cmd).start();
                process.waitFor();
                process.getOutputStream().close();
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}