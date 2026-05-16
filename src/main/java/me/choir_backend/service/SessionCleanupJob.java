package me.choir_backend.service;

import me.choir_backend.model.Session;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SessionCleanupJob {

    private final SessionService sessionService;
    private final JavaMailSender mailSender;

    private final String frontendUrl = "https://google.com";

    final String senderEmail = System.getenv("MAIL_USERNAME");
    private final String chorleiterEmail = System.getenv("EMAIL_CHORLEITER");

    public SessionCleanupJob(SessionService sessionService, JavaMailSender mailSender) {
        this.sessionService = sessionService;
        this.mailSender = mailSender;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupForgottenSessions() {
        List<Session> forgottenSessions = sessionService.closeOpenAndGetNotYetFinalizedSessions();
        if (forgottenSessions != null && !forgottenSessions.isEmpty())
            sendNotificationEmail(forgottenSessions);
    }

    private void sendNotificationEmail(List<Session> sessions) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(chorleiterEmail);
        message.setSubject("Offene Chorprobe automatisch geschlossen – Finalisierung erforderlich");

        List<String> finalizeLinks = new ArrayList<>();
        //TODO check frontend link when done or add two buttons somehow to do it directly
        sessions.forEach(session -> finalizeLinks.add(String.format("ProbeId: %d - StartZeit: %s - %s/admin/sessions/%d/finalize \n", session.getId(), session.getStartTime(), frontendUrl, session.getId())));
        String formattedLinks = String.join("", finalizeLinks);

        String text = String.format(
                "Hallo Niklas,\n\n" +
                        "die folgenden Proben wurden noch nicht händisch beendet.\n" +
                        "Das System hat die Sessions automatisch geschlossen (AUTO_CLOSE).\n\n" +
                        "Bitte klicke auf den folgende Links, um die Tickets für die anwesenden/abwesenden " +
                        "Mitglieder final abzurechnen (COMMIT oder REGULAR):\n\n" +
                        "%s\n\n" +
                        "Viele Grüße,\nJan",
                formattedLinks
        );

        message.setText(text);
        mailSender.send(message);
    }
}

