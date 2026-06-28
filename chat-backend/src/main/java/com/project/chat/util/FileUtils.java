package com.project.chat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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

    public static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private FileUtils() {
    }
}
