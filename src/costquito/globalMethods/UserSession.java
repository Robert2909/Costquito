package costquito.globalMethods;

import java.time.Instant;
import java.util.UUID;

public final class UserSession {
    private final String username;
    private final UserRole role;
    private final Instant startedAt;
    private final String sessionId;

    public UserSession(String username, UserRole role) {
        this.username = username;
        this.role = role;
        this.startedAt = Instant.now();
        this.sessionId = UUID.randomUUID().toString();
    }

    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public Instant getStartedAt() { return startedAt; }
    public String getSessionId() { return sessionId; }
}
