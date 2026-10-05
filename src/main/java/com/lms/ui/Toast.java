package com.lms.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import com.lms.util.AnimationUtil;

/**
 * Lightweight toast-notification helper. Toasts are stacked into a StackPane overlay
 * that every top-level screen mounts at its root, so any controller can call Toast.show(...).
 */
public final class Toast {

    private static StackPane overlay;

    private Toast() {}

    public static void attach(StackPane root) {
        overlay = new StackPane();
        // Toasts are informational only. They must never intercept clicks meant
        // for the dashboard navigation or the underlying page.
        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);
        StackPane.setAlignment(overlay, Pos.TOP_RIGHT);
        overlay.setPadding(new Insets(24));
        root.getChildren().add(overlay);
    }

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    public static void show(String title, String message, Type type) {
        if (overlay == null) return;

        String styleClass = switch (type) {
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            case WARNING -> "toast-warning";
            case INFO -> "toast";
        };

        Label t = new Label(title);
        t.setStyle("-fx-font-weight: 700; -fx-text-fill: -text; -fx-font-size: 13px;");
        Label m = new Label(message);
        m.getStyleClass().add("muted-text");
        m.setWrapText(true);
        m.setMaxWidth(280);

        VBox box = new VBox(4, t, m);
        box.getStyleClass().addAll("toast", styleClass);
        box.setMaxWidth(320);

        VBox container = new VBox(10);
        container.setAlignment(Pos.TOP_RIGHT);
        if (!overlay.getChildren().isEmpty() && overlay.getChildren().get(0) instanceof VBox existing) {
            existing.getChildren().add(0, box);
        } else {
            container.getChildren().add(box);
            overlay.getChildren().add(container);
        }

        AnimationUtil.slideInFromRight(box, 60, 260);

        PauseTransition delay = new PauseTransition(Duration.seconds(3.4));
        delay.setOnFinished(e -> AnimationUtil.fadeOut(box, 220, () -> {
            if (box.getParent() instanceof VBox parent) parent.getChildren().remove(box);
        }));
        delay.play();
    }

    public static void success(String title, String message) { show(title, message, Type.SUCCESS); }
    public static void error(String title, String message) { show(title, message, Type.ERROR); }
    public static void warning(String title, String message) { show(title, message, Type.WARNING); }
    public static void info(String title, String message) { show(title, message, Type.INFO); }
}
