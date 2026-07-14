package me.choir_backend.service;

import jakarta.transaction.Transactional;
import me.choir_backend.Boundary.GetSessionResponse;
import me.choir_backend.Exception.ResourceNotFoundException;
import me.choir_backend.model.Session;
import me.choir_backend.model.SessionType;
import me.choir_backend.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public List<Session> closeOpenAndGetNotYetFinalizedSessions() {
        closeStaleSessions();
        return sessionRepository.findAllBySessionType(SessionType.AUTO_CLOSE);
    }


    public Session getActiveSession() {
        Optional<Session> activeSessionOptional = sessionRepository.findFirstByIsOpen(true);
        if (activeSessionOptional.isEmpty() || activeSessionOptional.get().isExpired()) {
            return null;
        } else return activeSessionOptional.get();
    }

    public Session getActiveSessionForCheckIn() {
        Optional<Session> activeSessionOptional = sessionRepository.findFirstByIsOpenWithLock();
        return activeSessionOptional.orElseGet(this::createAndSaveNewSession);
    }


    public void closeStaleSessions() {
        List<Session> activeSessions = sessionRepository.findAllByIsOpen(true);
        for (Session session : activeSessions) {
            if (session.isExpired()) {
                session.setOpen(false);
                session.setSessionType(SessionType.AUTO_CLOSE);
                sessionRepository.save(session);
                log.warn("Auto-closed stale session {} (started {})", session.getId(), session.getStartTime());
            }
        }
    }

    public Session createAndSaveNewSession() {
        Session session = sessionRepository.save(new Session());
        log.info("Opened new session {}", session.getId());
        return session;
    }

    public void saveSession(Session session) {
        sessionRepository.save(session);
    }

    public Session findMandatorySession(Long id) {
        return sessionRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(String.format("Invalid Session ID, %s ; Session not found.", id)));
    }

    public List<GetSessionResponse> getAllSessions() {
        return sessionRepository.findAllSessionsWithAttendeeCount();
    }
}
