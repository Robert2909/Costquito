package costquito;

import javafx.application.Application;
import javafx.stage.Stage;

import costquito.globalMethods.WindowUtils;
import costquito.globalMethods.Views;
import costquito.globalMethods.LogUtils;
import costquito.globalMethods.UserRepository;
import java.nio.file.Paths;

public class Costquito extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            LogUtils.init(null);
            LogUtils.setMinLevel(LogUtils.Level.AUDIT);
            LogUtils.setConsoleMirror(true);

            // ======== FIJA AQUÍ la carpeta de datos ========
            java.nio.file.Path MEDIA = java.nio.file.Paths
                    .get("src", "costquito", "media")
                    .toAbsolutePath()
                    .normalize();

            costquito.globalMethods.ProductRepository.setBaseDir(MEDIA);
            costquito.globalMethods.UserRepository.setBaseDir(MEDIA);

            LogUtils.audit("media_base_dir_set",
                    "media", MEDIA.toString());

            // (opcional) crea los JSON de ejemplo si no existen
            costquito.globalMethods.UserRepository.initResource("/costquito/media/usuarios.json");
            // =================================================

            WindowUtils.initPrimaryStage(primaryStage);
            Views.registerAll();

            WindowUtils.navigate(Views.LOGIN);
            LogUtils.audit("app_started", "route", Views.LOGIN);

            primaryStage.setTitle("Costquito");
            primaryStage.show();

        } catch (Exception e) {
            LogUtils.error("fatal_boot_error", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
