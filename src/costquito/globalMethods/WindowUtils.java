package costquito.globalMethods;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class WindowUtils {

    private static Stage primaryStage;

    private static final Map<String, String> routes = new ConcurrentHashMap<>();
    private static final Map<String, Parent> viewCache = new ConcurrentHashMap<>();
    private static final Map<String, Object> controllerCache = new ConcurrentHashMap<>();

    private WindowUtils() {}

    /* ===================== Inicialización ===================== */

    public static void initPrimaryStage(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("Stage primario nulo.");
        primaryStage = stage;
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(null);
        LogUtils.audit("window_init", "style", "UNDECORATED", "fullscreen", true);
    }

    /* ===================== Registro de rutas ===================== */

    public static void registerRoute(String id, String fxmlPath) {
        if (id == null || fxmlPath == null) throw new IllegalArgumentException("id y fxmlPath no pueden ser nulos.");
        routes.put(id, fxmlPath);
        LogUtils.audit("route_registered", "id", id);
    }

    /* ===================== Navegación ===================== */

    public static void navigate(String routeId) {
        navigate(routeId, false, null);
    }

    public static void navigate(String routeId, boolean useCache) {
        navigate(routeId, useCache, null);
    }

    public static void navigate(String routeId, Consumer<Object> controllerHook) {
        navigate(routeId, false, controllerHook);
    }

    public static void navigate(String routeId, boolean useCache, Consumer<Object> hook) {
        ensureFx(() -> {
            ensureInitialized();
            String fxml = routes.get(routeId);
            if (fxml == null) {
                LogUtils.warn("route_missing", "routeId", routeId);
                throw new IllegalStateException("Ruta no registrada: " + routeId);
            }
            try {
                Parent root;
                Object controller = null;
                if (useCache && viewCache.containsKey(routeId)) {
                    root = viewCache.get(routeId);
                    controller = controllerCache.get(routeId);
                } else {
                    FXMLLoader loader = new FXMLLoader(WindowUtils.class.getResource(fxml));
                    root = loader.load();
                    controller = loader.getController();
                    if (useCache) {
                        viewCache.put(routeId, root);
                        controllerCache.put(routeId, controller);
                    }
                }
                if (primaryStage.getScene() == null) primaryStage.setScene(new Scene(root));
                else primaryStage.getScene().setRoot(root);

                if (hook != null && controller != null) hook.accept(controller);

                LogUtils.audit("navigate_success", "routeId", routeId, "cache", useCache);
            } catch (Exception e) {
                LogUtils.error("navigate_error", e, "routeId", routeId);
                throw new RuntimeException(e);
            }
        });
    }

    /* ===================== Internos ===================== */

    private static void ensureInitialized() {
        if (primaryStage == null) {
            LogUtils.warn("windowutils_no_inicializado");
            throw new IllegalStateException("WindowUtils no inicializado. Llama a initPrimaryStage() en el arranque.");
        }
    }

    private static void ensureFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}

// PREGUNTAR DONDE ESTA EL LOG
// PEDIR PRINTEARLO 