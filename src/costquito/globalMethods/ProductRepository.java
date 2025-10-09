package costquito.globalMethods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class ProductRepository {
    
    // === Base configurable ===
    private static Path MEDIA_DIR = Paths.get("src", "costquito", "media");
    private static Path PRODUCTS_PATH = MEDIA_DIR.resolve("productos.json");

    public static synchronized void setBaseDir(Path dir) {
        if (dir != null) MEDIA_DIR = dir;
        PRODUCTS_PATH = MEDIA_DIR.resolve("productos.json");
        LogUtils.info("products_base_dir_set",
                "base", MEDIA_DIR.toAbsolutePath().toString(),
                "file", PRODUCTS_PATH.toAbsolutePath().toString());
    }

    // Gson
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final Type LIST_TYPE = new TypeToken<List<ProductRecord>>() {}.getType();

    // Cache en memoria
    private static List<ProductRecord> cache = new ArrayList<>();
    private static boolean initialized = false;

    private ProductRepository() {}

    // ====== Init / Reload / Save ======
    public static synchronized void init() {
        if (initialized) return;
        reload();
        initialized = true;
        LogUtils.info("products_init",
                "abs", PRODUCTS_PATH.toAbsolutePath().toString(),
                "exists", Files.exists(PRODUCTS_PATH));
    }

    public static synchronized void setPath(Path p) {
        // Solo si alguna vez quieres redefinir la ruta en tiempo de ejecución
        PRODUCTS_PATH = Objects.requireNonNull(p);
    }

    /** Lee de disco. Si está corrupto, renombra a .bad y arranca vacío. */
    public static synchronized void reload() {
        try {
            if (!Files.exists(PRODUCTS_PATH)) {
                cache = new ArrayList<>();
                return;
            }
            String json = Files.readString(PRODUCTS_PATH, StandardCharsets.UTF_8);
            List<ProductRecord> list = GSON.fromJson(json, LIST_TYPE);
            cache = (list != null) ? list : new ArrayList<>();
            normalize();
        } catch (Exception e) {
            // Respaldo .bad y arranque vacío
            try {
                Path bad = PRODUCTS_PATH.resolveSibling(PRODUCTS_PATH.getFileName() + "." + Instant.now().toEpochMilli() + ".bad");
                Files.createDirectories(PRODUCTS_PATH.getParent());
                if (Files.exists(PRODUCTS_PATH)) {
                    Files.move(PRODUCTS_PATH, bad, StandardCopyOption.REPLACE_EXISTING);
                }
                LogUtils.warn("products_json_corrupto_backup", "backup", bad.toAbsolutePath().toString());
            } catch (IOException ignored) { }
            cache = new ArrayList<>();
        }
        LogUtils.audit("products_reloaded",
                "path", PRODUCTS_PATH.toAbsolutePath().toString(),
                "count", cache.size(),
                "exists", java.nio.file.Files.exists(PRODUCTS_PATH));
    }

    /** Guarda con escritura atómica para evitar corrupción. */
    public static synchronized void save() {
        try {
            Files.createDirectories(PRODUCTS_PATH.getParent());
            String json = GSON.toJson(cache, LIST_TYPE);

            Path tmp = PRODUCTS_PATH.resolveSibling(PRODUCTS_PATH.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Move atómico (si el FS lo soporta) + reemplazo
            try {
                Files.move(tmp, PRODUCTS_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, PRODUCTS_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LogUtils.error("products_save_error", e, "path", PRODUCTS_PATH.toAbsolutePath().toString());
            throw new RuntimeException("Error guardando productos.json", e);
        }
        LogUtils.audit("products_saved",
                "path", PRODUCTS_PATH.toAbsolutePath().toString(),
                "count", cache.size());
    }

    // ====== Validaciones ======
    public static boolean isValidNombre(String nombre) {
        if (nombre == null) return false;
        String n = nombre.trim();
        if (n.isEmpty()) return false;
        if (n.length() > 200) return false;
        // Sin emojis (básico): filtra surrogate pairs altos
        for (int i = 0; i < n.length(); i++) {
            if (Character.isSurrogate(n.charAt(i))) return false;
        }
        return true;
    }

    public static boolean isValidPrecio(double precio) {
        return (precio >= 0.0) && (precio <= 999_999.99);
    }

    public static boolean isValidCantidad(int cantidad) {
        return cantidad >= 0 && cantidad <= 100_000;
    }

    // ====== CRUD ======
    public static synchronized List<ProductRecord> getAll() {
        init();
        return new ArrayList<>(cache);
    }

    public static synchronized List<ProductRecord> searchByName(String q) {
        init();
        String s = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        return cache.stream()
                .filter(p -> p != null && (s.isEmpty() || p.nombre.toLowerCase(Locale.ROOT).contains(s)))
                .sorted(Comparator.comparing(p -> p.nombre.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    /** Agrega con unicidad por nombre (case-insensitive, trim). */
    public static synchronized void add(ProductRecord p) {
        init();
        Objects.requireNonNull(p, "product");
        if (!isValidNombre(p.nombre) || !isValidPrecio(p.precio) || !isValidCantidad(p.cantidad)) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        String key = keyName(p.nombre);
        if (cache.stream().anyMatch(x -> keyName(x.nombre).equals(key))) {
            throw new IllegalArgumentException("Nombre duplicado");
        }
        if (p.id == null || p.id.isBlank()) p.id = java.util.UUID.randomUUID().toString();
        cache.add(p);
        save(); // <-- persiste a disco
    }

    /** Update por id. Revalida nombre/duplicados. */
    public static synchronized void update(ProductRecord upd) {
        init();
        Objects.requireNonNull(upd, "product");
        if (upd.id == null || upd.id.isBlank()) throw new IllegalArgumentException("id requerido");
        if (!isValidNombre(upd.nombre) || !isValidPrecio(upd.precio) || !isValidCantidad(upd.cantidad)) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        int idx = indexOfId(upd.id);
        if (idx < 0) throw new IllegalArgumentException("No existe id");

        String key = keyName(upd.nombre);
        // nombre duplicado con otro registro
        for (ProductRecord x : cache) {
            if (!x.id.equals(upd.id) && keyName(x.nombre).equals(key)) {
                throw new IllegalArgumentException("Nombre duplicado");
            }
        }
        cache.set(idx, upd);
        save(); // <-- persiste a disco
    }

    public static synchronized void deleteById(String id) {
        init();
        int idx = indexOfId(id);
        if (idx >= 0) {
            cache.remove(idx);
            save(); // <-- persiste a disco
        }
    }

    // ====== Import / Export ======
    /** Exporta el JSON actual al path indicado. */
    public static synchronized void exportTo(Path target) {
        init();
        Objects.requireNonNull(target, "target");
        try {
            Files.createDirectories(target.getParent());
            String json = GSON.toJson(cache, LIST_TYPE);
            Files.writeString(target, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LogUtils.info("products_export_ok", "to", target.toAbsolutePath().toString());
        } catch (IOException e) {
            LogUtils.error("products_export_error", e, "to", target.toAbsolutePath().toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * Importa desde un archivo externo.
     * @param source archivo JSON con el arreglo de productos
     * @param replaceAll si true, reemplaza por completo; si false, intenta MEZCLAR:
     *                   - si id coincide, actualiza
     *                   - si nombre (normalizado) coincide con otro id, lanza error para evitar colisiones
     *                   - si no coincide, agrega nuevo
     */
    public static synchronized void importFrom(Path source, boolean replaceAll) {
        init();
        Objects.requireNonNull(source, "source");
        try {
            String json = Files.readString(source, StandardCharsets.UTF_8);
            List<ProductRecord> incoming = GSON.fromJson(json, LIST_TYPE);
            if (incoming == null) incoming = new ArrayList<>();
            // Validación básica
            for (ProductRecord p : incoming) {
                if (p == null || !isValidNombre(p.nombre) || !isValidPrecio(p.precio) || !isValidCantidad(p.cantidad)) {
                    throw new IllegalArgumentException("Datos inválidos en archivo a importar");
                }
            }
            if (replaceAll) {
                cache = new ArrayList<>(incoming);
                dedupAndFixIds();
                save();
                LogUtils.audit("products_import_replace_ok", "count", cache.size());
            } else {
                // Merge seguro
                Map<String, ProductRecord> byId = cache.stream().collect(Collectors.toMap(x -> x.id, x -> x));
                Set<String> nameKeys = cache.stream().map(x -> keyName(x.nombre)).collect(Collectors.toSet());

                for (ProductRecord p : incoming) {
                    if (p.id != null && byId.containsKey(p.id)) {
                        // actualiza por id
                        ProductRecord current = byId.get(p.id);
                        // si cambia a un nombre que ya existe en otro producto => error
                        String k = keyName(p.nombre);
                        if (!keyName(current.nombre).equals(k) && nameKeys.contains(k)) {
                            throw new IllegalArgumentException("Conflicto de nombre al importar: " + p.nombre);
                        }
                        byId.put(p.id, p);
                    } else {
                        // nuevo: valida nombre único
                        String k = keyName(p.nombre);
                        if (nameKeys.contains(k)) {
                            throw new IllegalArgumentException("Duplicado por nombre al importar: " + p.nombre);
                        }
                        if (p.id == null || p.id.isBlank()) p.id = UUID.randomUUID().toString();
                        byId.put(p.id, p);
                        nameKeys.add(k);
                    }
                }
                cache = new ArrayList<>(byId.values());
                save();
                LogUtils.audit("products_import_merge_ok", "count", cache.size());
            }
        } catch (IOException e) {
            LogUtils.error("products_import_io_error", e, "from", source.toAbsolutePath().toString());
            throw new RuntimeException(e);
        }
    }

    // ====== Helpers ======
    private static String keyName(String n) {
        return (n == null ? "" : n.trim().toLowerCase(Locale.ROOT));
    }

    private static int indexOfId(String id) {
        if (id == null) return -1;
        for (int i = 0; i < cache.size(); i++) {
            ProductRecord p = cache.get(i);
            if (p != null && id.equals(p.id)) return i;
        }
        return -1;
    }

    private static void normalize() {
        // asegúrate de tener ids
        dedupAndFixIds();
        // quita nulls accidentales
        cache = cache.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static void dedupAndFixIds() {
        for (ProductRecord p : cache) {
            if (p.id == null || p.id.isBlank()) p.id = UUID.randomUUID().toString();
        }
        // Opcional: si hubiera ids duplicados (raro), re-asígnalos
        Set<String> seen = new HashSet<>();
        for (ProductRecord p : cache) {
            if (!seen.add(p.id)) p.id = UUID.randomUUID().toString();
        }
    }
}
