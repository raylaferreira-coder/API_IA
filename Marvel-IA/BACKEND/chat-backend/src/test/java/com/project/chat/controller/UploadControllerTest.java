package com.project.chat.controller;

import com.project.chat.dto.response.UploadResponse;
import com.project.chat.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadService uploadService;

    @Test
    void uploadFile_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "teste.txt", "text/plain", "conteudo".getBytes());
        UploadResponse response = new UploadResponse(
                1L, "teste.txt", "text/plain", 8L, LocalDateTime.now(), "Arquivo enviado com sucesso.");

        when(uploadService.uploadFile(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .param("sessionId", "session-1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentId").value(1));
    }
}
