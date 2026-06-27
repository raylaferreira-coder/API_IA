package com.project.chat.ai;

import com.project.chat.ai.dto.GenerateResponse;

public interface AiAgent {

    String ask(String question);

    String askWithContext(String question, String context);

    GenerateResponse askDetailed(String question);

    GenerateResponse askDetailedWithContext(String question, String context);

    boolean isAvailable();
}
