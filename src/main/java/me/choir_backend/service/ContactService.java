package me.choir_backend.service;

import me.choir_backend.Boundary.ContactRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String adminFeedbackEmail;

    public ContactService(JavaMailSender mailSender,
                          @Value("${spring.mail.username}") String senderEmail,
                          @Value("${app.admin-feedback-email}") String adminFeedbackEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.adminFeedbackEmail = adminFeedbackEmail;
    }

    public void sendFeedback(ContactRequest request, Authentication authentication) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(adminFeedbackEmail);
        message.setReplyTo(request.email());
        message.setSubject("Nofomo Feedback von " + request.name());
        message.setText(buildBody(request, authentication));
        mailSender.send(message);
    }

    private String buildBody(ContactRequest request, Authentication authentication) {
        String loggedInAs = authentication == null ? "unbekannt" :
                authentication.getName() + " (" +
                        authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(", "))
                        + ")";

        return "Name: " + request.name() + "\n" +
                "E-Mail: " + request.email() + "\n\n" +
                "Nachricht:\n" + request.message() + "\n\n" +
                "---\n" +
                "Debug-Informationen:\n" +
                "Server-Zeit: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n" +
                "Client-Zeit: " + valueOrUnknown(request.clientTimestamp()) + "\n" +
                "Seite: " + valueOrUnknown(request.page()) + "\n" +
                "Member-Key: " + valueOrUnknown(request.memberKey()) + "\n" +
                "Angemeldet als: " + loggedInAs + "\n" +
                "Viewport: " + valueOrUnknown(request.viewport()) + "\n" +
                "User-Agent: " + valueOrUnknown(request.userAgent());
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
