package com.project.chat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer PostgreSQL + pgvector (VECTOR type incompatível com H2)")
class ChatApplicationTests {

    @Test
    void contextLoads() {
    }
}
