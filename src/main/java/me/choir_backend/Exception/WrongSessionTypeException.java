package me.choir_backend.Exception;

public class WrongSessionTypeException extends RuntimeException {
    public WrongSessionTypeException(String message) {
        super(message);
    }
}
