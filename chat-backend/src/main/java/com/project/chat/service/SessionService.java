package com.project.chat.service;

import com.project.chat.dto.response.SessionResponse;
import com.project.chat.entity.Session;
import com.project.chat.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SessionResponse createOrGetSession() {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId);
        session = sessionRepository.save(session);
        return toResponse(session);
    }

    public SessionResponse getSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.project.chat.exception.ResourceNotFoundException(
                        "Sessão não encontrada: " + sessionId));
        return toResponse(session);
    }

    public void invalidateSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.project.chat.exception.ResourceNotFoundException(
                        "Sessão não encontrada: " + sessionId));
        session.setExpired(true);
        sessionRepository.save(session);
    }

    public void expireOldSessions(int expirationHours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(expirationHours);
        var oldSessions = sessionRepository.findByExpiredFalseAndLastActivityBefore(threshold);
        for (Session s : oldSessions) {
            s.setExpired(true);
            sessionRepository.save(s);
        }
    }

    private SessionResponse toResponse(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getCreatedAt(),
                session.getLastActivity(),
                session.isExpired()
        );
    }
}
