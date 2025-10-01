package costquito.globalMethods;

import javafx.application.Platform;

public class AppUtils {

    /**
     * Cierra por completo la aplicación.
     * Se puede llamar desde cualquier parte del programa.
     */
    public static void cerrarPrograma() {
        LogUtils.audit("app_close_requested");
        try {
            Platform.exit();
        } catch (Exception e) {
            LogUtils.error("platform_exit_error", e);
        } finally {
            LogUtils.audit("app_terminated");
            System.exit(0);
        }
    }


    /**
     * Reinicia la aplicación.
     * Vuelve a ejecutar la clase principal (Costquito).
     */
    public static void reiniciarPrograma() {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classPath = System.getProperty("java.class.path");
        String mainClass = "costquito.Costquito";

        LogUtils.audit("app_restart_requested",
                "javaBin", javaBin,
                "mainClass", mainClass);

        try {
            ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classPath, mainClass);
            builder.start();
            LogUtils.info("Nuevo proceso lanzado para reinicio");

        } catch (Exception e) {
            LogUtils.error("Error al reiniciar la aplicación", e);
        } finally {
            cerrarPrograma();
        }
    }
}
