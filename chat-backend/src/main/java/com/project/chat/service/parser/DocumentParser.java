package com.project.chat.service.parser;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentParser {
    String parse(InputStream inputStream) throws IOException;
    boolean supports(String sourceType);
}
