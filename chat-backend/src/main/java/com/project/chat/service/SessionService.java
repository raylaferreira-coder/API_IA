package com.project.chat.service;

import com.project.chat.dto.response.SessionResponse;
import com.project.chat.entity.Session;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final int expirationHours;

    public SessionService(SessionRepository sessionRepository,
                          @Value("${session.expiration-hours:24}") int expirationHours) {
        this.sessionRepository = sessionRepository;
        this.expirationHours = expirationHours;
    }

    @Transactional
    public SessionResponse createSession() {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId);
        session = sessionRepository.save(session);
        log.info("Nova sessão criada: id={}, sessionId={}", session.getId(), session.getSessionId());
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + sessionId));
        return toResponse(session);
    }

    @Transactional
    public void invalidateSession(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + sessionId));
        session.setExpired(true);
        sessionRepository.save(session);
        log.info("Sessão invalidada: sessionId={}", sessionId);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireOldSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(expirationHours);
        var oldSessions = sessionRepository.findByExpiredFalseAndLastActivityBefore(threshold);
        for (Session s : oldSessions) {
            s.setExpired(true);
            sessionRepository.save(s);
        }
        if (!oldSessions.isEmpty()) {
            log.info("{} sessões expiradas por inatividade.", oldSessions.size());
        }
    }

    private SessionResponse toResponse(Session session) {
        return new SessionResponse(
                session.getSessionId(),
                session.getCreatedAt(),
                session.getLastActivity(),
                session.isExpired()
        );
    }
}
