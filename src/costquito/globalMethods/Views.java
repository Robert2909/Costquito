package costquito.globalMethods;

public final class Views {

    public static final String LOGIN = "login";
    public static final String PANEL_ADMIN_OPCION1  = "panel_admin_opcion1";
    public static final String PANEL_ADMIN_OPCION2  = "panel_admin_opcion2";
    public static final String PANEL_ADMIN_OPCION3  = "panel_admin_opcion3";
    public static final String PANEL_ADMIN_OPCION4  = "panel_admin_opcion4";
    public static final String PANEL_VENDEDOR_OPCION1  = "panel_vendedor_opcion1";

    private Views() {}

    public static void registerAll() {
        WindowUtils.registerRoute(LOGIN, "/costquito/fxml/IniciarSesion.fxml");
        WindowUtils.registerRoute(PANEL_ADMIN_OPCION1,  "/costquito/fxml/PanelAdminOpcion1.fxml");
        WindowUtils.registerRoute(PANEL_ADMIN_OPCION2,  "/costquito/fxml/PanelAdminOpcion2.fxml");
        WindowUtils.registerRoute(PANEL_ADMIN_OPCION3,  "/costquito/fxml/PanelAdminOpcion3.fxml");
        WindowUtils.registerRoute(PANEL_ADMIN_OPCION4,  "/costquito/fxml/PanelAdminOpcion4.fxml");
        WindowUtils.registerRoute(PANEL_VENDEDOR_OPCION1,  "/costquito/fxml/PanelVendedorOpcion1.fxml");
    }
}
