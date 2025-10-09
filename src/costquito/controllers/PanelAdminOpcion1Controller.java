package costquito.controllers;

import costquito.globalMethods.AppUtils;
import costquito.globalMethods.LogUtils;
import costquito.globalMethods.ProductRecord;
import costquito.globalMethods.ProductRepository;
import costquito.globalMethods.Views;
import costquito.globalMethods.WindowUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class PanelAdminOpcion1Controller implements Initializable {

    @FXML private AnchorPane PanelAdminOpcion1AnchorPane;

    @FXML private Button administracionButton;
    @FXML private Button agregarProductoButton;
    @FXML private Button aumentar1productoButton;

    @FXML private TextField barraBusquedaTextField;
    @FXML private Pane barraSuperiorPane;
    @FXML private Button buscarProductoButton;

    @FXML private Button cancelarSeleccionButton;
    @FXML private Label cantidadProductoLabel;
    @FXML private TextField cantidadProductoTextField;
    @FXML private Button cerrarButton;
    @FXML private Pane contenidoPane;
    @FXML private Button cuentasButton;
    @FXML private Button editarProductoButton;
    @FXML private Button eliminarProductoButton;
    @FXML private ImageView logoImageView;
    @FXML private Pane logoPane;
    @FXML private Label nombreProductoLabel;
    @FXML private TextField nombreProductoTextField;
    @FXML private Label notificacionLabel;
    @FXML private Label precioProductoLabel;
    @FXML private TextField precioProductoTextField;
    @FXML private Pane productosPane;

    /** TableView y columnas definidas en FXML */
    @FXML private TableView<ProductRecord> productosTableView;
    @FXML private TableColumn<ProductRecord, String> nombreTableColumn;
    @FXML private TableColumn<ProductRecord, String> precioTableColumn;   // mostramos formateado
    @FXML private TableColumn<ProductRecord, Number> cantidadTableColumn;
    @FXML private TableColumn<ProductRecord, String> idTableColumn;

    @FXML private Button reducir1productoButton;
    @FXML private Button regresarButton;
    @FXML private Button reportesButton;
    @FXML private Button ventasButton;

    // ======== Estado ========
    private final ObservableList<ProductRecord> tableData = FXCollections.observableArrayList();
    private ProductRecord selected = null;
    private String originalNombre = null;
    private String originalPrecioStr = null;
    private String originalCantidadStr = null;
    private String currentQuery = "";

    private static final DecimalFormat PRICE_FMT;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.US);
        PRICE_FMT = new DecimalFormat("0.00", s);
        PRICE_FMT.setGroupingUsed(false);
    }

    // ======== Ciclo de vida ========
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ProductRepository.init();
        ProductRepository.reload();

        java.nio.file.Path p = java.nio.file.Paths.get("costquito.media", "productos.json");
        costquito.globalMethods.LogUtils.audit("productos_json_path",
                "abs", p.toAbsolutePath().toString(),
                "exists", java.nio.file.Files.exists(p));

        setupTable();              // columnas + clases CSS
        loadTable("");             // sin filtro al iniciar

        setupMasksAndValidation();
        setupSearchBar();
        setupButtonsAndShortcuts();
        goInitialState();
        LogUtils.audit("admin_inventario_opened");
    }

    private void setupTable() {
        // Usar todo el ancho del TableView
        productosTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ===== Enlazar columnas a propiedades y formato =====
        nombreTableColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombre));
        precioTableColumn.setCellValueFactory(cell -> new SimpleStringProperty(PRICE_FMT.format(cell.getValue().precio)));
        cantidadTableColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().cantidad));
        idTableColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        // ===== Anchos proporcionales =====
        // Nombre 35%, Precio 15%, Cantidad 12%, ID 38%
        nombreTableColumn.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.35));
        precioTableColumn.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.15));
        cantidadTableColumn.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.12));
        idTableColumn.prefWidthProperty().bind(productosTableView.widthProperty().multiply(0.38));

        // ===== Celda con elipsis para ID (evita desbordes) =====
        idTableColumn.setCellFactory(col -> new TableCell<ProductRecord, String>() {
            private final Label lbl = new Label();
            {
                lbl.getStyleClass().add("id-cell"); // estilo extra en Tables.css
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setTextOverrun(javafx.scene.control.OverrunStyle.CENTER_ELLIPSIS);
                lbl.setWrapText(false);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(item);
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        // ===== Clases CSS (TableView + Columnas) =====
        if (!productosTableView.getStyleClass().contains("tabla-productos")) {
            productosTableView.getStyleClass().add("tabla-productos");
        }
        addOnce(nombreTableColumn.getStyleClass(), "col-nombre");
        addOnce(precioTableColumn.getStyleClass(), "col-precio");
        addOnce(cantidadTableColumn.getStyleClass(), "col-cantidad");
        addOnce(idTableColumn.getStyleClass(), "col-id");

        // ===== Datos y selección =====
        productosTableView.setItems(tableData);
        productosTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> onRowSelected(val));
    }


    private static void addOnce(java.util.List<String> list, String cls) {
        if (!list.contains(cls)) list.add(cls);
    }

    private void loadTable(String query) {
        currentQuery = query == null ? "" : query.trim();
        tableData.setAll(ProductRepository.searchByName(currentQuery));
        // si la selección actual no está en el filtro, limpiar
        if (selected != null && tableData.stream().noneMatch(p -> p.id.equals(selected.id))) {
            goInitialState();
        }
    }

    // ======== Máscaras/validación ========
    private void setupMasksAndValidation() {
        nombreProductoTextField.textProperty().addListener((o, a, b) -> updateButtonsState());

        precioProductoTextField.textProperty().addListener((o, a, b) -> {
            updateButtonsState();
        });

        cantidadProductoTextField.textProperty().addListener((o, a, b) -> {
            String clean = b.replaceAll("[^0-9]", "");
            if (!b.equals(clean)) cantidadProductoTextField.setText(clean);
            updateButtonsState();
        });
    }

    private boolean isNombreValid() {
        String n = nombreProductoTextField.getText() == null ? "" : nombreProductoTextField.getText().trim();
        return ProductRepository.isValidNombre(n);
    }

    private boolean isPrecioValid() {
        Double p = parsePrecio(precioProductoTextField.getText());
        return p != null && ProductRepository.isValidPrecio(p);
    }

    private boolean isCantidadValid() {
        Integer c = parseCantidad(cantidadProductoTextField.getText());
        return c != null && ProductRepository.isValidCantidad(c);
    }

    private Double parsePrecio(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        s = s.replace("$", "").replace(" ", "");
        if (s.indexOf(',') >= 0 && s.indexOf('.') < 0) s = s.replace(',', '.');
        try {
            double v = Double.parseDouble(s);
            if (!ProductRepository.isValidPrecio(v)) return null;
            String norm = PRICE_FMT.format(v);
            return Double.parseDouble(norm);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseCantidad(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            int v = Integer.parseInt(s);
            if (!ProductRepository.isValidCantidad(v)) return null;
            return v;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ======== Estados ========
    private void goInitialState() {
        productosTableView.getSelectionModel().clearSelection();
        selected = null;
        originalNombre = originalPrecioStr = originalCantidadStr = null;

        nombreProductoTextField.clear();
        precioProductoTextField.clear();
        cantidadProductoTextField.clear();

        agregarProductoButton.setDisable(true);
        editarProductoButton.setDisable(true);
        eliminarProductoButton.setDisable(true);
        cancelarSeleccionButton.setDisable(true);
        reducir1productoButton.setDisable(true);
        aumentar1productoButton.setDisable(true);

        setInfo(" ");
    }

    private void onRowSelected(ProductRecord p) {
        selected = p;
        if (p == null) {
            goInitialState();
            return;
        }
        nombreProductoTextField.setText(p.nombre);
        precioProductoTextField.setText(PRICE_FMT.format(p.precio));
        cantidadProductoTextField.setText(String.valueOf(p.cantidad));

        originalNombre = nombreProductoTextField.getText();
        originalPrecioStr = precioProductoTextField.getText();
        originalCantidadStr = cantidadProductoTextField.getText();

        agregarProductoButton.setDisable(true);
        eliminarProductoButton.setDisable(false);
        cancelarSeleccionButton.setDisable(false);
        reducir1productoButton.setDisable(false);
        aumentar1productoButton.setDisable(false);

        updateButtonsState();
    }

    private boolean hasChangesAgainstOriginal() {
        if (selected == null) return false;
        String n = (nombreProductoTextField.getText() == null ? "" : nombreProductoTextField.getText().trim());
        String p = normalizePrecioText(precioProductoTextField.getText());
        String c = cantidadProductoTextField.getText() == null ? "" : cantidadProductoTextField.getText().trim();
        return !(eq(n, originalNombre != null ? originalNombre.trim() : "")
                && eq(p, normalizePrecioText(originalPrecioStr))
                && eq(c, (originalCantidadStr != null ? originalCantidadStr.trim() : "")));
    }

    private String normalizePrecioText(String text) {
        Double v = parsePrecio(text);
        return v == null ? "" : PRICE_FMT.format(v);
    }

    private boolean eq(String a, String b) { return (a == null ? "" : a).equals(b == null ? "" : b); }

    private void updateButtonsState() {
        boolean nameOk = isNombreValid();
        boolean priceOk = isPrecioValid();
        boolean qtyOk = isCantidadValid();

        if (selected == null) {
            agregarProductoButton.setDisable(!(nameOk && priceOk && qtyOk));
            editarProductoButton.setDisable(true);
            eliminarProductoButton.setDisable(true);
            cancelarSeleccionButton.setDisable(true);
            reducir1productoButton.setDisable(!qtyOk);
            aumentar1productoButton.setDisable(!qtyOk);
        } else {
            boolean changed = hasChangesAgainstOriginal();
            editarProductoButton.setDisable(!(nameOk && priceOk && qtyOk && changed));
        }
    }

    // ======== Búsqueda ========
    private void setupSearchBar() {
        // Enter en la barra → buscar
        barraBusquedaTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                buscarProducto(null);
                e.consume();
            }
        });
        // Si se borra el texto, mostrar todo
        barraBusquedaTextField.textProperty().addListener((o, a, b) -> {
            if (b == null || b.isBlank()) {
                loadTable("");
                setInfo(" ");
            }
        });
    }

    @FXML
    void buscarProducto(ActionEvent event) {
        String q = barraBusquedaTextField.getText();
        loadTable(q);
        setInfo(" ");
        LogUtils.info("buscar_producto", "q", q == null ? "" : q.trim(), "count", tableData.size());
        if (tableData.isEmpty()) {
            setInfo("Sin resultados.");
        }
    }

    // ======== Acciones CRUD ========
    @FXML
    void agregarProducto(ActionEvent event) {
        String nombre = nombreProductoTextField.getText() == null ? "" : nombreProductoTextField.getText().trim();
        Double precio = parsePrecio(precioProductoTextField.getText());
        Integer cant  = parseCantidad(cantidadProductoTextField.getText());

        if (!ProductRepository.isValidNombre(nombre) || precio == null || cant == null) {
            setError("Revise los datos.");
            LogUtils.warn("producto_add_validacion_fallida", "nombre", nombre);
            return;
        }

        try {
            ProductRecord p = new ProductRecord(nombre, precio, cant);
            ProductRepository.add(p);
            LogUtils.audit("producto_agregado", "id", p.id, "nombre", p.nombre);

            // Respetar filtro actual y seleccionar el nuevo si visible
            loadTable(currentQuery);
            productosTableView.getSelectionModel().select(
                    tableData.stream().filter(x -> x.id.equals(p.id)).findFirst().orElse(null)
            );
            productosTableView.scrollTo(productosTableView.getSelectionModel().getSelectedIndex());
            setSuccess("Producto agregado correctamente.");
        } catch (IllegalArgumentException ex) {
            setError("Nombre de producto duplicado.");
            LogUtils.warn("producto_add_nombre_duplicado", "nombre", nombre);
        } catch (Exception ex) {
            setError("No se pudo guardar el producto.");
            LogUtils.error("producto_add_error_io", ex);
        }
    }

    @FXML
    void editarProducto(ActionEvent event) {
        if (selected == null) return;

        String nombre = nombreProductoTextField.getText() == null ? "" : nombreProductoTextField.getText().trim();
        Double precio = parsePrecio(precioProductoTextField.getText());
        Integer cant  = parseCantidad(cantidadProductoTextField.getText());

        if (!ProductRepository.isValidNombre(nombre) || precio == null || cant == null) {
            setError("Revise los datos.");
            LogUtils.warn("producto_edit_validacion_fallida", "id", selected.id);
            return;
        }

        try {
            ProductRecord upd = new ProductRecord();
            upd.id = selected.id;
            upd.nombre = nombre;
            upd.precio = precio;
            upd.cantidad = cant;

            ProductRepository.update(upd);
            LogUtils.audit("producto_editado", "id", upd.id, "nombre", upd.nombre);

            loadTable(currentQuery);
            goInitialState();
            setSuccess("Cambios guardados.");
        } catch (IllegalArgumentException ex) {
            setError("Nombre de producto duplicado.");
            LogUtils.warn("producto_edit_nombre_duplicado", "id", selected.id, "nombre", nombre);
        } catch (Exception ex) {
            setError("No se pudo guardar el cambio.");
            LogUtils.error("producto_edit_error_io", ex);
        }
    }

    @FXML
    void eliminarProducto(ActionEvent event) {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmación");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar ‘" + selected.nombre + "’ definitivamente?");
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.OK)).setText("Eliminar");
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            ProductRepository.deleteById(selected.id);
            LogUtils.audit("producto_eliminado", "id", selected.id, "nombre", selected.nombre);
            loadTable(currentQuery);
            goInitialState();
            setSuccess("Producto eliminado.");
        } catch (Exception ex) {
            setError("No se pudo eliminar.");
            LogUtils.error("producto_delete_error_io", ex);
        }
    }

    @FXML
    void cancelarSeleccion(ActionEvent event) { goInitialState(); }

    @FXML
    void aumentar1Producto(ActionEvent event) {
        Integer c = parseCantidad(cantidadProductoTextField.getText());
        if (c == null) c = 0;
        c = Math.min(100000, c + 1);
        cantidadProductoTextField.setText(String.valueOf(c));
    }

    @FXML
    void reducir1Producto(ActionEvent event) {
        Integer c = parseCantidad(cantidadProductoTextField.getText());
        if (c == null) c = 0;
        c = Math.max(0, c - 1);
        cantidadProductoTextField.setText(String.valueOf(c));
    }

    // ======== Navegación / cierre ========
    @FXML
    void cerrarPrograma(ActionEvent event) { AppUtils.cerrarPrograma(); }
    @FXML
    void irAdministracion(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION1); }
    @FXML
    void irCuentas(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION4); }
    @FXML
    void irReportes(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION3); }
    @FXML
    void irVentas(ActionEvent event) { WindowUtils.navigate(Views.PANEL_ADMIN_OPCION2); }
    @FXML
    void regresarInicarSesion(ActionEvent event) { WindowUtils.navigate(Views.LOGIN); }

    // ======== Shortcuts ========
    private void setupButtonsAndShortcuts() {
        PanelAdminOpcion1AnchorPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                // si el foco está en la barra de búsqueda, la maneja setupSearchBar()
                if (barraBusquedaTextField.isFocused()) return;
                if (selected == null) {
                    if (!agregarProductoButton.isDisable()) agregarProducto(null);
                } else {
                    if (!editarProductoButton.isDisable()) editarProducto(null);
                }
                e.consume();
            }
        });
    }

    // ======== Notificaciones ========
    private void setSuccess(String msg) {
        notificacionLabel.setText(msg);
        notificacionLabel.getStyleClass().removeAll("notif-error", "notif-info");
        if (!notificacionLabel.getStyleClass().contains("notif-success"))
            notificacionLabel.getStyleClass().add("notif-success");
    }

    private void setError(String msg) {
        notificacionLabel.setText(msg);
        notificacionLabel.getStyleClass().removeAll("notif-success", "notif-info");
        if (!notificacionLabel.getStyleClass().contains("notif-error"))
            notificacionLabel.getStyleClass().add("notif-error");
    }

    private void setInfo(String msg) {
        notificacionLabel.setText(msg);
        notificacionLabel.getStyleClass().removeAll("notif-success", "notif-error");
        if (!notificacionLabel.getStyleClass().contains("notif-info"))
            notificacionLabel.getStyleClass().add("notif-info");
    }
}
