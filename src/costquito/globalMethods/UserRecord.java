package costquito.globalMethods;

/**
 * Modelo de usuario alineado con SessionManager:
 * - passwordHash: se guarda el hash (NO texto claro).
 * - enabled: habilitado para iniciar sesión.
 * - role: público para compatibilidad con accesos directos (SessionManager).
 *
 * Si prefieres encapsular, mantén también getters/setters, pero dejamos los
 * campos públicos para evitar cambios en clases existentes.
 */
public class UserRecord {

    public String username;
    public String passwordHash;
    public boolean enabled = true;
    public UserRole role;
    public String displayName;

    public UserRecord() { }

    public UserRecord(UserRole role, String username, String passwordHash, boolean enabled) {
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    public static UserRecord of(UserRole role, String username, String passwordHash, boolean enabled) {
        return new UserRecord(role, username, passwordHash, enabled);
    }

    // Getters/Setters por si en alguna parte se usan métodos
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
