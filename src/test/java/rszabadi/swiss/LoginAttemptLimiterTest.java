package rszabadi.swiss;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter();

    @Test
    void clientIsNotBlockedAtFirst() {
        assertThat(limiter.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void fewFailuresDoNotBlock() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("1.2.3.4");
        }
        assertThat(limiter.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void tooManyFailuresBlockOnlyThatClient() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure("1.2.3.4");
        }
        assertThat(limiter.isBlocked("1.2.3.4")).isTrue();
        assertThat(limiter.isBlocked("5.6.7.8")).isFalse();
    }

    @Test
    void successfulLoginClearsTheFailures() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("1.2.3.4");
        }
        limiter.recordSuccess("1.2.3.4");
        limiter.recordFailure("1.2.3.4");
        assertThat(limiter.isBlocked("1.2.3.4")).isFalse();
    }
}