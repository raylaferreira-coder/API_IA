package com.project.chat.controller;

import com.project.chat.dto.response.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.net.http.HttpClient;

import static org.mockito.Mockito.when;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Connection;

@WebMvcTest(HealthController.class)
@TestPropertySource(properties = "rag.ollama.url=http://localhost:1")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private HttpClient httpClient;

    @Test
    void health_WhenDatabaseIsUp_ShouldReturn200() throws Exception {
        Connection connection = mockConnection();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.ollama").value("DOWN"));
    }

    private Connection mockConnection() {
        return org.mockito.Mockito.mock(Connection.class);
    }
}
