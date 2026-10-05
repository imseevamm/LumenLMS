package com.lms.ui;

import com.lms.util.AnimationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Factory for reusable, consistently-styled JavaFX components.
 * Centralizing these keeps every screen visually consistent and avoids re-styling widgets ad hoc.
 */
public final class UIComponents {

    private UIComponents() {}

    public static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("primary-button");
        b.setCursor(javafx.scene.Cursor.HAND);
        AnimationUtil.applyButtonPress(b);
        return b;
    }

    public static Button secondaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary-button");
        b.setCursor(javafx.scene.Cursor.HAND);
        AnimationUtil.applyButtonPress(b);
        return b;
    }

    public static Button dangerButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("danger-button");
        b.setCursor(javafx.scene.Cursor.HAND);
        AnimationUtil.applyButtonPress(b);
        return b;
    }

    public static Button ghostButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("ghost-button");
        b.setCursor(javafx.scene.Cursor.HAND);
        return b;
    }

    public static Label pageTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }

    public static Label pageSubtitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-subtitle");
        return l;
    }

    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    public static Label badge(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", styleClass);
        return l;
    }

    /** A dashboard stat card with an animated counter, a colored icon chip and an optional trend line. */
    public static VBox statCard(String label, int value, String accentHex) {
        return statCard(label, value, accentHex, "●", null);
    }

    public static VBox statCard(String label, int value, String accentHex, String icon, String trend) {
        return statCard(label, value, accentHex, icon, trend, null);
    }

    /** Dashboard stat card with an optional click action. */
    public static VBox statCard(String label, int value, String accentHex, String icon, String trend, Runnable onClick) {
        VBox box = new VBox(12);
        box.getStyleClass().add("stat-card");
        box.setPrefWidth(210);

        StackPane chip = iconChip(icon, accentHex);

        Label val = new Label("0");
        val.getStyleClass().add("stat-value");
        AnimationUtil.animateCounter(val, value, 900);

        Label lab = new Label(label);
        lab.getStyleClass().add("stat-label");
        lab.setWrapText(true);

        VBox textCol = new VBox(2, val, lab);
        if (trend != null && !trend.isBlank()) {
            Label trendLabel = new Label(trend);
            trendLabel.getStyleClass().add("stat-trend");
            textCol.getChildren().add(trendLabel);
        }

        box.getChildren().addAll(chip, textCol);
        if (onClick != null) {
            box.setCursor(javafx.scene.Cursor.HAND);
            box.setOnMouseClicked(e -> onClick.run());
            box.setAccessibleText(label + " statistic. Click to open.");
        }
        AnimationUtil.applyHoverScale(box);
        return box;
    }

    public static VBox statCardDecimal(String label, String displayValue, String accentHex) {
        return statCardDecimal(label, displayValue, accentHex, "●", null);
    }

    public static VBox statCardDecimal(String label, String displayValue, String accentHex, String icon, String trend) {
        return statCardDecimal(label, displayValue, accentHex, icon, trend, null);
    }

    /** Dashboard decimal stat card with an optional click action. */
    public static VBox statCardDecimal(String label, String displayValue, String accentHex, String icon, String trend, Runnable onClick) {
        VBox box = new VBox(12);
        box.getStyleClass().add("stat-card");
        box.setPrefWidth(210);

        StackPane chip = iconChip(icon, accentHex);

        Label val = new Label(displayValue);
        val.getStyleClass().add("stat-value");

        Label lab = new Label(label);
        lab.getStyleClass().add("stat-label");
        lab.setWrapText(true);

        VBox textCol = new VBox(2, val, lab);
        if (trend != null && !trend.isBlank()) {
            Label trendLabel = new Label(trend);
            trendLabel.getStyleClass().add("stat-trend");
            textCol.getChildren().add(trendLabel);
        }

        box.getChildren().addAll(chip, textCol);
        if (onClick != null) {
            box.setCursor(javafx.scene.Cursor.HAND);
            box.setOnMouseClicked(e -> onClick.run());
            box.setAccessibleText(label + " statistic. Click to open.");
        }
        AnimationUtil.applyHoverScale(box);
        return box;
    }

    /** Small rounded, tinted icon container used at the top of stat cards (e.g. a book/chart emoji on a soft color wash). */
    public static StackPane iconChip(String icon, String accentHex) {
        Region bg = new Region();
        bg.getStyleClass().add("stat-icon-chip");
        Color c = Color.web(accentHex);
        bg.setStyle("-fx-background-color: rgba(" + (int) (c.getRed() * 255) + "," + (int) (c.getGreen() * 255) + ","
                + (int) (c.getBlue() * 255) + ",0.14);");
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(16));
        iconLabel.setTextFill(c);
        StackPane chip = new StackPane(bg, iconLabel);
        chip.setMaxSize(40, 40);
        chip.setMinSize(40, 40);
        return chip;
    }


    /** Applies the LumenLMS dialog theme to native Dialog/Alert windows. */
    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null) return;
        String css = UIComponents.class.getResource("/com/lms/css/theme.css") != null
                ? UIComponents.class.getResource("/com/lms/css/theme.css").toExternalForm() : null;
        if (css != null && !dialog.getDialogPane().getStylesheets().contains(css)) {
            dialog.getDialogPane().getStylesheets().add(css);
        }
    }

    public static VBox card(Node... children) {
        VBox box = new VBox(12, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        return box;
    }

    public static VBox glassCard(Node... children) {
        VBox box = new VBox(12, children);
        box.getStyleClass().add("glass-card");
        box.setPadding(new Insets(20));
        return box;
    }

    public static ProgressBar progress(double fraction) {
        ProgressBar bar = new ProgressBar(0);
        bar.setMaxWidth(Double.MAX_VALUE);
        AnimationUtil.animateProgress(bar, fraction, 800);
        return bar;
    }

    public static Circle avatar(String initials, double radius) {
        Circle circle = new Circle(radius);
        circle.getStyleClass().add("avatar-circle");
        circle.setFill(Color.web("#287BEF"));
        return circle;
    }

    public static StackPane avatarWithInitials(String initials, double size) {
        Circle circle = new Circle(size / 2);
        circle.getStyleClass().add("avatar-circle");
        Label label = new Label(initials);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.36));
        StackPane pane = new StackPane(circle, label);
        pane.setPrefSize(size, size);
        return pane;
    }

    /** Standard "nothing here yet" placeholder used across empty tables/lists. */
    public static VBox emptyState(String title, String subtitle) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48));
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("muted-text");
        box.getChildren().addAll(t, s);
        return box;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static javafx.scene.control.Separator hSeparator() {
        return new javafx.scene.control.Separator();
    }
}
