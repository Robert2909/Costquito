package costquito.controllers;

import costquito.globalMethods.AppUtils;
import costquito.globalMethods.Views;
import costquito.globalMethods.WindowUtils;
import costquito.globalMethods.SalesRepository;
import costquito.globalMethods.SalesRepository.SaleRecord;
import costquito.globalMethods.SalesRepository.SaleItem;
import costquito.globalMethods.UserRecord;
import costquito.globalMethods.UserRepository;
import costquito.globalMethods.ExportServiceExcel;
import costquito.globalMethods.LogUtils;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PanelAdminOpcion3Controller {

    // ---------- UI ----------
    @FXML private AnchorPane PanelAdminOpcion3AnchorPane;
    @FXML private Button administracionButton;
    @FXML private TextField barraBusquedaTextField;
    @FXML private Pane barraSuperiorPane;
    @FXML private Button buscarVentaButton;
    @FXML private TableColumn<?, ?> cantidadTableColumn;
    @FXML private Button cerrarButton;
    @FXML private Pane contenidoPane;
    @FXML private Button cuentasButton;
    @FXML private Label detallesVentaLabel;
    @FXML private DatePicker fechaDatePicker;
    @FXML private Label fechaLabel;
    @FXML private TableColumn<?, ?> folioVentaTableColumn;
    @FXML private Label horaVentaLabel;
    @FXML private TableColumn<?, ?> horaVentaTableColumn;
    @FXML private TextField horaVentaTextField;
    @FXML private Label listaProductosVentaLabel;
    @FXML private Pane listaProductosVentaPane;
    @FXML private TableView<?> listaProductosVentaTableView; // cast en runtime
    @FXML private Label listaVentasLabel;
    @FXML private Pane listaVentasPane;
    @FXML private TableView<?> listaVentasTableView; // cast en runtime
    @FXML private ImageView logoImageView;
    @FXML private Pane logoPane;
    @FXML private Label metodoPagoVentaLabel;
    @FXML private TableColumn<?, ?> metodoPagoVentaTableColumn;
    @FXML private TextField metodoPagoVentaTextField;
    @FXML private Label montoCambioVentaLabel;
    @FXML private TableColumn<?, ?> montoCambioVentaTableColumn;
    @FXML private TextField montoCambioVentaTextField;
    @FXML private Label montoPagoVentaLabel;
    @FXML private TableColumn<?, ?> montoPagoVentaTableColumn;
    @FXML private TextField montoPagoVentaTextField;
    @FXML private Label montoTotalVentaLabel;
    @FXML private TableColumn<?, ?> montoTotalVentaTableColumn;
    @FXML private TextField montoTotalVentaTextField;
    @FXML private TableColumn<?, ?> nombreTableColumn;
    @FXML private Label notificacionLabel;
    @FXML private Button obtenerVentasDiaButton; // Botón "Exportar ventas del día"
    @FXML private TableColumn<?, ?> precioIndividualTableColumn;
    @FXML private TableColumn<?, ?> precioTotalTableColumn;
    @FXML private Button regresarButton;
    @FXML private Button reportesButton;
    @FXML private Label vendedorVentaLabel;
    @FXML private TableColumn<?, ?> vendedorVentaTableColumn;
    @FXML private TextField vendedorVentaTextField;
    @FXML private Button ventasButton;

    // ---------- Modelo en memoria ----------
    private final ObservableList<VentaRow> ventas = FXCollections.observableArrayList();
    private final ObservableList<ItemRow> items = FXCollections.observableArrayList();

    // Casts seguros para las tablas generics
    @SuppressWarnings("unchecked")
    private TableView<VentaRow> ventasTable() {
        return (TableView<VentaRow>) (TableView<?>) listaVentasTableView;
    }
    @SuppressWarnings("unchecked")
    private TableView<ItemRow> itemsTable() {
        return (TableView<ItemRow>) (TableView<?>) listaProductosVentaTableView;
    }

    // Formato de dinero $10,000.00
    private static final DecimalFormat MONEY = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.US);
    static { MONEY.applyPattern("$#,##0.00"); }

    private static final DateTimeFormatter FILE_NAME_FMT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private final BooleanProperty exportando = new SimpleBooleanProperty(false);

    // ---------- Inicialización ----------
    @FXML
    private void initialize() {
        // Placeholders
        ventasTable().setPlaceholder(new Label("Sin ventas para la fecha seleccionada"));
        itemsTable().setPlaceholder(new Label("Tabla sin contenido"));

        // Enlazar columnas de VENTAS
        ((TableColumn<VentaRow, Number>) folioVentaTableColumn).setCellValueFactory(d -> d.getValue().folioProperty());
        ((TableColumn<VentaRow, String>) horaVentaTableColumn).setCellValueFactory(d -> d.getValue().horaProperty());
        ((TableColumn<VentaRow, String>) vendedorVentaTableColumn).setCellValueFactory(d -> d.getValue().vendedorProperty());
        ((TableColumn<VentaRow, String>) metodoPagoVentaTableColumn).setCellValueFactory(d -> d.getValue().metodoProperty());
        ((TableColumn<VentaRow, Number>) montoTotalVentaTableColumn).setCellValueFactory(d -> d.getValue().montoTotalProperty());
        ((TableColumn<VentaRow, Number>) montoPagoVentaTableColumn).setCellValueFactory(d -> d.getValue().montoPagoProperty());
        ((TableColumn<VentaRow, Number>) montoCambioVentaTableColumn).setCellValueFactory(d -> d.getValue().montoCambioProperty());

        setMoneyCellFactory((TableColumn<VentaRow, Number>) montoTotalVentaTableColumn);
        setMoneyCellFactory((TableColumn<VentaRow, Number>) montoPagoVentaTableColumn);
        setMoneyCellFactory((TableColumn<VentaRow, Number>) montoCambioVentaTableColumn);

        // Enlazar columnas de ITEMS
        ((TableColumn<ItemRow, String>) nombreTableColumn).setCellValueFactory(d -> d.getValue().nombreProperty());
        ((TableColumn<ItemRow, Number>) precioIndividualTableColumn).setCellValueFactory(d -> d.getValue().precioUnitProperty());
        ((TableColumn<ItemRow, Number>) cantidadTableColumn).setCellValueFactory(d -> d.getValue().cantidadProperty());
        ((TableColumn<ItemRow, Number>) precioTotalTableColumn).setCellValueFactory(d -> d.getValue().importeProperty());
        
        // Las tablas siempre llenan su ancho
        ventasTable().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemsTable().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // ---- Porcentajes sugeridos (ventas) ----
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) folioVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.08));
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) horaVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.14));
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) vendedorVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.16));
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) metodoPagoVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.18));
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) montoTotalVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.14));
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) montoPagoVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.15));
        ((TableColumn<VentaRow, ?>) (TableColumn<?, ?>) montoCambioVentaTableColumn)
                .prefWidthProperty().bind(ventasTable().widthProperty().multiply(0.15));

        // ---- Porcentajes sugeridos (items) ----
        ((TableColumn<ItemRow, ?>) (TableColumn<?, ?>) nombreTableColumn)
                .prefWidthProperty().bind(itemsTable().widthProperty().multiply(0.46));
        ((TableColumn<ItemRow, ?>) (TableColumn<?, ?>) precioIndividualTableColumn)
                .prefWidthProperty().bind(itemsTable().widthProperty().multiply(0.18));
        ((TableColumn<ItemRow, ?>) (TableColumn<?, ?>) cantidadTableColumn)
                .prefWidthProperty().bind(itemsTable().widthProperty().multiply(0.18));
        ((TableColumn<ItemRow, ?>) (TableColumn<?, ?>) precioTotalTableColumn)
                .prefWidthProperty().bind(itemsTable().widthProperty().multiply(0.18));
        
        setStringTooltipCellFactory((TableColumn<VentaRow, String>) horaVentaTableColumn);
        setStringTooltipCellFactory((TableColumn<VentaRow, String>) vendedorVentaTableColumn);
        setStringTooltipCellFactory((TableColumn<VentaRow, String>) metodoPagoVentaTableColumn);
        setStringTooltipCellFactory((TableColumn<ItemRow, String>)  nombreTableColumn);

        setMoneyCellFactory((TableColumn<ItemRow, Number>) precioIndividualTableColumn);
        setMoneyCellFactory((TableColumn<ItemRow, Number>) precioTotalTableColumn);
        ((TableColumn<ItemRow, Number>) cantidadTableColumn).setCellFactory(col -> new TableCell<ItemRow, Number>() {
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format(Locale.US, "%.2f", value.doubleValue()));
            }
        });

        ventasTable().setItems(ventas);
        itemsTable().setItems(items);

        // Selección de venta → llena detalles e items (single-click)
        ventasTable().getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) fillDetail(sel); else clearDetail();
        });

        // Buscar: Enter o botón
        barraBusquedaTextField.setOnAction(e -> buscarActual());
        buscarVentaButton.setOnAction(this::buscarVenta);

        // DatePicker: valor inicial + recarga automática
        fechaDatePicker.setValue(LocalDate.now());
        fechaDatePicker.valueProperty().addListener((obs, old, d) -> cargarVentasDelDia(d));

        // Exportar deshabilitado si no hay filas
        obtenerVentasDiaButton.disableProperty().bind(
            Bindings.or(Bindings.isEmpty(ventas), exportando)
        );

        // Carga inicial (hoy)
        cargarVentasDelDia(LocalDate.now());

        // Label de notificación
        if (notificacionLabel != null) {
            notificacionLabel.setText("");
        }
    }

    // ---------- Navegación ----------
    @FXML private void cerrarPrograma(ActionEvent event) { AppUtils.cerrarPrograma(); }
    @FXML private void irAdministracion(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION1); }
    @FXML private void irCuentas(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION4); }
    @FXML private void irReportes(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION3); }
    @FXML private void irVentas(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION2); }
    @FXML private void regresarInicarSesion(ActionEvent event) { WindowUtils.navigate(Views.LOGIN); }

    // ---------- Buscar ----------
    @FXML
    void buscarVenta(ActionEvent event) {
        buscarActual();
    }

    private void buscarActual() {
        final String q = normalize(barraBusquedaTextField.getText());
        if (q.isBlank()) {
            ventasTable().setItems(ventas);
            autoSeleccionarPrimera();
            return;
        }
        final ObservableList<VentaRow> filtradas = ventas.stream()
                .filter(matchesVenta(q))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        ventasTable().setItems(filtradas);
        if (filtradas.isEmpty()) clearDetail(); else ventasTable().getSelectionModel().select(0);
    }

    private Predicate<VentaRow> matchesVenta(String q) {
        return v -> {
            if (contains(String.valueOf(v.getFolio()), q)) return true;
            if (contains(v.getHora(), q)) return true;
            if (contains(v.getVendedor(), q)) return true;
            if (contains(v.getMetodo(), q)) return true;
            if (contains(MONEY.format(v.getMontoTotal()), q)) return true;
            if (contains(MONEY.format(v.getMontoPago()), q)) return true;
            if (contains(MONEY.format(v.getMontoCambio()), q)) return true;

            for (ItemRow it : v.getItems()) {
                if (contains(it.getNombre(), q)) return true;
                if (contains(it.getProductId(), q)) return true;
                if (contains(String.format(Locale.US, "%.2f", it.getPrecioUnit()), q)) return true;
                if (contains(String.format(Locale.US, "%.2f", it.getCantidad()), q)) return true;
                if (contains(String.format(Locale.US, "%.2f", it.getImporte()), q)) return true;
            }
            return false;
        };
    }

    private boolean contains(String value, String q) {
        if (value == null) return false;
        return normalize(value).contains(q);
    }
    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    // ---------- Carga por fecha ----------
    private void cargarVentasDelDia(LocalDate date) {
        Task<List<SaleRecord>> task = new Task<>() {
            @Override protected List<SaleRecord> call() {
                return SalesRepository.loadByDate(date);
            }
        };
        task.setOnSucceeded(e -> {
            ventas.clear();
            List<SaleRecord> registros = task.getValue();
            if (registros != null) {
                for (SaleRecord r : registros) {
                    ventas.add(VentaRow.fromSaleRecord(r, resolveVendedor(r.cashierUsername)));
                }
                // Orden por folio DESC (última venta arriba)
                ventas.sort((a, b) -> Integer.compare(b.getFolio(), a.getFolio()));
            }
            ventasTable().setItems(ventas);
            autoSeleccionarPrimera();

            if (ventas.isEmpty()) {
                notificacionLabel.setText("Sin ventas para la fecha seleccionada");
            } else {
                notificacionLabel.setText(""); // limpia si había mensaje previo
            }
        });
        task.setOnFailed(e -> {
            ventas.clear();
            ventasTable().setItems(ventas);
            clearDetail();
            notificacionLabel.setText("No fue posible cargar las ventas del día seleccionado.");
        });
        new Thread(task, "load-ventas-dia").start();
    }

    private String resolveVendedor(String cashierUsername) {
        try {
            UserRecord u = UserRepository.findByUsername(cashierUsername);
            // Si no tienes getNombre(), cambia por el campo/metodo correcto o retorna tal cual:
            return (u != null && u.getUsername() != null && !u.getUsername().isBlank())
                    ? u.getUsername() : cashierUsername;
        } catch (Exception ex) {
            return cashierUsername;
        }
    }

    private void autoSeleccionarPrimera() {
        if (!ventasTable().getItems().isEmpty()) {
            ventasTable().getSelectionModel().select(0);
        } else {
            clearDetail();
        }
    }

    private void fillDetail(VentaRow v) {
        horaVentaTextField.setText(v.getHora());
        vendedorVentaTextField.setText(v.getVendedor());
        metodoPagoVentaTextField.setText(v.getMetodo());
        montoTotalVentaTextField.setText(MONEY.format(v.getMontoTotal()));
        montoPagoVentaTextField.setText(MONEY.format(v.getMontoPago()));
        montoCambioVentaTextField.setText(MONEY.format(v.getMontoCambio()));
        items.setAll(v.getItems());
        itemsTable().refresh();
    }

    private void clearDetail() {
        horaVentaTextField.clear();
        vendedorVentaTextField.clear();
        metodoPagoVentaTextField.clear();
        montoTotalVentaTextField.clear();
        montoPagoVentaTextField.clear();
        montoCambioVentaTextField.clear();
        items.clear();
    }

    // ---------- Exportación (botón "Exportar ventas del día") ----------
    @FXML
    void obtenerVentasDia(ActionEvent event) {
        LogUtils.audit("[REPORTES] Click en Exportar ventas del día");

        if (ventas.isEmpty()) {
            notificacionLabel.setText("No hay ventas para exportar.");
            return;
        }

        LocalDate d = (fechaDatePicker.getValue() != null) ? fechaDatePicker.getValue() : LocalDate.now();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte de ventas");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName("Reporte de ventas de Costquito " + d.format(FILE_NAME_FMT) + ".xlsx");

        var scene = obtenerVentasDiaButton.getScene();
        var owner = (scene != null) ? scene.getWindow() : null;

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            LogUtils.audit("[REPORTES] Exportación cancelada por el usuario");
            notificacionLabel.setText("");
            return;
        }

        List<VentaRow> toExport = List.copyOf(ventas);

        exportando.set(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ExportServiceExcel.exportVentasDia(file, d, toExport);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            exportando.set(false);
            LogUtils.audit("[REPORTES] Exportación OK -> " + file.getAbsolutePath());
            notificacionLabel.setText("Reporte exportado correctamente.");
        });
        task.setOnFailed(e -> {
            exportando.set(false);
            LogUtils.error("[REPORTES] Exportación FALLÓ.", task.getException());
            notificacionLabel.setText("No fue posible exportar el reporte.");
        });
        new Thread(task, "export-ventas-dia").start();
    }


    // ---------- Helpers ----------
    private static <T> void setMoneyCellFactory(TableColumn<T, Number> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String txt = MONEY.format(value.doubleValue());
                    setText(txt);
                    setTooltip(new Tooltip(txt));
                }
            }
        });
    }
    
    private static <S> void setStringTooltipCellFactory(TableColumn<S, String> col) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        });
    }

    // ---------- ViewModels ----------
    public static class VentaRow {
        private final IntegerProperty folio = new SimpleIntegerProperty();
        private final StringProperty hora = new SimpleStringProperty();
        private final StringProperty vendedor = new SimpleStringProperty();
        private final StringProperty metodo = new SimpleStringProperty();
        private final DoubleProperty montoTotal = new SimpleDoubleProperty();
        private final DoubleProperty montoPago = new SimpleDoubleProperty();
        private final DoubleProperty montoCambio = new SimpleDoubleProperty();
        private final ObservableList<ItemRow> items = FXCollections.observableArrayList();

        public static VentaRow fromSaleRecord(SaleRecord r, String vendedorMostrado) {
            VentaRow v = new VentaRow();
            v.setFolio(r.folio);
            v.setHora(r.hora);
            v.setVendedor((vendedorMostrado != null && !vendedorMostrado.isBlank())
                    ? vendedorMostrado : r.cashierUsername);
            v.setMetodo(r.metodoPago);
            v.setMontoTotal(r.total);
            v.setMontoPago(r.pago);
            v.setMontoCambio(r.cambio);
            if (r.items != null) {
                for (SaleItem it : r.items) {
                    v.items.add(ItemRow.from(
                            r.uuid,
                            it.productId,
                            it.nombreSnapshot,
                            it.precioUnitario,
                            it.cantidad,
                            it.totalLinea
                    ));
                }
            }
            return v;
        }

        public int getFolio() { return folio.get(); }
        public void setFolio(int v) { folio.set(v); }
        public IntegerProperty folioProperty() { return folio; }

        public String getHora() { return hora.get(); }
        public void setHora(String v) { hora.set(v); }
        public StringProperty horaProperty() { return hora; }

        public String getVendedor() { return vendedor.get(); }
        public void setVendedor(String v) { vendedor.set(v); }
        public StringProperty vendedorProperty() { return vendedor; }

        public String getMetodo() { return metodo.get(); }
        public void setMetodo(String v) { metodo.set(v); }
        public StringProperty metodoProperty() { return metodo; }

        public double getMontoTotal() { return montoTotal.get(); }
        public void setMontoTotal(double v) { montoTotal.set(v); }
        public DoubleProperty montoTotalProperty() { return montoTotal; }

        public double getMontoPago() { return montoPago.get(); }
        public void setMontoPago(double v) { montoPago.set(v); }
        public DoubleProperty montoPagoProperty() { return montoPago; }

        public double getMontoCambio() { return montoCambio.get(); }
        public void setMontoCambio(double v) { montoCambio.set(v); }
        public DoubleProperty montoCambioProperty() { return montoCambio; }

        public ObservableList<ItemRow> getItems() { return items; }
    }

    public static class ItemRow {
        private final StringProperty ventaUuid = new SimpleStringProperty();
        private final StringProperty productId = new SimpleStringProperty();
        private final StringProperty nombre = new SimpleStringProperty();
        private final DoubleProperty precioUnit = new SimpleDoubleProperty();
        private final DoubleProperty cantidad = new SimpleDoubleProperty();
        private final DoubleProperty importe = new SimpleDoubleProperty();

        public static ItemRow from(String ventaUuid, String productId, String nombre,
                                   double pu, double cant, double imp) {
            ItemRow r = new ItemRow();
            r.setVentaUuid(ventaUuid);
            r.setProductId(productId);
            r.setNombre(nombre);
            r.setPrecioUnit(pu);
            r.setCantidad(cant);
            r.setImporte(imp);
            return r;
        }

        public String getVentaUuid() { return ventaUuid.get(); }
        public void setVentaUuid(String v) { ventaUuid.set(v); }
        public StringProperty ventaUuidProperty() { return ventaUuid; }

        public String getProductId() { return productId.get(); }
        public void setProductId(String v) { productId.set(v); }
        public StringProperty productIdProperty() { return productId; }

        public String getNombre() { return nombre.get(); }
        public void setNombre(String v) { nombre.set(v); }
        public StringProperty nombreProperty() { return nombre; }

        public double getPrecioUnit() { return precioUnit.get(); }
        public void setPrecioUnit(double v) { precioUnit.set(v); }
        public DoubleProperty precioUnitProperty() { return precioUnit; }

        public double getCantidad() { return cantidad.get(); }
        public void setCantidad(double v) { cantidad.set(v); }
        public DoubleProperty cantidadProperty() { return cantidad; }

        public double getImporte() { return importe.get(); }
        public void setImporte(double v) { importe.set(v); }
        public DoubleProperty importeProperty() { return importe; }
    }
}
