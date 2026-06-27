package com.project.chat.controller;

import com.project.chat.dto.response.ErrorResponse;
import com.project.chat.dto.response.SessionResponse;
import com.project.chat.service.SessionService;
import com.project.chat.util.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<SessionResponse> createOrGetSession() {
        SessionResponse response = sessionService.createOrGetSession();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> invalidateSession(@PathVariable String sessionId, HttpServletRequest request) {
        if (!FileUtils.isValidUuid(sessionId)) {
            ErrorResponse error = new ErrorResponse(
                    400, "Bad Request",
                    "O identificador de sessão fornecido é inválido.",
                    request.getRequestURI()
            );
            return ResponseEntity.badRequest().body(error);
        }
        sessionService.invalidateSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
