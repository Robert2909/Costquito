package costquito.controllers;

import costquito.globalMethods.AppUtils;
import costquito.globalMethods.Views;
import costquito.globalMethods.WindowUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class PanelVendedorOpcion1Controller {

    @FXML
    private AnchorPane PanelVendedorOpcion1AnchorPane;

    @FXML
    private Pane barraSuperiorPane;

    @FXML
    private Button cerrarButton;

    @FXML
    private Pane contenidoPane;

    @FXML
    private ImageView logoImageView;

    @FXML
    private Pane logoPane;

    @FXML
    private Button regresarButton;

    @FXML
    private Button ventasButton;

    @FXML
    void cerrarPrograma(ActionEvent event) {
        AppUtils.cerrarPrograma();
    }

    @FXML
    void irVentas(ActionEvent event) {
        WindowUtils.navigate(Views.PANEL_VENDEDOR_OPCION1);
    }

    @FXML
    void regresarInicarSesion(ActionEvent event) {
        WindowUtils.navigate(Views.LOGIN);
    }

}
