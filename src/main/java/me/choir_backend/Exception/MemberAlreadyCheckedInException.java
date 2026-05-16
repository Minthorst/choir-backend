package me.choir_backend.Exception;

import me.choir_backend.model.Session;

public class MemberAlreadyCheckedInException extends RuntimeException {
    public MemberAlreadyCheckedInException(Session session) {
        super(String.format("Member is already checked in to current Session %s opened: %s", session.getId(), session.getStartTime()));
    }
}
