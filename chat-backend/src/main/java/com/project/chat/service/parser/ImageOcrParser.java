package com.project.chat.service.parser;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class ImageOcrParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(ImageOcrParser.class);

    private static final Set<String> IMAGE_TYPES = Set.of(
            "jpg", "jpeg", "png", "bmp", "tiff", "tif", "gif");

    @Override
    public String parse(InputStream inputStream) throws IOException {
        log.info("Executando OCR em imagem");
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                log.warn("Nao foi possivel ler a imagem para OCR");
                return "";
            }
            ITesseract tesseract = new Tesseract();
            String result = tesseract.doOCR(image);
            log.info("OCR concluido: {} caracteres extraidos", result.length());
            return result.trim();
        } catch (TesseractException e) {
            log.warn("OCR falhou (Tesseract pode nao estar instalado): {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.warn("Erro inesperado no OCR: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public boolean supports(String sourceType) {
        return IMAGE_TYPES.contains(sourceType.toLowerCase());
    }
}
