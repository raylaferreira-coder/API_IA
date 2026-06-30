package com.project.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${chat.storage.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de upload: " + this.uploadDir, e);
        }
    }

    public Path store(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            String rawExt = originalName.substring(originalName.lastIndexOf("."));
            extension = rawExt.replaceAll("[^a-zA-Z0-9.]", "");
        }
        if (extension.length() > 10) {
            extension = "";
        }
        String storedName = UUID.randomUUID() + extension;
        Path targetPath = uploadDir.resolve(storedName).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw new IOException("Path traversal detectado: " + originalName);
        }
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    public void delete(Path path) throws IOException {
        Files.deleteIfExists(path);
    }
}
