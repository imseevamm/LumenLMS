package com.lms.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Reusable, centralized JavaFX animations so transitions aren't hand-rolled in every controller.
 * All animations are intentionally short and use gentle easing to feel premium, not childish.
 */
public final class AnimationUtil {

    private AnimationUtil() {}

    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    public static void fadeOut(Node node, double durationMs, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(0);
        ft.setInterpolator(Interpolator.EASE_IN);
        if (onFinished != null) ft.setOnFinished(e -> onFinished.run());
        ft.play();
    }

    public static void slideInFromRight(Node node, double distance, double durationMs) {
        node.setTranslateX(distance);
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromX(distance);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        new ParallelTransition(tt, ft).play();
    }

    public static void slideInFromBottom(Node node, double distance, double durationMs) {
        node.setTranslateY(distance);
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(distance);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        new ParallelTransition(tt, ft).play();
    }

    public static void scaleIn(Node node, double durationMs) {
        node.setScaleX(0.9);
        node.setScaleY(0.9);
        node.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(durationMs), node);
        st.setFromX(0.9); st.setFromY(0.9);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        new ParallelTransition(st, ft).play();
    }

    /** Subtle lift-on-hover, wired once per card. */
    public static void applyHoverScale(Node node) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), node);
        grow.setToX(1.02); grow.setToY(1.02);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(160), node);
        shrink.setToX(1.0); shrink.setToY(1.0);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#287BEF", 0.45));
        glow.setRadius(24);
        glow.setSpread(0.05);

        node.setOnMouseEntered(e -> { grow.playFromStart(); node.setEffect(glow); });
        node.setOnMouseExited(e -> { shrink.playFromStart(); node.setEffect(null); });
    }

    /** Quick "press" feedback for buttons. */
    public static void applyButtonPress(Node node) {
        ScaleTransition down = new ScaleTransition(Duration.millis(90), node);
        down.setToX(0.96); down.setToY(0.96);
        ScaleTransition up = new ScaleTransition(Duration.millis(120), node);
        up.setToX(1.0); up.setToY(1.0);

        node.setOnMousePressed(e -> down.playFromStart());
        node.setOnMouseReleased(e -> up.playFromStart());
    }

    public static void animateSidebarWidth(Node node, double fromWidth, double toWidth) {
        if (!(node instanceof javafx.scene.layout.Region region)) return;
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(region.minWidthProperty(), fromWidth),
                        new KeyValue(region.prefWidthProperty(), fromWidth),
                        new KeyValue(region.maxWidthProperty(), fromWidth)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(region.minWidthProperty(), toWidth, Interpolator.EASE_BOTH),
                        new KeyValue(region.prefWidthProperty(), toWidth, Interpolator.EASE_BOTH),
                        new KeyValue(region.maxWidthProperty(), toWidth, Interpolator.EASE_BOTH))
        );
        tl.play();
    }

    /** Smoothly animates a ProgressBar/ProgressIndicator's progress value from its current value to target. */
    public static void animateProgress(ProgressBar bar, double toValue, double durationMs) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), bar.getProgress())),
                new KeyFrame(Duration.millis(durationMs), new KeyValue(bar.progressProperty(), toValue, Interpolator.EASE_OUT))
        );
        tl.play();
    }

    /** Animates a Label's numeric text counting up from 0 to targetValue (e.g., dashboard stat cards). */
    public static void animateCounter(Label label, int targetValue, double durationMs) {
        IntegerPropertyProxy proxy = new IntegerPropertyProxy();
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(proxy.value, 0)),
                new KeyFrame(Duration.millis(durationMs), new KeyValue(proxy.value, targetValue, Interpolator.EASE_OUT))
        );
        proxy.value.addListener((obs, oldV, newV) -> label.setText(String.valueOf(newV.intValue())));
        tl.play();
    }

    /** Small helper wrapping an IntegerProperty since javafx.beans is verbose to inline above. */
    private static class IntegerPropertyProxy {
        final javafx.beans.property.SimpleIntegerProperty value = new javafx.beans.property.SimpleIntegerProperty(0);
    }
}
