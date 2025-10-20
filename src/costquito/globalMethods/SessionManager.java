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
    
    // ---------- Autenticación contra JSON ----------
    private static UserRole authenticate(String username, String password) {
        UserRecord rec = UserRepository.findByUsername(username);
        if (rec == null) {
            LogUtils.warn("usuario_no_encontrado", "username", username);
            return null;
        }
        if (!rec.enabled) {
            LogUtils.warn("usuario_deshabilitado", "username", username);
            return null;
        }
        boolean ok = PasswordUtils.matches(rec.passwordHash, password);
        if (!ok) {
            LogUtils.warn("password_incorrecto", "username", username);
            return null;
        }
        return rec.role;
    }
    
    public static String getUsername() {
        UserSession u = getCurrent();
        String name = (u != null) ? u.getUsername() : null;
        return (name != null && !name.isBlank()) ? name : "desconocido";
    }

    // Alias opcional si en algún lado ya usabas este nombre:
    public static String getCurrentUsername() {
        return getUsername();
    }

}
