package costquito.controllers;

import costquito.globalMethods.AppUtils;
import costquito.globalMethods.Views;
import costquito.globalMethods.WindowUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

public class PanelAdminOpcion4Controller implements Initializable {
    
    private costquito.globalMethods.UserRecord adminModel;
    private costquito.globalMethods.UserRecord vendedorModel;

    @FXML
    private AnchorPane PanelAdminOpcion4AnchorPane;

    @FXML
    private Button administracionButton;

    @FXML
    private Pane barraSuperiorPane;

    @FXML
    private Button cerrarButton;

    @FXML
    private Pane contenidoPane;

    @FXML
    private Label cuentaAdministradorContrasenaLabel;

    @FXML
    private TextField cuentaAdministradorContrasenaTextField;

    @FXML
    private Button cuentaAdministradorEditarButton;

    @FXML
    private Button cuentaAdministradorGuardarButton;

    @FXML
    private Pane cuentaAdministradorPane;

    @FXML
    private Label cuentaAdministradorTituloLabel;

    @FXML
    private Label cuentaAdministradorUsuarioLabel;

    @FXML
    private TextField cuentaAdministradorUsuarioTextField;

    @FXML
    private Label cuentaVendedorContrasenaLabel;

    @FXML
    private TextField cuentaVendedorContrasenaTextField;

    @FXML
    private Button cuentaVendedorEditarButton;

    @FXML
    private Button cuentaVendedorGuardarButton;

    @FXML
    private Pane cuentaVendedorPane;

    @FXML
    private Label cuentaVendedorTituloLabel;

    @FXML
    private Label cuentaVendedorUsuarioLabel;

    @FXML
    private TextField cuentaVendedorUsuarioTextField;

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
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        costquito.globalMethods.UserRepository.init();
        costquito.globalMethods.UserRepository.reload();

        // --- DEBUG: ver lo que realmente llega del repo ---
        for (costquito.globalMethods.UserRecord u : costquito.globalMethods.UserRepository.getAll()) {
            System.out.println("[USERS] role=" + u.role + " user=" + u.username + " hash=" + u.passwordHash);
        }
        // ---------------------------------------------------

        adminModel    = costquito.globalMethods.UserRepository.findByRole(costquito.globalMethods.UserRole.ADMIN);
        vendedorModel = costquito.globalMethods.UserRepository.findByRole(costquito.globalMethods.UserRole.VENDOR);

        fillAdminFromModel();
        fillVendedorFromModel();

        cuentaAdministradorUsuarioTextField.setDisable(true);
        cuentaAdministradorContrasenaTextField.setDisable(true);
        cuentaAdministradorGuardarButton.setDisable(true);

        cuentaVendedorUsuarioTextField.setDisable(true);
        cuentaVendedorContrasenaTextField.setDisable(true);
        cuentaVendedorGuardarButton.setDisable(true);
    }


    
    private void fillAdminFromModel() {
        if (adminModel != null) {
            cuentaAdministradorUsuarioTextField.setText(safe(adminModel.username));
            // passwordHash puede venir como "plain:xxxx" o "sha256:...."
            cuentaAdministradorContrasenaTextField.setText(toUiPassword(adminModel.passwordHash));
        } else {
            cuentaAdministradorUsuarioTextField.setText("");
            cuentaAdministradorContrasenaTextField.setText("");
        }
    }

    private void fillVendedorFromModel() {
        if (vendedorModel != null) {
            cuentaVendedorUsuarioTextField.setText(safe(vendedorModel.username));
            cuentaVendedorContrasenaTextField.setText(toUiPassword(vendedorModel.passwordHash));
        } else {
            cuentaVendedorUsuarioTextField.setText("");
            cuentaVendedorContrasenaTextField.setText("");
        }
    }

    // Convierte el formato persistido a lo que debe verse en el TextField
    private String toUiPassword(String stored) {
        if (stored == null) return "";
        String s = stored.trim();
        if (s.regionMatches(true, 0, "plain:", 0, 6)) {
            return s.substring(6);
        }
        return "";
    }



    private String safe(String s) { return s == null ? "" : s; }

    @FXML
    void cerrarPrograma(ActionEvent event) {
        AppUtils.cerrarPrograma();
    }

    @FXML
    void editarCuentaAdministrador(ActionEvent event) {
        // Habilitar edición en la tarjeta de Administrador
        cuentaAdministradorUsuarioTextField.setDisable(false);
        cuentaAdministradorContrasenaTextField.setDisable(false);

        // Botones
        cuentaAdministradorEditarButton.setDisable(true);
        cuentaAdministradorGuardarButton.setDisable(false);

        // UX: foco al usuario
        cuentaAdministradorUsuarioTextField.requestFocus();
        cuentaAdministradorUsuarioTextField.positionCaret(
                cuentaAdministradorUsuarioTextField.getText() != null
                        ? cuentaAdministradorUsuarioTextField.getText().length() : 0);
    }

    @FXML
    void editarCuentaVendedor(ActionEvent event) {
        // Habilitar edición en la tarjeta de Vendedor
        cuentaVendedorUsuarioTextField.setDisable(false);
        cuentaVendedorContrasenaTextField.setDisable(false);

        // Botones
        cuentaVendedorEditarButton.setDisable(true);
        cuentaVendedorGuardarButton.setDisable(false);

        // UX: foco al usuario
        cuentaVendedorUsuarioTextField.requestFocus();
        cuentaVendedorUsuarioTextField.positionCaret(
                cuentaVendedorUsuarioTextField.getText() != null
                        ? cuentaVendedorUsuarioTextField.getText().length() : 0);
    }

    @FXML
    void guardarCuentaAdministrador(ActionEvent event) {
        // 1) Tomar valores de UI
        String newUser = cuentaAdministradorUsuarioTextField.getText() == null ? "" : cuentaAdministradorUsuarioTextField.getText().trim();
        String newPass = cuentaAdministradorContrasenaTextField.getText() == null ? "" : cuentaAdministradorContrasenaTextField.getText().trim();

        // 2) Validación mínima (ajústala si lo requieres)
        if (newUser.isEmpty() || newPass.isEmpty()) {
            System.out.println("Administrador: usuario y contraseña no pueden estar vacíos.");
            return;
        }

        try {
            // 3) Persistir en JSON
            costquito.globalMethods.UserRepository.updateCredentials(costquito.globalMethods.UserRole.ADMIN, newUser, newPass);
            costquito.globalMethods.UserRepository.save();

            // 4) Bloquear nuevamente la edición
            cuentaAdministradorUsuarioTextField.setDisable(true);
            cuentaAdministradorContrasenaTextField.setDisable(true);

            // Botones
            cuentaAdministradorGuardarButton.setDisable(true);
            cuentaAdministradorEditarButton.setDisable(false);

            System.out.println("Administrador: cambios guardados.");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Administrador: error al guardar.");
        }
    }

    @FXML
    void guardarCuentaVendedor(ActionEvent event) {
        // 1) Tomar valores de UI
        String newUser = cuentaVendedorUsuarioTextField.getText() == null ? "" : cuentaVendedorUsuarioTextField.getText().trim();
        String newPass = cuentaVendedorContrasenaTextField.getText() == null ? "" : cuentaVendedorContrasenaTextField.getText().trim();

        // 2) Validación mínima
        if (newUser.isEmpty() || newPass.isEmpty()) {
            System.out.println("Vendedor: usuario y contraseña no pueden estar vacíos.");
            return;
        }

        try {
            // 3) Persistir en JSON
            costquito.globalMethods.UserRepository.updateCredentials(costquito.globalMethods.UserRole.VENDOR, newUser, newPass);
            costquito.globalMethods.UserRepository.save();

            // 4) Bloquear nuevamente la edición
            cuentaVendedorUsuarioTextField.setDisable(true);
            cuentaVendedorContrasenaTextField.setDisable(true);

            // Botones
            cuentaVendedorGuardarButton.setDisable(true);
            cuentaVendedorEditarButton.setDisable(false);

            System.out.println("Vendedor: cambios guardados.");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Vendedor: error al guardar.");
        }
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
