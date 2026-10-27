package rszabadi.swiss;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptLimiter.class);

    private record Attempt(int failures, Instant windowStart, Instant blockedUntil) {
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String client) {
        Attempt attempt = attempts.get(client);
        return attempt != null
                && attempt.blockedUntil() != null
                && Instant.now().isBefore(attempt.blockedUntil());
    }

    public void recordFailure(String client) {
        Instant now = Instant.now();
        attempts.values().removeIf(a -> isStale(a, now));   // forget old entries
        Attempt updated = attempts.compute(client, (key, old) -> {
            boolean fresh = old == null || isStale(old, now);
            int failures = fresh ? 1 : old.failures() + 1;
            Instant start = fresh ? now : old.windowStart();
            Instant blockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK) : null;
            return new Attempt(failures, start, blockedUntil);
        });
        if (updated.blockedUntil() != null) {
            log.warn("Too many failed logins from {}: login locked for {} minutes", client, LOCK.toMinutes());
        }
    }

    public void recordSuccess(String client) {
        attempts.remove(client);
    }

    private static boolean isStale(Attempt attempt, Instant now) {
        return attempt.blockedUntil() == null
                ? attempt.windowStart().plus(WINDOW).isBefore(now)
                : attempt.blockedUntil().isBefore(now);
    }
}