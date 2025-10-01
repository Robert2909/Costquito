package costquito.controllers;

import costquito.globalMethods.AppUtils;
import costquito.globalMethods.LogUtils;
import costquito.globalMethods.SessionManager;
import costquito.globalMethods.UserRole;
import costquito.globalMethods.UserSession;
import costquito.globalMethods.Views;
import costquito.globalMethods.WindowUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class IniciarSesionController {

    @FXML
    private Button cerrarButton;

    @FXML
    private Pane contenidoPane;

    @FXML
    private PasswordField contrasenaPasswordField;

    @FXML
    private AnchorPane iniciarSesionAnchorPane;

    @FXML
    private Button iniciarSesionButton;

    @FXML
    private ImageView logoImageView;

    @FXML
    private Pane logoPane;

    @FXML
    private Label tituloLabel;

    @FXML
    private TextField usuarioTextField;

    @FXML
    void cerrarPrograma(ActionEvent event) {
        AppUtils.cerrarPrograma();
    }

    @FXML
    void iniciarSesion(ActionEvent e) {
        String user = usuarioTextField.getText() == null ? "" : usuarioTextField.getText().trim();
        String pass = contrasenaPasswordField.getText() == null ? "" : contrasenaPasswordField.getText();

        boolean ok = SessionManager.login(user, pass);
        if (!ok) {
            // Podrías mostrar un label de error en la UI si quieres
            LogUtils.warn("login_denied_ui", "username", user);
            return;
        }

        UserSession s = SessionManager.getCurrent();
        if (s.getRole() == UserRole.ADMIN) {
            LogUtils.setCurrentView(Views.PANEL_ADMIN_OPCION1);
            WindowUtils.navigate(Views.PANEL_ADMIN_OPCION1);
            LogUtils.audit("navigate_after_login", "to", Views.PANEL_ADMIN_OPCION1);
        } else {
            LogUtils.setCurrentView(Views.PANEL_VENDEDOR_OPCION1);
            WindowUtils.navigate(Views.PANEL_VENDEDOR_OPCION1);
            LogUtils.audit("navigate_after_login", "to", Views.PANEL_VENDEDOR_OPCION1);
        }
    }
}
