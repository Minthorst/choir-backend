package me.choir_backend.config;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 15;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, FailureWindow> failuresByIp = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        FailureWindow window = failuresByIp.get(ip);
        if (window == null) return false;
        if (window.isExpired()) {
            failuresByIp.remove(ip);
            return false;
        }
        return window.count >= MAX_FAILURES;
    }

    public void loginFailed(String ip) {
        failuresByIp.compute(ip, (key, window) ->
                (window == null || window.isExpired()) ? new FailureWindow() : window.increment());
    }

    public void loginSucceeded(String ip) {
        failuresByIp.remove(ip);
    }

    private static class FailureWindow {
        private final Instant start = Instant.now();
        private int count = 1;

        private FailureWindow increment() {
            count++;
            return this;
        }

        private boolean isExpired() {
            return start.plus(WINDOW).isBefore(Instant.now());
        }
    }
}
