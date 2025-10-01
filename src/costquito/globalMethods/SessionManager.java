package costquito.globalMethods;

import java.util.Objects;

public final class SessionManager {

    private static volatile UserSession current = null;

    private SessionManager() {}

    public static synchronized boolean login(String username, String password) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");

        // TODO: Reemplazar por lectura de tu JSON (usuarios, hashes, roles).
        // DEMO mínima:
        UserRole role = authenticate(username, password);
        if (role == null) {
            LogUtils.warn("login_failed", "username", username);
            return false;
        }

        current = new UserSession(username, role);
        LogUtils.setCurrentUser(username);
        LogUtils.audit("login_success", "sessionId", current.getSessionId(), "role", role.name());
        return true;
    }

    public static synchronized void logout() {
        if (current != null) {
            LogUtils.audit("logout", "sessionId", current.getSessionId(), "username", current.getUsername());
        }
        current = null;
        LogUtils.setCurrentUser(null);
    }

    public static UserSession getCurrent() {
        return current;
    }

    public static boolean isLoggedIn() {
        return current != null;
    }

    public static boolean isAdmin() {
        return current != null && current.getRole() == UserRole.ADMIN;
    }

    // ---------- Autenticación demo ----------
    private static UserRole authenticate(String username, String password) {
        if ("admin".equalsIgnoreCase(username) && "admin".equals(password)) {
            return UserRole.ADMIN;
        }
        if ("vendedor".equalsIgnoreCase(username) && "1234".equals(password)) {
            return UserRole.VENDEDOR;
        }
        return null;
    }
}
