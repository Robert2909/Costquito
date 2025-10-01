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

public class PanelAdminOpcion2Controller {
        
    @FXML
    private AnchorPane PanelAdminOpcion1AnchorPane;

    @FXML
    private Button administracionButton;

    @FXML
    private Pane barraSuperiorPane;

    @FXML
    private Button cerrarButton;

    @FXML
    private Pane contenidoPane;
    
    @FXML
    private Button cuentasButton;

    @FXML
    private ImageView logoImageView;

    @FXML
    private Pane logoPane;

    @FXML
    private Button regresarButton;

    @FXML
    private Button reportesButton;

    @FXML
    private Button ventasButton;

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
