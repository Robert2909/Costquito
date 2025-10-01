package costquito;

import javafx.application.Application;
import javafx.stage.Stage;

import costquito.globalMethods.WindowUtils;
import costquito.globalMethods.Views;
import costquito.globalMethods.LogUtils;

public class Costquito extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            LogUtils.init(null);
            LogUtils.setConsoleMirror(true);
            LogUtils.setMinLevel(LogUtils.Level.AUDIT); // o WARN si quieres ultra-silencio

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
