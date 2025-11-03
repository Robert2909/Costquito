package costquito.globalMethods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Repo de ventas particionado por día:
 *   src/costquito/media/ventas/ventas-YYYY-MM-DD.json
 * Folio global en:
 *   src/costquito/media/folio.json  -> { "nextFolio": 7 }
 */
public final class SalesRepository {

    private SalesRepository() {}

    // ====== Tipos ======
    public static final class SaleRecord {
        public String uuid;
        public int    folio;
        public String fecha;
        public String hora;
        public String cashierUsername;
        public String metodoPago;
        public double total;
        public double pago;
        public double cambio;
        public boolean inventoryApplied;
        public List<SaleItem> items = new ArrayList<>();
    }

    public static final class SaleItem {
        public String productId;
        public String nombreSnapshot;
        public double precioUnitario;
        public int    cantidad;
        public double totalLinea;
    }

    private static final class FolioState { int nextFolio; }

    // ====== Constantes / paths base ======
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_SALES = new TypeToken<List<SaleRecord>>() {}.getType();
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Directorio base local para media (sin AppUtils)
    private static final Path BASE_DIR   = Paths.get("src", "costquito", "media");
    private static Path ventasDir()      { return BASE_DIR.resolve("ventas"); }
    private static Path folioFile()      { return BASE_DIR.resolve("folio.json"); }
    private static Path fileFor(LocalDate d) {
        String name = "ventas-" + FILE_DATE_FMT.format(d) + ".json";
        return ventasDir().resolve(name);
    }

    // ====== Setup ======
    private static void ensureLayout() {
        try {
            Files.createDirectories(ventasDir());
            // BASE_DIR también, por si no existe:
            Files.createDirectories(BASE_DIR);
        } catch (IOException e) {
            throw new RuntimeException("No pude crear carpeta de ventas: " + ventasDir(), e);
        }
        ensureFolioFile();
    }

    private static synchronized void ensureFolioFile() {
        try {
            if (Files.notExists(folioFile())) {
                int max = scanAllFilesGetMaxFolio();
                FolioState st = new FolioState();
                st.nextFolio = max + 1;
                try (Writer w = Files.newBufferedWriter(
                        folioFile(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    GSON.toJson(st, w);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No pude inicializar folio.json", e);
        }
    }

    private static int scanAllFilesGetMaxFolio() throws IOException {
        if (Files.notExists(ventasDir())) return 0;
        try (Stream<Path> s = Files.list(ventasDir())) {
            List<Path> jsons = s.filter(p -> {
                        String fn = p.getFileName().toString();
                        return fn.startsWith("ventas-") && fn.endsWith(".json");
                    })
                    .collect(Collectors.toList());

            int max = 0;
            for (Path p : jsons) {
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    @SuppressWarnings("unchecked")
                    List<SaleRecord> list =
                            Optional.ofNullable((List<SaleRecord>) GSON.fromJson(r, LIST_SALES))
                                    .orElseGet(ArrayList::new);
                    for (SaleRecord sr : list) {
                        if (sr != null) max = Math.max(max, sr.folio);
                    }
                } catch (Exception ignore) {
                    // Si hay un archivo roto, lo saltamos
                }
            }
            return max;
        }
    }

    // ====== API pública ======

    /** Devuelve las ventas del día indicado (si no existe el archivo, lista vacía). */
    public static List<SaleRecord> loadSalesFor(LocalDate date) {
        ensureLayout();
        Path f = fileFor(date);
        if (Files.notExists(f)) return new ArrayList<>();
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            @SuppressWarnings("unchecked")
            List<SaleRecord> list =
                    Optional.ofNullable((List<SaleRecord>) GSON.fromJson(r, LIST_SALES))
                            .orElseGet(ArrayList::new);
            return list;
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo " + f, e);
        }
    }

    /** Alias de compatibilidad: mismo comportamiento que loadSalesFor(date). */
    public static List<SaleRecord> loadByDate(LocalDate date) {
        return loadSalesFor(date);
    }

    /** Devuelve las ventas de HOY. */
    public static List<SaleRecord> loadToday() {
        return loadSalesFor(LocalDate.now());
    }

    /** Agrega una venta al archivo de HOY y actualiza el folio global. */
    public static synchronized void appendToday(SaleRecord sale) {
        ensureLayout();

        // Asignar folio si viene 0
        if (sale.folio <= 0) {
            sale.folio = nextFolioAndIncrement();
        }

        Path f = fileFor(LocalDate.now());
        List<SaleRecord> current = loadToday();
        current.add(sale);

        // Escritura atómica: escribir a .tmp y mover
        Path tmp = f.resolveSibling(f.getFileName().toString() + ".tmp");
        try (Writer w = Files.newBufferedWriter(
                tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(current, w);
        } catch (IOException e) {
            throw new RuntimeException("Error escribiendo temporal " + tmp, e);
        }
        try {
            Files.createDirectories(f.getParent());
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("No pude mover " + tmp + " a " + f, e);
        }
    }

    /** Lee, incrementa y persiste el siguiente folio global. */
    public static synchronized int nextFolioAndIncrement() {
        ensureLayout();
        // Leer estado actual
        FolioState st;
        try (Reader r = Files.newBufferedReader(folioFile(), StandardCharsets.UTF_8)) {
            st = Optional.ofNullable(GSON.fromJson(r, FolioState.class))
                    .orElseGet(FolioState::new);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo folio.json", e);
        }

        if (st.nextFolio <= 0) {
            try {
                st.nextFolio = scanAllFilesGetMaxFolio() + 1;
            } catch (IOException e) {
                st.nextFolio = 1;
            }
        }

        int result = st.nextFolio;
        st.nextFolio = result + 1;

        // Persistir
        try (Writer w = Files.newBufferedWriter(
                folioFile(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(st, w);
        } catch (IOException e) {
            throw new RuntimeException("Error actualizando folio.json", e);
        }

        return result;
    }
}
