package me.choir_backend.service;

import jakarta.transaction.Transactional;
import me.choir_backend.Boundary.GetSessionResponse;
import me.choir_backend.Exception.ResourceNotFoundException;
import me.choir_backend.model.Session;
import me.choir_backend.model.SessionType;
import me.choir_backend.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public List<Session> closeOpenAndGetNotYetFinalizedSessions() {
        List<Session> sessions = sessionRepository.getForgottenAndUnfinalizedSessions();
        sessions.forEach(this::closeAndSaveOpenSession);
        return sessions;
    }

    private void closeAndSaveOpenSession(Session session) {
        if(session.isExpired()){
            session.setOpen(false);
            session.setSessionType(SessionType.AUTO_CLOSE);
            saveSession(session);
        }
    }

    public Session getOrCreateActiveSession() {
        Optional<Session> activeSessionOptional = sessionRepository.findFirstByIsOpenWithLock();
        if (activeSessionOptional.isEmpty()) {
            return createAndSaveNewSession();
        } else if (activeSessionOptional.get().isExpired()) {
            Session expiredSession = activeSessionOptional.get();
            expiredSession.setOpen(false);
            expiredSession.setSessionType(SessionType.AUTO_CLOSE);
            sessionRepository.save(expiredSession);
            return createAndSaveNewSession();
        } else return activeSessionOptional.get();
    }

    private Session createAndSaveNewSession() {
        return sessionRepository.save(new Session());
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
