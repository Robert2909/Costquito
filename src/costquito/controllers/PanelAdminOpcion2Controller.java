package costquito.controllers;

import costquito.globalMethods.AppUtils;
import costquito.globalMethods.LogUtils;
import costquito.globalMethods.ProductRecord;
import costquito.globalMethods.ProductRepository;
import costquito.globalMethods.SalesRepository;
import costquito.globalMethods.SaleRecord;
import costquito.globalMethods.SessionManager;
import costquito.globalMethods.UserSession;
import costquito.globalMethods.Views;
import costquito.globalMethods.WindowUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.scene.image.ImageView;

public class PanelAdminOpcion2Controller implements Initializable {

    /* ===================== FXML ===================== */
    @FXML private AnchorPane PanelAdminOpcion2AnchorPane;

    @FXML private Button administracionButton;
    @FXML private Button agregarProductoVentaButton;
    @FXML private Button aumentar1CantidadProductoVentaButton;
    @FXML private TextField barraBusquedaTextField;
    @FXML private Pane barraSuperiorPane;
    @FXML private Button buscarProductoButton;
    @FXML private Button cancelarSeleccionProductosButton;
    @FXML private Button cancelarSeleccionVentaButton;

    @FXML private Label cantidadCambioVentaLabel;
    @FXML private TextField cantidadCambioVentaTextField;

    @FXML private Label cantidadPagoVentaLabel;
    @FXML private TextField cantidadPagoVentaTextField;

    @FXML private TableColumn<ProductRecord, Number> cantidadTableColumn;
    @FXML private Label cantidadTotalVentaLabel;
    @FXML private TextField cantidadTotalVentaTextField;

    @FXML private Button cerrarButton;
    @FXML private Pane contenidoPane;

    @FXML private Button cuentasButton;

    @FXML private ComboBox<String> elegirMetodoPagoComboBox;

    @FXML private Button eliminarProductoVentaButton;

    @FXML private TableColumn<ProductRecord, String> idTableColumn;

    @FXML private Pane listaProductosVentaPane;

    @FXML private TableView<SaleLine> listaProductosVentaTableView;

    @FXML private ImageView logoImageView;
    @FXML private Pane logoPane;
    @FXML private Label metodoPagoLabel;

    @FXML private TableColumn<ProductRecord, String> nombreTableColumn;

    @FXML private Label notificacionLabel;

    @FXML private TableColumn<SaleLine, String> precioIndividualTableColumn;
    @FXML private TableColumn<ProductRecord, String> precioTableColumn;
    @FXML private TableColumn<SaleLine, String> precioTotalTableColumn;

    @FXML private Pane productosPane;
    @FXML private TableView<ProductRecord> productosTableView;

    @FXML private Button reducir1CantidadProductoVentaButton;
    @FXML private Button registrarVentaButton;

    @FXML private Button regresarButton;
    @FXML private Button reportesButton;

    @FXML private Label tituloProductosLabel;
    @FXML private Label tituloVentaLabel;

    @FXML private Button ventasButton;

    /* ===================== Estado ===================== */
    private final ObservableList<ProductRecord> inventarioData = FXCollections.observableArrayList();
    private final ObservableList<SaleLine> carritoData = FXCollections.observableArrayList();

    private final PauseTransition notifAutoHide = new PauseTransition(Duration.seconds(3));

    private static final DecimalFormat PRICE_FMT;
    private static final DecimalFormat PRICE_FMT_GROUPED;
    static {
        // Sin miles (para almacenamiento / normalización), 2 decimales
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.US);
        PRICE_FMT = new DecimalFormat("0.00", s);
        PRICE_FMT.setGroupingUsed(false);

        // Con miles (para pantalla), 2 decimales
        DecimalFormatSymbols s2 = new DecimalFormatSymbols(Locale.US);
        PRICE_FMT_GROUPED = new DecimalFormat("#,##0.00", s2);
        PRICE_FMT_GROUPED.setGroupingUsed(true);
    }

    private String currentQuery = "";
    private boolean initializing = false;

    /* ===================== ViewModel de carrito ===================== */
    public static final class SaleLine {
        public final String productId;
        public final SimpleStringProperty nombre = new SimpleStringProperty("");
        public final SimpleIntegerProperty cantidad = new SimpleIntegerProperty(1);
        public final SimpleDoubleProperty precioUnitario = new SimpleDoubleProperty(0.0);

        public SaleLine(String productId, String nombre, int cantidad, double precioUnitario) {
            this.productId = productId;
            this.nombre.set(nombre);
            this.cantidad.set(cantidad);
            this.precioUnitario.set(precioUnitario);
        }

        public double getTotal() {
            return cantidad.get() * precioUnitario.get();
        }
    }

    /* ===================== Ciclo de vida ===================== */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializing = true;

        // Repositorios
        ProductRepository.init();
        ProductRepository.reload();
        // Quitar estas dos si las agregamos:
        // SalesRepository.init();
        // SalesRepository.loadToday();

        // Notificación autohide
        notifAutoHide.setOnFinished(e -> notificacionLabel.setText(" "));

        // Tablas
        setupInventarioTable();
        setupCarritoTable();

        // Inventario + foco
        loadInventario("");
        barraBusquedaTextField.requestFocus();

        // Pago
        elegirMetodoPagoComboBox.setItems(FXCollections.observableArrayList(
                "💵 Efectivo", "💳 Tarjeta", "📨 Transferencia"
        ));
        elegirMetodoPagoComboBox.valueProperty().addListener((o, a, b) -> onMetodoPagoChanged(b));

        // Máscara pago
        setupPagoMask();

        // Botones / atajos
        bindSearchButtonToTextfield();
        setupButtonsAndShortcuts();

        // Estado inicial
        goInitialState();
        updateTotalsAndButtons();

        initializing = false;

        // Usa tu utilidad real de telemetría
        LogUtils.audit("pos_opened");
    }

    /* ===================== Setup tablas ===================== */

    private void setupInventarioTable() {
        productosTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Mapear por encabezado (tolerante a espacios y mayúsculas)
        for (TableColumn<?, ?> c : productosTableView.getColumns()) {
            String h = (c.getText() == null) ? "" : c.getText().trim().toLowerCase(Locale.ROOT);

            if (h.equals("nombre")) {
                @SuppressWarnings("unchecked") TableColumn<ProductRecord, String> col = (TableColumn<ProductRecord, String>) c;
                col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombre));
                col.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.35));
            } else if (h.equals("precio")) {
                @SuppressWarnings("unchecked") TableColumn<ProductRecord, String> col = (TableColumn<ProductRecord, String>) c;
                col.setCellValueFactory(cell -> new SimpleStringProperty("$ " + PRICE_FMT_GROUPED.format(cell.getValue().precio)));
                col.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.15));
            } else if (h.equals("cantidad")) {
                @SuppressWarnings("unchecked") TableColumn<ProductRecord, Number> col = (TableColumn<ProductRecord, Number>) c;
                col.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().cantidad));
                col.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.12));
            } else if (h.equals("id")) {
                @SuppressWarnings("unchecked") TableColumn<ProductRecord, String> col = (TableColumn<ProductRecord, String>) c;
                col.setCellValueFactory(new PropertyValueFactory<>("id"));
                col.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.38));
            }
        }

        // Doble clic → agregar
        productosTableView.setOnMouseClicked(ev -> {
            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY && ev.getClickCount() == 2) {
                agregarProductoVenta(null);
            }
        });

        // Selección → habilitar botones
        productosTableView.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateTotalsAndButtons());
    }

    private void setupCarritoTable() {
        listaProductosVentaTableView.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        listaProductosVentaTableView.setItems(carritoData);

        // Configurar por encabezado (Nombre, Precio Individual, Cantidad, Precio Total)
        for (TableColumn<?, ?> c : listaProductosVentaTableView.getColumns()) {
            String h = (c.getText() == null) ? "" : c.getText().trim().toLowerCase(Locale.ROOT);

            if (h.equals("nombre")) {
                @SuppressWarnings("unchecked")
                TableColumn<SaleLine, String> col = (TableColumn<SaleLine, String>) c;
                col.setCellValueFactory(cell -> cell.getValue().nombre);
                col.prefWidthProperty().bind(
                        listaProductosVentaTableView.widthProperty().multiply(0.40));
            } else if (h.equals("precio individual")) {
                @SuppressWarnings("unchecked")
                TableColumn<SaleLine, String> col = (TableColumn<SaleLine, String>) c;
                // BINDING: se recalcula cuando cambia precioUnitario
                col.setCellValueFactory(cell ->
                        Bindings.createStringBinding(
                                () -> "$ " + PRICE_FMT_GROUPED.format(
                                        cell.getValue().precioUnitario.get()
                                ),
                                cell.getValue().precioUnitario
                        )
                );
                col.prefWidthProperty().bind(
                        listaProductosVentaTableView.widthProperty().multiply(0.18));
            } else if (h.equals("cantidad")) {
                @SuppressWarnings("unchecked")
                TableColumn<SaleLine, Number> col = (TableColumn<SaleLine, Number>) c;
                col.setCellValueFactory(cell -> cell.getValue().cantidad);
                col.prefWidthProperty().bind(
                        listaProductosVentaTableView.widthProperty().multiply(0.12));
            } else if (h.equals("precio total")) {
                @SuppressWarnings("unchecked")
                TableColumn<SaleLine, String> col = (TableColumn<SaleLine, String>) c;
                // BINDING: depende de cantidad y precioUnitario (reacciona a +1/-1)
                col.setCellValueFactory(cell ->
                        Bindings.createStringBinding(
                                () -> "$ " + PRICE_FMT_GROUPED.format(
                                        cell.getValue().cantidad.get()
                                                * cell.getValue().precioUnitario.get()
                                ),
                                cell.getValue().cantidad,
                                cell.getValue().precioUnitario
                        )
                );
                col.prefWidthProperty().bind(
                        listaProductosVentaTableView.widthProperty().multiply(0.30));
            }
        }

        // Doble clic → +1
        listaProductosVentaTableView.setOnMouseClicked(ev -> {
            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY && ev.getClickCount() == 2) {
                aumentar1CantidadProductoVenta(null);
            }
        });

        // Selección → habilitar botones
        listaProductosVentaTableView.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> updateTotalsAndButtons());
    }

    /* ===================== Setup inputs ===================== */

    private void setupPagoMask() {
        cantidadPagoVentaTextField.textProperty().addListener((o, old, val) -> {
            if (initializing) return;
            String clean = normalizeMoneyInput(val);
            if (!Objects.equals(val, clean)) {
                int caret = cantidadPagoVentaTextField.getCaretPosition();
                cantidadPagoVentaTextField.setText(clean);
                cantidadPagoVentaTextField.positionCaret(Math.min(caret, clean.length()));
                return;
            }
            updateTotalsAndButtons();
        });
    }

    // 6 enteros + '.' + 2 decimales, permite borrar, y normaliza ".,"
    private String normalizeMoneyInput(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace(',', '.');
        if (s.isEmpty()) return "";
        // Sólo dígitos y un punto
        StringBuilder sb = new StringBuilder();
        boolean dot = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
            else if (c == '.' && !dot) { sb.append('.'); dot = true; }
        }
        s = sb.toString();
        if (s.isEmpty()) return "";

        // Limitar a 6 enteros + 2 decimales
        int dotIdx = s.indexOf('.');
        if (dotIdx < 0) {
            if (s.length() > 6) s = s.substring(0, 6);
        } else {
            String ent = s.substring(0, Math.min(6, dotIdx));
            String dec = s.substring(dotIdx + 1);
            if (dec.length() > 2) dec = dec.substring(0, 2);
            s = ent + "." + dec;
        }
        return s;
    }

    /* ===================== Carga / búsqueda ===================== */

    private void loadInventario(String query) {
        currentQuery = (query == null) ? "" : query.trim();
        List<ProductRecord> list = ProductRepository.searchByName(currentQuery);
        // Orden alfabético por nombre (ya lo hace search, pero reforzamos)
        inventarioData.setAll(list);
        productosTableView.setItems(inventarioData);
    }

    private List<SaleLine> filterCarrito(String query) {
        String s = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return new ArrayList<>(carritoData);
        return carritoData.stream()
                .filter(l -> l != null && l.nombre.get() != null
                        && l.nombre.get().toLowerCase(Locale.ROOT).contains(s))
                .collect(Collectors.toList());
    }

    /* ===================== Estados/UX ===================== */

    private void goInitialState() {
        productosTableView.getSelectionModel().clearSelection();
        listaProductosVentaTableView.getSelectionModel().clearSelection();

        carritoData.clear();

        cantidadTotalVentaTextField.setText("$ 0.00");
        cantidadPagoVentaTextField.setText("");
        cantidadCambioVentaTextField.setText("$ 0.00");

        // Método de pago: lo mantenemos entre ventas (tu requerimiento),
        // así que no lo limpiamos aquí si ya tiene valor.

        updateTotalsAndButtons();
        setInfo(" ");
    }

    private void updateTotalsAndButtons() {
        // --- Totales ---
        BigDecimal total = computeCarritoTotal();
        cantidadTotalVentaTextField.setText("$ " + PRICE_FMT_GROUPED.format(total));

        String metodo = elegirMetodoPagoComboBox.getValue();
        boolean efectivo = "💵 Efectivo".equals(metodo);

        // Pago
        BigDecimal pago = BigDecimal.ZERO;
        if (efectivo) {
            String raw = cantidadPagoVentaTextField.getText();
            if (raw != null && !raw.isBlank()) {
                try {
                    pago = new BigDecimal(raw.replace(',', '.'));
                } catch (Exception ignored) { pago = BigDecimal.ZERO; }
            }
            cantidadPagoVentaTextField.setDisable(false);
        } else if (metodo != null) {
            // Tarjeta / Transferencia
            pago = total;
            cantidadPagoVentaTextField.setText(PRICE_FMT.format(pago));
            cantidadPagoVentaTextField.setDisable(true);
        } else {
            // Sin método aún
            cantidadPagoVentaTextField.setDisable(true);
            cantidadPagoVentaTextField.setText("");
        }

        // Cambio = pago - total (no negativo para presentación)
        BigDecimal cambio = pago.subtract(total);
        if (cambio.compareTo(BigDecimal.ZERO) < 0) {
            cantidadCambioVentaTextField.setText("$ 0.00");
        } else {
            cantidadCambioVentaTextField.setText("$ " + PRICE_FMT_GROUPED.format(cambio));
        }

        // --- Habilitación de botones ---
        boolean prodSel = productosTableView.getSelectionModel().getSelectedItem() != null;
        boolean lineaSel = listaProductosVentaTableView.getSelectionModel().getSelectedItem() != null;
        boolean carritoNoVacio = !carritoData.isEmpty();

        agregarProductoVentaButton.setDisable(!prodSel);
        cancelarSeleccionProductosButton.setDisable(!prodSel);

        eliminarProductoVentaButton.setDisable(!lineaSel);
        cancelarSeleccionVentaButton.setDisable(!lineaSel);

        // +1 habilitado si hay selección y no excede stock
        boolean plusEnabled = false;
        if (lineaSel) {
            SaleLine l = listaProductosVentaTableView.getSelectionModel().getSelectedItem();
            int stockMax = getStockForProduct(l.productId);
            plusEnabled = l.cantidad.get() < stockMax;
        }
        aumentar1CantidadProductoVentaButton.setDisable(!plusEnabled);

        // -1 habilitado si hay selección y cantidad > 0 (al llegar a 0 elimina)
        boolean minusEnabled = false;
        if (lineaSel) {
            SaleLine l = listaProductosVentaTableView.getSelectionModel().getSelectedItem();
            minusEnabled = l.cantidad.get() > 0;
        }
        reducir1CantidadProductoVentaButton.setDisable(!minusEnabled);

        // Registrar venta: condiciones
        boolean metodoOk = metodo != null;
        boolean pagoOk = !efectivo || pago.compareTo(total) >= 0;
        registrarVentaButton.setDisable(!(carritoNoVacio && metodoOk && pagoOk));

        // Notificación si falta pago en efectivo
        if (efectivo && metodoOk && carritoNoVacio) {
            if (pago.compareTo(total) < 0) {
                setError("Pago insuficiente. Falta $" + PRICE_FMT_GROUPED.format(total.subtract(pago)));
            } else {
                clearNotif();
            }
        }
    }

    private BigDecimal computeCarritoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleLine l : carritoData) {
            if (l == null) continue;
            BigDecimal p = BigDecimal.valueOf(l.precioUnitario.get());
            BigDecimal c = BigDecimal.valueOf(l.cantidad.get());
            BigDecimal line = p.multiply(c).setScale(2, RoundingMode.HALF_UP);
            total = total.add(line);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int getStockForProduct(String productId) {
        if (productId == null) return 0;
        for (ProductRecord p : ProductRepository.getAll()) {
            if (p != null && productId.equals(p.id)) return p.cantidad;
        }
        return 0;
    }

    /* ===================== Notificaciones ===================== */
    private void setSuccess(String msg) {
        notificacionLabel.setText(msg);
        notificacionLabel.getStyleClass().removeAll("notif-error", "notif-info");
        if (!notificacionLabel.getStyleClass().contains("notif-success"))
            notificacionLabel.getStyleClass().add("notif-success");
        notifAutoHide.playFromStart();
    }
    private void setError(String msg) {
        notificacionLabel.setText(msg);
        notificacionLabel.getStyleClass().removeAll("notif-success", "notif-info");
        if (!notificacionLabel.getStyleClass().contains("notif-error"))
            notificacionLabel.getStyleClass().add("notif-error");
        // No auto-ocultar errores para que el usuario los vea hasta corregir
    }
    private void setInfo(String msg) {
        notificacionLabel.setText(msg);
        notificacionLabel.getStyleClass().removeAll("notif-success", "notif-error");
        if (!notificacionLabel.getStyleClass().contains("notif-info"))
            notificacionLabel.getStyleClass().add("notif-info");
        notifAutoHide.playFromStart();
    }
    private void clearNotif() {
        notificacionLabel.setText(" ");
        notifAutoHide.stop();
    }

    /* ===================== Atajos y botones ===================== */

    private void setupButtonsAndShortcuts() {
        // ENTER comportamiento contextual
        PanelAdminOpcion2AnchorPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (barraBusquedaTextField.isFocused()) {
                    buscarProducto(null);
                } else if (productosTableView.isFocused()) {
                    if (!agregarProductoVentaButton.isDisable()) agregarProductoVenta(null);
                } else if (!registrarVentaButton.isDisable()) {
                    registrarVenta(null);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.DELETE) {
                if (!eliminarProductoVentaButton.isDisable()) eliminarProductoVenta(null);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                if (!cancelarSeleccionVentaButton.isDisable()) cancelarSeleccionVenta(null);
                else if (!cancelarSeleccionProductosButton.isDisable()) cancelarSeleccionProductos(null);
                e.consume();
            }
        });

        // Buscar botón
        buscarProductoButton.setOnAction(this::buscarProducto);
    }

    /* ===================== Handlers ===================== */

    @FXML
    void agregarProductoVenta(ActionEvent event) {
        ProductRecord p = productosTableView.getSelectionModel().getSelectedItem();
        if (p == null) return;

        // Si stock < 1 → no agregar (sin notificación)
        if (p.cantidad < 1) {
            productosTableView.getSelectionModel().clearSelection();
            updateTotalsAndButtons();
            return;
        }

        // Buscar si ya está en carrito → mergear
        for (SaleLine l : carritoData) {
            if (l.productId.equals(p.id)) {
                int stockMax = p.cantidad;
                int nuevo = Math.min(stockMax, l.cantidad.get() + 1);
                l.cantidad.set(nuevo);
                listaProductosVentaTableView.getSelectionModel().select(l);
                listaProductosVentaTableView.scrollTo(l);
                productosTableView.getSelectionModel().clearSelection();
                updateTotalsAndButtons();
                return;
            }
        }

        // Agregar nueva línea
        SaleLine nl = new SaleLine(p.id, p.nombre, 1, p.precio);
        carritoData.add(nl);
        listaProductosVentaTableView.getSelectionModel().select(nl);
        listaProductosVentaTableView.scrollTo(nl);
        productosTableView.getSelectionModel().clearSelection();

        updateTotalsAndButtons();
        clearNotif();
    }

    @FXML
    void aumentar1CantidadProductoVenta(ActionEvent event) {
        SaleLine l = listaProductosVentaTableView.getSelectionModel().getSelectedItem();
        if (l == null) return;
        int stockMax = getStockForProduct(l.productId);
        if (l.cantidad.get() >= stockMax) {
            updateTotalsAndButtons();
            return;
        }
        l.cantidad.set(l.cantidad.get() + 1);
        updateTotalsAndButtons();
    }

    @FXML
    void reducir1CantidadProductoVenta(ActionEvent event) {
        SaleLine l = listaProductosVentaTableView.getSelectionModel().getSelectedItem();
        if (l == null) return;
        int nuevo = l.cantidad.get() - 1;
        if (nuevo <= 0) {
            carritoData.remove(l);
            listaProductosVentaTableView.getSelectionModel().clearSelection();
        } else {
            l.cantidad.set(nuevo);
        }
        updateTotalsAndButtons();
    }

    @FXML
    void eliminarProductoVenta(ActionEvent event) {
        SaleLine l = listaProductosVentaTableView.getSelectionModel().getSelectedItem();
        if (l == null) return;
        carritoData.remove(l);
        listaProductosVentaTableView.getSelectionModel().clearSelection();
        updateTotalsAndButtons();
    }

    @FXML
    void cancelarSeleccionProductos(ActionEvent event) {
        productosTableView.getSelectionModel().clearSelection();
        updateTotalsAndButtons();
    }

    @FXML
    void cancelarSeleccionVenta(ActionEvent event) {
        listaProductosVentaTableView.getSelectionModel().clearSelection();
        updateTotalsAndButtons();
    }

    @FXML
    void buscarProducto(ActionEvent event) {
        String q = barraBusquedaTextField.getText();
        loadInventario(q);

        // También filtra visualmente el carrito (sin modificarlo)
        List<SaleLine> filtered = filterCarrito(q);
        listaProductosVentaTableView.setItems(FXCollections.observableArrayList(filtered));
        // Para que los cambios de carrito real sigan reflejándose,
        // si la búsqueda está vacía volvemos a la lista "viva"
        if (q == null || q.isBlank()) {
            listaProductosVentaTableView.setItems(carritoData);
        }

        setInfo(filtered.isEmpty() && inventarioData.isEmpty() ? "Sin resultados." : " ");
    }

    @FXML
    void registrarVenta(ActionEvent event) {
        try {
            // === 1) Validaciones de seguridad ===
            if (carritoData == null || carritoData.isEmpty()) {
                notificacionLabel.setText("No hay productos en la venta.");
                return;
            }
            Object metodoSel = elegirMetodoPagoComboBox.getValue();
            if (metodoSel == null) {
                notificacionLabel.setText("Selecciona un método de pago.");
                return;
            }
            final String metodo = metodoSel.toString(); // 💵/💳/📨

            // === 2) Totales (recalcular por seguridad) ===
            BigDecimal totalBD = BigDecimal.ZERO;
            for (SaleLine line : carritoData) {
                totalBD = totalBD.add(BigDecimal.valueOf(line.getTotal()));
            }
            totalBD = totalBD.setScale(2, RoundingMode.HALF_UP);

            BigDecimal pagoBD;
            if (metodo.startsWith("💵")) {
                String raw = cantidadPagoVentaTextField.getText();
                if (raw == null || raw.isBlank()) {
                    notificacionLabel.setText("Indica el monto pagado en efectivo.");
                    return;
                }
                raw = raw.trim().replace(",", "");
                try {
                    pagoBD = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
                } catch (Exception ex) {
                    notificacionLabel.setText("Monto de pago inválido.");
                    return;
                }
                if (pagoBD.compareTo(totalBD) < 0) {
                    notificacionLabel.setText("El pago no puede ser menor al total.");
                    return;
                }
            } else {
                pagoBD = totalBD;
            }
            BigDecimal cambioBD = pagoBD.subtract(totalBD).setScale(2, RoundingMode.HALF_UP);

            // === 3) Construir items (snapshot) ===
            List<SalesRepository.SaleItem> items = new ArrayList<>();
            for (SaleLine line : carritoData) {
                SalesRepository.SaleItem it = new SalesRepository.SaleItem();
                it.productId = line.productId;
                it.nombreSnapshot = line.nombre.get();
                it.precioUnitario = line.precioUnitario.get();
                it.cantidad = line.cantidad.get();
                it.totalLinea = line.getTotal();
                items.add(it);
            }

            // === 4) Descontar inventario ===
            boolean inventoryApplied = true;

            // Toma una copia del inventario actual
            List<ProductRecord> productos = new ArrayList<>(ProductRepository.getAll());

            // Índice por id para modificar rápido
            Map<String, ProductRecord> idx = new HashMap<>();
            for (ProductRecord pr : productos) {
                idx.put(pr.getId(), pr);
            }

            // Aplica las bajas
            Set<String> cambiados = new HashSet<>();
            for (SalesRepository.SaleItem it : items) {
                ProductRecord pr = idx.get(it.productId);
                if (pr != null) {
                    int nuevo = Math.max(0, pr.getCantidad() - it.cantidad);
                    pr.setCantidad(nuevo);
                    cambiados.add(pr.getId());
                }
            }

            try {
                // Persiste únicamente los modificados (menos I/O)
                for (String id : cambiados) {
                    ProductRecord pr = idx.get(id);
                    if (pr != null) ProductRepository.update(pr);
                }

                // refrescar tabla inventario (feedback inmediato)
                productosTableView.getItems().setAll(ProductRepository.getAll());
                productosTableView.refresh();

                // Auditorías simples
                LogUtils.audit("products_saved");
                LogUtils.audit("products_reloaded");
            } catch (Exception ex) {
                inventoryApplied = false; // guardamos la venta pero marcamos el flag
            }

            // === 5) Registro de venta (folio global, archivo del día) ===
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String fecha = now.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String hora  = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            SalesRepository.SaleRecord sr = new SalesRepository.SaleRecord();
            sr.uuid = java.util.UUID.randomUUID().toString();
            sr.folio = 0; // se asigna en appendToday()
            sr.fecha = fecha;
            sr.hora = hora;
            sr.cashierUsername = SessionManager.getUsername();
            sr.metodoPago = metodo;
            sr.total = totalBD.doubleValue();
            sr.pago  = pagoBD.doubleValue();
            sr.cambio = cambioBD.doubleValue();
            sr.inventoryApplied = inventoryApplied;
            sr.items = items;

            SalesRepository.appendToday(sr);
            LogUtils.audit("sales_saved");
            LogUtils.audit("pos_payment_confirmed");

            // === 6) Feedback + limpieza (manteniendo método de pago) ===
            cantidadTotalVentaTextField.setText("$ " + PRICE_FMT_GROUPED.format(totalBD));
            if (metodo.startsWith("💵")) {
                cantidadCambioVentaTextField.setText("$ " + PRICE_FMT_GROUPED.format(cambioBD));
                cantidadPagoVentaTextField.clear();
            } else {
                cantidadCambioVentaTextField.setText("$ 0.00");
            }

            carritoData.clear();
            listaProductosVentaTableView.getSelectionModel().clearSelection();
            listaProductosVentaTableView.refresh();

            cantidadTotalVentaTextField.setText("$ 0.00");
            cantidadCambioVentaTextField.setText("$ 0.00");

            onMetodoPagoChanged(elegirMetodoPagoComboBox.getValue());
            notificacionLabel.setText("Venta registrada. Folio #" + sr.folio + " por $ " + PRICE_FMT_GROUPED.format(totalBD));
            updateTotalsAndButtons();

            // refrescar inventario (manteniendo filtro)
            refreshInventarioKeepingFilter();
        } catch (Exception ex) {
            ex.printStackTrace();
            notificacionLabel.setText("Error al registrar la venta: " + ex.getMessage());
        }
    }


    /* ===================== Helpers ===================== */

    private BigDecimal parseMoneySafe(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    // === Manejo de cambios de método de pago (ComboBox) ===
    private void onMetodoPagoChanged(String metodo) {
        boolean efectivo = "💵 Efectivo".equals(metodo);

        if (efectivo) {
            // Vuelves a permitir captura manual y limpias el campo (tu regla)
            cantidadPagoVentaTextField.setDisable(false);
            cantidadPagoVentaTextField.setText("");
        } else if (metodo != null) {
            // Tarjeta / Transferencia: pago = total y campo deshabilitado
            java.math.BigDecimal total = computeCarritoTotal();
            cantidadPagoVentaTextField.setDisable(true);
            cantidadPagoVentaTextField.setText(PRICE_FMT.format(total));
        } else {
            // Sin método elegido
            cantidadPagoVentaTextField.setDisable(true);
            cantidadPagoVentaTextField.setText("");
        }

        clearNotif();
        updateTotalsAndButtons();
    }
    
    // Refresca la tabla de inventario respetando el filtro actual de la barra de búsqueda
    private void refreshInventarioKeepingFilter() {
        String q = (barraBusquedaTextField.getText() == null) ? "" : barraBusquedaTextField.getText().trim();

        // Relee desde disco por si hubo cambios de stock
        costquito.globalMethods.ProductRepository.reload();

        // Rellena los datos (usa tu misma lista observable del inventario)
        java.util.List<costquito.globalMethods.ProductRecord> list =
                costquito.globalMethods.ProductRepository.searchByName(q);

        inventarioData.setAll(list);              // <-- tu ObservableList del inventario
        productosTableView.getSelectionModel().clearSelection();
        productosTableView.refresh();
    }
    
    private void bindSearchButtonToTextfield() {
        buscarProductoButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> {
                    String t = barraBusquedaTextField.getText();
                    return t == null || t.trim().isEmpty();
                },
                barraBusquedaTextField.textProperty()
            )
        );
    }

    /* ===================== Navegación y cierre ===================== */

    @FXML
    void cerrarPrograma(ActionEvent event) {
        AppUtils.cerrarPrograma();
    }

    @FXML
    void irAdministracion(ActionEvent event) {
        WindowUtils.navigate(Views.PANEL_ADMIN_OPCION1);
    }

    @FXML
    void irCuentas(ActionEvent event) {
        WindowUtils.navigate(Views.PANEL_ADMIN_OPCION4);
    }

    @FXML
    void irReportes(ActionEvent event) {
        WindowUtils.navigate(Views.PANEL_ADMIN_OPCION3);
    }

    @FXML
    void irVentas(ActionEvent event) {
        WindowUtils.navigate(Views.PANEL_ADMIN_OPCION2);
    }

    @FXML
    void regresarInicarSesion(ActionEvent event) {
        WindowUtils.navigate(Views.LOGIN);
    }
}
