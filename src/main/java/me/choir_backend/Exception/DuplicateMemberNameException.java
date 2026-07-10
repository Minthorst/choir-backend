package me.choir_backend.Exception;

public class DuplicateMemberNameException extends RuntimeException {
    public DuplicateMemberNameException(String name) {
        super(String.format("A member named \"%s\" already exists", name));
    }
}
