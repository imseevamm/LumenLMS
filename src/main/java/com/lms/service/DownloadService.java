package com.lms.service;

import com.lms.dao.DownloadDAO;
import com.lms.model.DownloadRecord;
import com.lms.model.User;
import com.lms.util.SessionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DownloadService {
    private final DownloadDAO downloadDAO = new DownloadDAO();

    public List<DownloadRecord> getHistory() {
        User user = SessionManager.getInstance().requireRole(com.lms.model.Role.ADMIN, com.lms.model.Role.INSTRUCTOR, com.lms.model.Role.STUDENT);
        return downloadDAO.findByUser(user.getId());
    }

    public Path download(Path source, String fileName, String title, String category) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("The file is no longer available on this computer.");
        }
        Path downloadsDir = Path.of(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloadsDir);
        String safeName = sanitize(fileName == null || fileName.isBlank() ? source.getFileName().toString() : fileName);
        Path destination = uniquePath(downloadsDir, safeName);
        Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);

        User user = SessionManager.getInstance().requireRole(com.lms.model.Role.ADMIN, com.lms.model.Role.INSTRUCTOR, com.lms.model.Role.STUDENT);
        DownloadRecord record = new DownloadRecord();
        record.setUserId(user.getId());
        record.setTitle(title == null || title.isBlank() ? safeName : title);
        record.setFileName(destination.getFileName().toString());
        record.setCategory(category == null || category.isBlank() ? "File" : category);
        downloadDAO.insert(record);
        return destination;
    }

    public void clearHistory() {
        User user = SessionManager.getInstance().requireRole(com.lms.model.Role.ADMIN, com.lms.model.Role.INSTRUCTOR, com.lms.model.Role.STUDENT);
        downloadDAO.clearByUser(user.getId());
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\/:*?\"<>|]", "_");
    }

    private static Path uniquePath(Path directory, String fileName) {
        Path target = directory.resolve(fileName);
        if (!Files.exists(target)) return target;
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) { base = fileName.substring(0, dot); ext = fileName.substring(dot); }
        int counter = 1;
        do { target = directory.resolve(base + " (" + counter++ + ")" + ext); } while (Files.exists(target));
        return target;
    }
}
