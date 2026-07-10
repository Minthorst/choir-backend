package me.choir_backend.service;

import me.choir_backend.model.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionCleanupJob {

    private final SessionService sessionService;
    private final JavaMailSender mailSender;

    private final String frontendUrl;
    private final String senderEmail;
    private final String chorleiterEmail;

    public SessionCleanupJob(SessionService sessionService, JavaMailSender mailSender,
                             @Value("${app.frontend-url}") String frontendUrl,
                             @Value("${spring.mail.username}") String senderEmail,
                             @Value("${app.chorleiter-email}") String chorleiterEmail) {
        this.sessionService = sessionService;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.senderEmail = senderEmail;
        this.chorleiterEmail = chorleiterEmail;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupForgottenSessions() {
        List<Session> forgottenSessions = sessionService.closeOpenAndGetNotYetFinalizedSessions();
        if (forgottenSessions != null && !forgottenSessions.isEmpty())
            sendNotificationEmail(forgottenSessions.size());
    }

    private void sendNotificationEmail(int amountOfNotFinalSessions) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(chorleiterEmail);
        message.setSubject("Offene Chorprobe automatisch geschlossen – Finalisierung erforderlich");

        String text = "Moin Niklas,\n\n" +
                amountOfNotFinalSessions + " Probe/n wurden noch nicht händisch beendet.\n" +
                "Das System hat die Sessions automatisch geschlossen (AUTO_CLOSE).\n\n" +
                "Bitte klicke auf den folgende Link und teile den korrekten Status zu, um die Tickets für die anwesenden/abwesenden " +
                "Mitglieder final abzurechnen (COMMIT oder REGULAR):\n\n" +
                frontendUrl + "/admin\n\n" +
                "Viele Grüße,\nJan";

        message.setText(text);
        mailSender.send(message);
    }
}

