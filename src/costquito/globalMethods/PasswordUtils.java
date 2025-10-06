package costquito.globalMethods;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtils {

    private PasswordUtils() { }

    /** Genera hash prefijado: "sha256:<hex>" */
    public static String hash(String plain) {
        if (plain == null) plain = "";
        return "sha256:" + sha256Hex(plain);
    }

    /** Verifica hash soportando "plain:", "sha256:" y hex sin prefijo. */
    public static boolean matches(String expectedHash, String plain) {
        if (expectedHash == null) expectedHash = "";
        if (plain == null) plain = "";

        if (expectedHash.startsWith("plain:")) {
            return slowEquals(expectedHash.substring(6), plain);
        }
        if (expectedHash.startsWith("sha256:")) {
            String hex = expectedHash.substring(7);
            return slowEquals(hex, sha256Hex(plain));
        }
        // Compatibilidad: si viene un hex sin prefijo, asumimos sha256.
        return slowEquals(expectedHash, sha256Hex(plain));
    }

    // ---- helpers ----

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit((b & 0xF), 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback (no debería ocurrir)
            return "";
        }
    }

    /** Comparación en tiempo (casi) constante. */
    private static boolean slowEquals(String a, String b) {
        int diff = a.length() ^ b.length();
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
