package com.lms.ui;

import com.lms.util.AppSettings;
import com.lms.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Owns the primary Stage and handles smooth cross-fade transitions between top-level screens. */
public final class SceneManager {

    private static SceneManager instance;
    private final Stage stage;
    private Scene scene;

    private SceneManager(Stage stage) {
        this.stage = stage;
    }

    public static SceneManager init(Stage stage) {
        instance = new SceneManager(stage);
        return instance;
    }

    public static SceneManager get() { return instance; }

    public Stage getStage() { return stage; }

    /** Replaces the whole window content with a fade transition. Used for Login <-> Signup <-> Dashboard. */
    public void setRoot(Parent root) {
        if (scene == null) {
            scene = new Scene(root, 1360, 860);
            attachTheme(scene);
            stage.setScene(scene);
            stage.setMinWidth(1080);
            stage.setMinHeight(680);
            FadeTransition ft = new FadeTransition(Duration.millis(260), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } else {
            root.setOpacity(0);
            scene.setRoot(root);
            applyTheme();
            FadeTransition ft = new FadeTransition(Duration.millis(280), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    private void attachTheme(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/com/lms/css/theme.css").toExternalForm());
        applyTheme();
    }

    /** Applies the original application theme and refreshes the window title. */
    public void applyTheme() {
        if (scene == null || scene.getRoot() == null) return;
        scene.getRoot().getStyleClass().remove("dark-theme");
        stage.setTitle(AppSettings.platformName() + " — Learning Management System");
    }

    /** Kept for compatibility with older callers; the application uses the original light theme. */
    public void applyTheme(String theme) {
        applyTheme();
    }

    public void goToLogin() {
        setRoot(new LoginView(this).build());
    }

    public void goToSignup() {
        setRoot(new SignupView(this).build());
    }

    public void goToDashboard() {
        setRoot(new DashboardShell(this, SessionManager.getInstance().getCurrentUser()).build());
    }
}
