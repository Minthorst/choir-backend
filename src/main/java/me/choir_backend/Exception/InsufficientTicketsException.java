package me.choir_backend.Exception;

public class InsufficientTicketsException extends RuntimeException {
    public InsufficientTicketsException() {
        super("Member does not have enough Tickets");
    }
}
