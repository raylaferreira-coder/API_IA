package com.project.chat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class KnowledgeScannerService {

    private static final String DOCKER_PATH = "/app/knowledge";
    private static final String LOCAL_PATH = "../knowledge";

    public List<Path> scan() throws IOException {

        Path folder = Path.of(DOCKER_PATH);

        if (!Files.exists(folder)) {
            folder = Path.of(LOCAL_PATH);
        }

        if (!Files.exists(folder)) {
            throw new IOException(
                    "Pasta knowledge não encontrada.\n"
                            + "Docker: " + DOCKER_PATH + "\n"
                            + "Local : " + LOCAL_PATH);
        }

        return Files.walk(folder)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
    }
}