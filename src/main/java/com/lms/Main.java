package com.lms;

import com.lms.ui.SceneManager;
import com.lms.util.BackgroundTask;
import com.lms.util.DBConnection;
import com.lms.util.AppSettings;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Application entry point. Keeps startup logic minimal and delegates everything
 * to the ui/service/dao layers - no business logic lives here.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(AppSettings.platformName() + " — Learning Management System");

        if (!DBConnection.testConnection()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Connection Failed");
            alert.setHeaderText("Could not connect to MySQL.");
            alert.setContentText("Please verify that MySQL is running and that " +
                    "src/main/resources/db.properties has the correct URL, username, and password. " +
                    "Also make sure you have imported database/lms.sql.");
            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.showAndWait();
            // Continue anyway - the UI will surface per-action errors gracefully rather than crashing.
        }

        try {
            DBConnection.ensureStudentRollNumbers();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(Main.class.getName())
                    .log(java.util.logging.Level.WARNING, "Could not initialize student roll numbers automatically.", e);
        }

        try {
            DBConnection.ensureDefaultAdminAccount();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(Main.class.getName())
                    .log(java.util.logging.Level.WARNING, "Could not initialize the default administrator account automatically.", e);
        }

        SceneManager sceneManager = SceneManager.init(primaryStage);
        sceneManager.goToLogin();

        primaryStage.show();
    }

    @Override
    public void stop() {
        BackgroundTask.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
