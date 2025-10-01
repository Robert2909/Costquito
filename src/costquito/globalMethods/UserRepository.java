package costquito.globalMethods;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga usuarios desde un JSON (array de objetos):
 * username, passwordHash ("plain:" o "sha256:"), role, enabled, displayName.
 * 1) Primero intenta classpath (recomendado): "/costquito/media/usuarios.json"
 * 2) Si no existe, intenta un archivo externo (Path).
 */
public final class UserRepository {

    // Fuente preferida: recurso en el classpath
    private static String resourcePath = "/costquito/media/usuarios.json";
    // Alternativa: archivo externo
    private static Path filePath = null;

    private static final Map<String, UserRecord> byUsername = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private UserRepository() {}

    /** Usar un recurso dentro del jar/classpath. Ej: "/costquito/media/usuarios.json" */
    public static synchronized void initResource(String classpathResource) {
        if (classpathResource != null && !classpathResource.isBlank()) {
            resourcePath = classpathResource;
        }
        filePath = null; // prioriza recurso
        reload();
    }

    /** Usar un archivo externo en disco. Ej: Paths.get("config/usuarios.json") */
    public static synchronized void init(Path externalFile) {
        filePath = externalFile;
        // mantenemos también el recurso por si quieres fallback, pero priorizamos archivo
        reload();
    }

    public static synchronized void reload() {
        String content = null;

        // 1) Intentar leer del classpath
        if (resourcePath != null) {
            try (InputStream in = UserRepository.class.getResourceAsStream(resourcePath)) {
                if (in != null) {
                    content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    LogUtils.audit("usuarios_cargados_classpath", "path", resourcePath);
                }
            } catch (IOException e) {
                LogUtils.warn("usuarios_classpath_read_warn", "path", resourcePath);
            }
        }

        // 2) Si no hubo contenido, intentar archivo externo
        if (content == null && filePath != null) {
            try {
                content = Files.readString(filePath, StandardCharsets.UTF_8);
                LogUtils.audit("usuarios_cargados_file", "path", filePath.toString());
            } catch (IOException e) {
                LogUtils.error("error_cargando_usuarios_file", e, "path", filePath.toString());
                throw new UncheckedIOException(e);
            }
        }

        // 3) Si seguimos sin contenido, error claro
        if (content == null) {
            String msg = "No se encontró usuarios.json ni en classpath (" + resourcePath + ") ni como archivo externo.";
            LogUtils.error(msg, null);
            throw new IllegalStateException(msg);
        }

        // Parseo simple del JSON esperado
        List<Map<String, String>> rows = parseArrayOfObjects(content);
        byUsername.clear();
        for (Map<String, String> m : rows) {
            String username = get(m, "username");
            String passHash = get(m, "passwordHash");
            String roleStr  = get(m, "role");
            String enabledS = get(m, "enabled");
            String display  = get(m, "displayName");
            if (username == null || passHash == null || roleStr == null) continue;

            boolean enabled = !"false".equalsIgnoreCase(enabledS);
            UserRole role;
            try { role = UserRole.valueOf(roleStr.trim().toUpperCase()); }
            catch (Exception e) { role = UserRole.VENDEDOR; }

            UserRecord rec = new UserRecord(username.trim(), passHash.trim(), role, enabled, display);
            byUsername.put(rec.username.toLowerCase(), rec);
        }
        loaded = true;
        LogUtils.audit("usuarios_indexados", "count", byUsername.size());
    }

    public static UserRecord findByUsername(String username) {
        if (!loaded) reload();
        if (username == null) return null;
        return byUsername.get(username.toLowerCase());
    }

    // ---------- Helpers JSON mínimos (para el formato previsto) ----------

    private static List<Map<String, String>> parseArrayOfObjects(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        if (json == null) return list;

        json = json.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*(?=\\n)", "");

        int i = skipWs(json, 0);
        if (i >= json.length() || json.charAt(i) != '[') return list;
        i++;
        while (true) {
            i = skipWs(json, i);
            if (i >= json.length()) break;
            if (json.charAt(i) == ']') { i++; break; }
            ParseObj po = readObject(json, i);
            if (po == null) break;
            list.add(po.map);
            i = skipWs(json, po.next);
            if (i < json.length() && json.charAt(i) == ',') { i++; continue; }
            if (i < json.length() && json.charAt(i) == ']') { i++; break; }
        }
        return list;
    }

    private static class ParseObj {
        Map<String, String> map; int next;
        ParseObj(Map<String, String> m, int n) { map = m; next = n; }
    }

    private static ParseObj readObject(String s, int i) {
        i = skipWs(s, i);
        if (i >= s.length() || s.charAt(i) != '{') return null;
        i++;
        Map<String, String> m = new LinkedHashMap<>();
        while (true) {
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == '}') { i++; break; }
            String key = readString(s, i);
            if (key == null) return null;
            i = nextPos;
            i = skipWs(s, i);
            if (i >= s.length() || s.charAt(i) != ':') return null;
            i++;
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == '"') {
                String val = readString(s, i);
                if (val == null) return null;
                i = nextPos;
                m.put(key, val);
            } else {
                int j = i;
                while (j < s.length() && ",}] \t\r\n".indexOf(s.charAt(j)) == -1) j++;
                String raw = s.substring(i, j).trim();
                m.put(key, raw);
                i = j;
            }
            i = skipWs(s, i);
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { i++; break; }
        }
        return new ParseObj(m, i);
    }

    private static int nextPos = 0;

    private static String readString(String s, int i) {
        i = skipWs(s, i);
        if (i >= s.length() || s.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '\\') {
                if (i >= s.length()) break;
                char n = s.charAt(i++);
                switch (n) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 <= s.length()) {
                            String hex = s.substring(i, i + 4);
                            try { sb.append((char) Integer.parseInt(hex, 16)); } catch (Exception ignore) {}
                            i += 4;
                        }
                        break;
                    default: sb.append(n);
                }
            } else if (c == '"') {
                nextPos = i;
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static int skipWs(String s, int i) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
            else break;
        }
        return i;
    }

    private static String get(Map<String, String> m, String k) {
        return m.get(k);
    }
}
