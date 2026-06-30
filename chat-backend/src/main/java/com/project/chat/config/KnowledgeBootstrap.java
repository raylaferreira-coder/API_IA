package com.project.chat.config;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.DocumentIngestionService;
import com.project.chat.service.KnowledgeScannerService;

@Component
public class KnowledgeBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBootstrap.class);

    private final KnowledgeScannerService scannerService;
    private final DocumentIngestionService ingestionService;
    private final DocumentRepository documentRepository;

    public KnowledgeBootstrap(
            KnowledgeScannerService scannerService,
            DocumentIngestionService ingestionService,
            DocumentRepository documentRepository) {

        this.scannerService = scannerService;
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    @Override
    public void run(String... args) {

        log.info("======================================");
        log.info("Iniciando carga automática da Knowledge Base...");
        log.info("======================================");

        try {

            List<Path> arquivos = scannerService.scan();

            if (arquivos.isEmpty()) {
                log.info("Nenhum documento encontrado.");
                return;
            }

            for (Path arquivo : arquivos) {

                String caminho = arquivo.toString();

                if (documentRepository.existsBySourcePath(caminho)) {

                    log.info("Documento já indexado: {}", arquivo.getFileName());
                    continue;

                }

                String nomeArquivo = arquivo.getFileName().toString();

                String tipo = detectarTipo(nomeArquivo);

                log.info("Indexando {}", nomeArquivo);

                ingestionService.ingestFromFile(
                        caminho,
                        tipo,
                        nomeArquivo);

            }

            log.info("======================================");
            log.info("Knowledge Base carregada.");
            log.info("======================================");

        } catch (Exception e) {

            log.error("Erro durante a carga automática da Knowledge Base", e);

        }

    }

    private String detectarTipo(String nome) {

        nome = nome.toLowerCase();

        if (nome.endsWith(".pdf"))
            return "pdf";

        if (nome.endsWith(".txt"))
            return "txt";

        if (nome.endsWith(".md"))
            return "markdown";

        if (nome.endsWith(".html"))
            return "html";

        if (nome.endsWith(".docx"))
            return "docx";

        if (nome.endsWith(".jpg"))
            return "jpg";

        if (nome.endsWith(".jpeg"))
            return "jpg";

        if (nome.endsWith(".png"))
            return "png";

        return "txt";
    }

}