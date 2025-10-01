package costquito.globalMethods;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtils {

    private PasswordUtils() {}

    /** Verifica contra un hash con prefijo. Soporta "plain:" y "sha256:" */
    public static boolean matches(String storedHash, String rawPassword) {
        if (storedHash == null || rawPassword == null) return false;
        if (storedHash.startsWith("plain:")) {
            String expected = storedHash.substring("plain:".length());
            return rawPassword.equals(expected);
        }
        if (storedHash.startsWith("sha256:")) {
            String hex = storedHash.substring("sha256:".length());
            return hex.equalsIgnoreCase(sha256Hex(rawPassword));
        }
        // Desconocido → nunca coincide
        return false;
    }

    /** Genera sha256:<hex> para que puedas poblar el JSON con hashes. */
    public static String sha256Tagged(String rawPassword) {
        return "sha256:" + sha256Hex(rawPassword);
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            LogUtils.error("sha256_error", e);
            return "";
        }
    }
}
