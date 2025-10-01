package costquito.globalMethods;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LogUtils {

    // ===================== Niveles =====================
    public enum Level {
        INFO(20), AUDIT(30), WARN(40), ERROR(50);
        final int sev;
        Level(int s) { this.sev = s; }
    }

    // ===================== Config =====================
    private static Path logDir = Paths.get("logs");          // por defecto ./logs
    private static final String FILE_PREFIX = "app-";
    private static final String FILE_SUFFIX = ".log.jsonl";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT  = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static volatile boolean consoleMirror = true;     // espejo en consola
    private static volatile Level minLevel = Level.AUDIT;     // umbral (por defecto muestra AUDIT+)

    // ===================== Estado =====================
    private static LocalDate currentDay = null;
    private static Path currentFile = null;
    private static final Object WRITE_LOCK = new Object();

    // Contexto global (se añade a cada log)
    private static final Map<String, Object> context = new ConcurrentHashMap<>();

    private LogUtils() {}

    // ===================== API pública =====================

    /** Inicializa el directorio de logs (opcional). Si es null, usa ./logs */
    public static void init(Path customDir) {
        synchronized (WRITE_LOCK) {
            if (customDir != null) logDir = customDir;
            ensureFileForToday();
        }
    }

    /** Cambia el nivel mínimo a registrar (INFO, AUDIT, WARN, ERROR). */
    public static void setMinLevel(Level level) {
        if (level != null) minLevel = level;
    }

    /** Activa/desactiva el espejo a consola. */
    public static void setConsoleMirror(boolean enabled) {
        consoleMirror = enabled;
    }

    /** Define el usuario actual para incluirlo en los logs. */
    public static void setCurrentUser(String user) {
        if (user == null || user.isBlank()) context.remove("user");
        else context.put("user", user);
    }

    /** Define la vista/route actual (ej. Views.MENU). */
    public static void setCurrentView(String view) {
        if (view == null || view.isBlank()) context.remove("view");
        else context.put("view", view);
    }

    /** Agrega/actualiza una clave de contexto global personalizada. */
    public static void setContext(String key, Object value) {
        if (key == null) return;
        if (value == null) context.remove(key);
        else context.put(key, value);
    }

    /** Limpia todo el contexto global. */
    public static void clearContext() {
        context.clear();
    }

    // -------- Atajos --------
    public static void info(String message, Object... kv)  { log(Level.INFO,  null,   message, null, kv); }
    public static void audit(String event, Object... kv)   { log(Level.AUDIT, event,  null,    null, kv); }
    public static void warn(String message, Object... kv)  { log(Level.WARN,  null,   message, null, kv); }
    public static void error(String message, Throwable t, Object... kv) {
        log(Level.ERROR, null, message, t, kv);
    }

    // ===================== Núcleo =====================

    private static void log(Level level, String event, String message, Throwable t, Object... kv) {
        // Filtrado por umbral
        if (level.sev < minLevel.sev) return;

        Map<String, Object> extras = kvToMap(kv);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("ts", ZonedDateTime.now(ZoneId.systemDefault()).format(TS_FMT));
        entry.put("level", level.name());
        if (event != null)   entry.put("event", event);
        if (message != null) entry.put("message", message);

        // contexto común
        String user = (String) context.get("user");
        String view = (String) context.get("view");
        if (user != null) entry.put("user", user);
        if (view != null) entry.put("view", view);
        entry.put("thread", Thread.currentThread().getName());

        if (!extras.isEmpty()) entry.put("extras", extras);
        if (t != null) entry.put("error", throwableToMap(t));

        String json = toJson(entry);

        // Escritura a archivo (thread-safe)
        synchronized (WRITE_LOCK) {
            ensureFileForToday();
            try {
                Files.writeString(
                    currentFile,
                    json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // Espejo a consola
        if (consoleMirror) {
            if (level == Level.ERROR) System.err.println(json);
            else System.out.println(json);
        }
    }

    private static void ensureFileForToday() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay) || currentFile == null) {
            try {
                Files.createDirectories(logDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            currentDay = today;
            String fileName = FILE_PREFIX + DAY_FMT.format(today) + FILE_SUFFIX;
            currentFile = logDir.resolve(fileName);
            if (!Files.exists(currentFile)) {
                try {
                    Files.createFile(currentFile);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    // ===================== Helpers =====================

    /** Convierte pares key,value... a Map. Si cantidad es impar, ignora el último valor suelto. */
    private static Map<String, Object> kvToMap(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (kv == null) return map;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object k = kv[i];
            Object v = kv[i + 1];
            if (k != null) map.put(String.valueOf(k), v);
        }
        return map;
    }

    private static Map<String, Object> throwableToMap(Throwable t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", t.getClass().getName());
        m.put("message", t.getMessage());
        m.put("stack", stackToString(t));
        return m;
    }

    private static String stackToString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append(el.toString()).append("\n");
        }
        return sb.toString().trim();
    }

    // JSON simple sin dependencias externas
    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String s) return "\"" + escapeJson(s) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(o));
            }
            sb.append("]");
            return sb.toString();
        }
        // fallback
        return toJson(String.valueOf(obj));
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
