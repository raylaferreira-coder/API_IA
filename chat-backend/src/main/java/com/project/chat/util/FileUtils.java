package com.project.chat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("text/plain", "application/pdf");
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType);
    }

    public static boolean isWithinSizeLimit(long size) {
        return size <= MAX_FILE_SIZE;
    }

    public static boolean isValidUuid(String value) {
        if (value == null) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidContent(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) return false;

            String mimeType = file.getContentType();
            if ("application/pdf".equals(mimeType)) {
                return bytes.length >= 4
                        && bytes[0] == '%'
                        && bytes[1] == 'P'
                        && bytes[2] == 'D'
                        && bytes[3] == 'F';
            }
            return true;
        } catch (IOException e) {
            log.warn("Erro ao ler arquivo para validação de integridade.");
            return false;
        }
    }

    private FileUtils() {
    }
}
