package costquito.globalMethods;

public final class UserRecord {
    public final String username;
    public final String passwordHash;
    public final UserRole role;
    public final boolean enabled;
    public final String displayName;

    public UserRecord(String username, String passwordHash, UserRole role, boolean enabled, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.displayName = displayName;
    }
}
