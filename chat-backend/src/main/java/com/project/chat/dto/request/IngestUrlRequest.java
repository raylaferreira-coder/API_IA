package com.project.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class IngestUrlRequest {

    @NotBlank(message = "A URL é obrigatória.")
    @URL(message = "Formato de URL inválido.")
    private String url;

    public IngestUrlRequest() {}

    public IngestUrlRequest(String url) {
        this.url = url;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
