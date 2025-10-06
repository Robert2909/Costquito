package costquito.globalMethods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Repositorio estático de usuarios respaldado en JSON.
 * Archivo en disco: costquito.media/usuarios.json
 *
 * Métodos usados por el proyecto:
 *  - initResource(String classpathResource)
 *  - init(), reload(), save()
 *  - findByRole(UserRole), findByUsername(String)
 *  - updateCredentials(UserRole, username, plainPassword)
 *  - updateUsername(UserRole, username), updatePassword(UserRole, plainPassword)
 */
public final class UserRepository {

    private static final Path USERS_PATH = Paths.get("costquito.media", "usuarios.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(UserRole.class, (com.google.gson.JsonDeserializer<UserRole>) (json, type, ctx) -> {
                if (json == null || json.isJsonNull()) return null;
                String s = json.getAsString();
                if (s == null) return null;
                s = s.trim();
                if (s.equalsIgnoreCase("VENDEDOR")) return UserRole.VENDOR; // alias español
                if (s.equalsIgnoreCase("VENDOR"))   return UserRole.VENDOR;
                if (s.equalsIgnoreCase("ADMIN"))    return UserRole.ADMIN;
                try { return UserRole.valueOf(s.toUpperCase()); }
                catch (IllegalArgumentException ex) { return null; }
            })
            .create();

    private static final java.lang.reflect.Type LIST_TYPE =
            new com.google.gson.reflect.TypeToken<java.util.List<UserRecord>>() {}.getType();


    private static List<UserRecord> cache = new ArrayList<>();
    private static boolean initialized = false;

    private UserRepository() { }

    /** Copia el archivo de ejemplo desde recursos al disco si aún no existe. */
    public static synchronized void initResource(String classpathResource) {
        try {
            if (Files.exists(USERS_PATH)) return;
            Files.createDirectories(USERS_PATH.getParent());
            try (InputStream in = UserRepository.class.getResourceAsStream(classpathResource)) {
                if (in != null) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    Files.writeString(USERS_PATH, json, StandardCharsets.UTF_8);
                } else {
                    // Si no hay recurso, crea por defecto (admin/vendedor con claves 1234)
                    createDefaultsAndSave();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Último recurso: defaults
            createDefaultsAndSave();
        }
    }

    /** Inicializa el repositorio (carga y asegura roles base). */
    public static synchronized void init() {
        if (!initialized) {
            reload();
            migrateKnownDefaultsToPlain();
            normalizeAndDeduplicate();
            ensureCoreRoles();
            initialized = true;
        }
    }

    /** Lee el JSON a memoria. */
    public static synchronized void reload() {
        try {
            if (!Files.exists(USERS_PATH)) {
                cache = new ArrayList<>();
                return;
            }
            String json = Files.readString(USERS_PATH, StandardCharsets.UTF_8);
            List<UserRecord> list = GSON.fromJson(json, LIST_TYPE);
            cache = (list != null) ? list : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            cache = new ArrayList<>();
        }
    }

    /** Persiste el cache al JSON. */
    public static synchronized void save() {
        try {
            if (cache == null) cache = new ArrayList<>();
            Files.createDirectories(USERS_PATH.getParent());
            String json = GSON.toJson(cache, LIST_TYPE);
            Files.writeString(USERS_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Devuelve todos (copia defensiva). */
    public static synchronized List<UserRecord> getAll() {
        init();
        return new ArrayList<>(cache);
    }

    /** Busca por rol. */
    public static synchronized UserRecord findByRole(UserRole role) {
        init();
        UserRecord candidate = null;
        for (UserRecord u : cache) {
            if (u != null && u.role == role) {
                if (u.passwordHash != null && u.passwordHash.trim().regionMatches(true, 0, "plain:", 0, 6)) {
                    return u; // preferimos el que se puede mostrar
                }
                if (candidate == null) candidate = u;
            }
        }
        return candidate;
    }



    /** Busca por username (exacto, case-sensitive). */
    public static synchronized UserRecord findByUsername(String username) {
        init();
        if (username == null) return null;
        for (UserRecord u : cache) {
            if (u != null && username.equals(u.username)) {
                return u;
            }
        }
        return null;
    }

    /** Crea/actualiza username y passwordHash para el rol indicado. */
    public static synchronized void updateCredentials(UserRole role, String username, String passwordPlain) {
        init();
        UserRecord u = findByRole(role);
        if (u == null) {
            u = new UserRecord();
            u.role = role;
            u.enabled = true;
            cache.add(u);
        }
        u.username = username;
        u.passwordHash = "plain:" + (passwordPlain == null ? "" : passwordPlain.trim()); // <-- guardar como plain
    }


    /** Actualiza solo el username. */
    public static synchronized void updateUsername(UserRole role, String username) {
        init();
        UserRecord u = findByRole(role);
        if (u == null) {
            u = new UserRecord();
            u.role = role;
            u.enabled = true;
            cache.add(u);
        }
        u.username = username;
    }

    /** Actualiza solo el passwordHash. */
    public static synchronized void updatePassword(UserRole role, String passwordPlain) {
        init();
        UserRecord u = findByRole(role);
        if (u == null) {
            u = new UserRecord();
            u.role = role;
            u.enabled = true;
            cache.add(u);
        }
        u.passwordHash = "plain:" + (passwordPlain == null ? "" : passwordPlain.trim()); // <-- plain
    }


    // ---------------------------------------------------------------------
    // Internos
    // ---------------------------------------------------------------------

    /** Garantiza que existan ADMIN y VENDOR; si falta alguno, lo crea con 1234. */
    private static void ensureCoreRoles() {
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        for (UserRecord u : cache) {
            if (u != null && u.role != null) roles.add(u.role);
        }
        boolean changed = false;
        if (!roles.contains(UserRole.ADMIN)) {
            cache.add(UserRecord.of(UserRole.ADMIN, "admin", "plain:1234", true));
            changed = true;
        }
        if (!roles.contains(UserRole.VENDOR)) {
            cache.add(UserRecord.of(UserRole.VENDOR, "vendedor", "plain:1234", true));
            changed = true;
        }
        if (changed) save();
    }

    /** Crea archivo con defaults si no existe nada. */
    private static void createDefaultsAndSave() {
        cache = new ArrayList<>();
        cache.add(UserRecord.of(UserRole.ADMIN, "admin", "plain:1234", true));
        cache.add(UserRecord.of(UserRole.VENDOR, "vendedor", "plain:1234", true));
        save();
    }
    
    private static void normalizeAndDeduplicate() {
        boolean changed = false;

        // 1) Si algún registro viene sin role por no mapear, intenta inferir por username/displayName
        for (UserRecord u : new ArrayList<>(cache)) {
            if (u == null) continue;
            if (u.role == null) {
                String un = u.username != null ? u.username.toLowerCase() : "";
                String dn = u.displayName != null ? u.displayName.toLowerCase() : "";
                if (un.equals("vendedor") || dn.contains("vendedor")) { u.role = UserRole.VENDOR; changed = true; }
                if (un.equals("magaly")   || dn.contains("admin"))    { u.role = UserRole.ADMIN;  changed = true; }
            }
        }

        // 2) Si hay duplicados por rol, conserva el "mejor" y elimina el resto
        changed |= dedupByRole(UserRole.ADMIN);
        changed |= dedupByRole(UserRole.VENDOR);

        if (changed) save();
    }

    private static boolean dedupByRole(UserRole role) {
        List<UserRecord> list = new ArrayList<>();
        for (UserRecord u : cache) if (u != null && u.role == role) list.add(u);
        if (list.size() <= 1) return false;

        // Preferir el que tenga password en claro (plain:) o displayName no vacío
        UserRecord keep = null;
        for (UserRecord u : list) {
            if (u.passwordHash != null && u.passwordHash.startsWith("plain:")) { keep = u; break; }
        }
        if (keep == null) {
            for (UserRecord u : list) {
                if (u.displayName != null && !u.displayName.isBlank()) { keep = u; break; }
            }
        }
        if (keep == null) keep = list.get(0);

        // Eliminar los demás
        boolean changed = false;
        for (UserRecord u : list) {
            if (u != keep) { cache.remove(u); changed = true; }
        }
        return changed;
    }
    
    // Conversión automática de hashes "sha256:1234" a "plain:1234"
    private static void migrateKnownDefaultsToPlain() {
        boolean changed = false;

        // Hash de "1234" (coincide con el que viste en el log)
        final String SHA256_1234 = "sha256:03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4";

        for (UserRecord u : cache) {
            if (u == null || u.passwordHash == null) continue;
            String h = u.passwordHash.trim().toLowerCase();
            if (h.equals(SHA256_1234)) {           // si es ese hash exacto
                u.passwordHash = "plain:1234";     // lo migramos a plano
                changed = true;
            }
        }
        if (changed) save();
    }

}
