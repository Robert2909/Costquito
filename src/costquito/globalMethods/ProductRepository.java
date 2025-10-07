package costquito.globalMethods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public final class ProductRepository {

    private static final Path FILE = Paths.get("costquito.media", "productos.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<ProductRecord>>() {}.getType();

    private static boolean initialized = false;
    private static final List<ProductRecord> cache = new ArrayList<>();

    private ProductRepository() {}

    /* ============================ Init/Carga ============================ */

    public static synchronized void init() {
        if (initialized) return;
        reload();
        initialized = true;
    }

    public static synchronized void reload() {
        try {
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                save(); // crea []
                return;
            }
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            List<ProductRecord> data = GSON.fromJson(json, LIST_TYPE);
            cache.clear();
            if (data != null) cache.addAll(data);
            normalizeAndFix();
        } catch (Exception ex) {
            // Archivo corrupto → backup .bad y regenerar
            try {
                Files.createDirectories(FILE.getParent());
                Path backup = FILE.resolveSibling(FILE.getFileName().toString() + ".bad");
                try {
                    Files.move(FILE, backup, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
                cache.clear();
                save();
                LogUtils.warn("productos_json_corrupto_regenerado", "backup", backup.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            String json = GSON.toJson(cache, LIST_TYPE);
            // Escritura atómica
            Path tmp = Files.createTempFile(FILE.getParent(), "productos-", ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /* ============================ Consultas ============================ */
    
    /** Busca por nombre (case-insensitive, trim). Si query es vacía o nula, devuelve todo ordenado. */
    public static synchronized List<ProductRecord> searchByName(String query) {
        init();
        String q = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));
        if (q.isEmpty()) return getAllSortedByName();
        return cache.stream()
                .filter(p -> normalizeKey(p.nombre).contains(q))
                .sorted(Comparator.comparing(p -> normalizeKey(p.nombre)))
                .collect(java.util.stream.Collectors.toList());
    }

    public static synchronized List<ProductRecord> getAllSortedByName() {
        init();
        return cache.stream()
                .sorted(Comparator.comparing(p -> normalizeKey(p.nombre)))
                .collect(Collectors.toList());
    }

    public static synchronized ProductRecord findById(String id) {
        init();
        if (id == null) return null;
        for (ProductRecord p : cache) if (id.equals(p.id)) return p;
        return null;
    }

    public static synchronized ProductRecord findByNameUnique(String nombre) {
        init();
        if (nombre == null) return null;
        String key = normalizeKey(nombre);
        for (ProductRecord p : cache) {
            if (normalizeKey(p.nombre).equals(key)) return p;
        }
        return null;
    }

    /* ============================ Mutadores ============================ */

    public static synchronized void add(ProductRecord p) {
        init();
        Objects.requireNonNull(p);
        // Unicidad de nombre (case-insensitive + trim)
        if (findByNameUnique(p.nombre) != null) {
            throw new IllegalArgumentException("Nombre de producto duplicado.");
        }
        cache.add(p);
        save();
    }

    public static synchronized void update(ProductRecord updated) {
        init();
        Objects.requireNonNull(updated);
        ProductRecord current = findById(updated.id);
        if (current == null) throw new IllegalArgumentException("Producto no encontrado.");

        // Si cambió el nombre, verificar unicidad
        if (!normalizeKey(current.nombre).equals(normalizeKey(updated.nombre))) {
            if (findByNameUnique(updated.nombre) != null) {
                throw new IllegalArgumentException("Nombre de producto duplicado.");
            }
        }
        current.nombre   = updated.nombre;
        current.precio   = updated.precio;
        current.cantidad = updated.cantidad;
        save();
    }

    public static synchronized void deleteById(String id) {
        init();
        cache.removeIf(p -> Objects.equals(p.id, id));
        save();
    }

    /* ============================ Helpers ============================ */

    private static void normalizeAndFix() {
        // Limpieza básica si hay nulos o ids vacíos
        Iterator<ProductRecord> it = cache.iterator();
        while (it.hasNext()) {
            ProductRecord p = it.next();
            if (p == null) { it.remove(); continue; }
            if (p.id == null || p.id.isBlank()) p.id = java.util.UUID.randomUUID().toString();
            if (p.nombre == null) p.nombre = "";
            if (p.precio < 0) p.precio = 0.0;
            if (p.cantidad < 0) p.cantidad = 0;
        }
        // Quitar duplicados por nombre normalizado dejando el primero
        Set<String> seen = new HashSet<>();
        it = cache.iterator();
        while (it.hasNext()) {
            ProductRecord p = it.next();
            String key = normalizeKey(p.nombre);
            if (seen.contains(key)) it.remove();
            else seen.add(key);
        }
        save();
    }

    // Unicidad: case-insensitive + trim (no quitamos tildes).
    private static String normalizeKey(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    // Validaciones públicas reutilizables
    public static boolean isValidNombre(String nombre) {
        if (nombre == null) return false;
        String trimmed = nombre.trim();
        if (trimmed.isEmpty() || trimmed.length() > 200) return false;
        // Sin emojis: rechazamos codepoints fuera del BMP (aprox. emojis)
        return trimmed.codePoints().noneMatch(cp -> cp > 0xFFFF);
    }

    public static boolean isValidPrecio(double precio) {
        return precio >= 0.0 && precio <= 999999.99;
    }

    public static boolean isValidCantidad(int cantidad) {
        return cantidad >= 0 && cantidad <= 100000;
    }
}
