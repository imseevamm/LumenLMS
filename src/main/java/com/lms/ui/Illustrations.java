package com.lms.ui;

import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;

/**
 * Builds small, tasteful "3D-style" decorative compositions out of plain JavaFX shapes,
 * gradients and drop shadows — no external image/3D-model dependencies, per the
 * "convincing 3D-style composition" requirement. Kept lightweight and static (no
 * animation loop) so it stays cheap to render on every dashboard/login screen.
 */
public final class Illustrations {

    private Illustrations() {}

    /**
     * A soft, layered "learner at a laptop" composition: a glowing backdrop blob,
     * a rounded laptop shape, a floating graduation cap, and small accent dots.
     * Used on the login hero panel and the student dashboard welcome card.
     */
    public static StackPane learnerComposition(double size) {
        StackPane root = new StackPane();
        root.setPrefSize(size, size);
        root.setMaxSize(size, size);

        // Soft glowing backdrop
        Circle backdrop = new Circle(size * 0.42);
        backdrop.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFFFFF", 0.55)),
                new Stop(1, Color.web("#DCEBFF", 0.35))));
        DropShadow backdropGlow = new DropShadow(40, Color.web("#287BEF", 0.25));
        backdrop.setEffect(backdropGlow);

        // Laptop base
        Rectangle base = new Rectangle(size * 0.5, size * 0.06);
        base.setArcWidth(14); base.setArcHeight(14);
        base.setFill(Color.web("#B9CFF2"));
        base.setTranslateY(size * 0.16);

        // Laptop screen
        Rectangle screen = new Rectangle(size * 0.4, size * 0.28);
        screen.setArcWidth(18); screen.setArcHeight(18);
        screen.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#287BEF")),
                new Stop(1, Color.web("#4B8FF7"))));
        DropShadow screenShadow = new DropShadow(18, Color.web("#1B5FCC", 0.45));
        screenShadow.setOffsetY(8);
        screen.setEffect(screenShadow);
        screen.setTranslateY(-size * 0.02);

        // Screen "code lines"
        javafx.scene.layout.VBox lines = new javafx.scene.layout.VBox(size * 0.03);
        lines.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 3; i++) {
            Rectangle line = new Rectangle(size * (0.22 - i * 0.045), size * 0.02);
            line.setArcWidth(6); line.setArcHeight(6);
            line.setFill(Color.web("#FFFFFF", 0.85));
            lines.getChildren().add(line);
        }
        lines.setTranslateY(-size * 0.02);
        lines.setTranslateX(-size * 0.02);

        // Floating graduation cap (top-right)
        Polygon capTop = new Polygon(
                0.0, 0.0,
                size * 0.16, size * 0.055,
                0.0, size * 0.11,
                -size * 0.16, size * 0.055
        );
        capTop.setFill(Color.web("#7B61FF"));
        DropShadow capShadow = new DropShadow(12, Color.web("#7B61FF", 0.4));
        capTop.setEffect(capShadow);
        Rectangle capBase = new Rectangle(size * 0.14, size * 0.05);
        capBase.setArcWidth(6); capBase.setArcHeight(6);
        capBase.setFill(Color.web("#6A4FE0"));
        capBase.setTranslateY(size * 0.075);
        StackPane cap = new StackPane(capBase, capTop);
        cap.setTranslateX(size * 0.28);
        cap.setTranslateY(-size * 0.30);
        cap.setRotate(-12);

        // Small accent dots (yellow + orange) for playful depth
        Circle dot1 = new Circle(size * 0.035, Color.web("#FFC857"));
        dot1.setEffect(new DropShadow(8, Color.web("#FFC857", 0.6)));
        dot1.setTranslateX(-size * 0.32);
        dot1.setTranslateY(-size * 0.22);

        Circle dot2 = new Circle(size * 0.028, Color.web("#FF8A65"));
        dot2.setEffect(new DropShadow(8, Color.web("#FF8A65", 0.6)));
        dot2.setTranslateX(size * 0.30);
        dot2.setTranslateY(size * 0.24);

        Circle dot3 = new Circle(size * 0.022, Color.web("#FFFFFF", 0.9));
        dot3.setTranslateX(-size * 0.22);
        dot3.setTranslateY(size * 0.28);

        StackPane laptop = new StackPane(base, screen, lines);

        root.getChildren().addAll(backdrop, laptop, cap, dot1, dot2, dot3);
        return root;
    }

    /**
     * A compact rounded "course topic" glyph used on course thumbnails — a soft
     * gradient tile with a single emoji-style icon, giving subtle depth without
     * a full illustration.
     */
    public static StackPane topicGlyph(String emoji, String hex, double size) {
        Circle bg = new Circle(size / 2);
        Color c = Color.web(hex);
        bg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, c.brighter()),
                new Stop(1, c.darker())));
        DropShadow shadow = new DropShadow(14, c.deriveColor(0, 1, 1, 0.5));
        shadow.setOffsetY(4);
        bg.setEffect(shadow);
        javafx.scene.control.Label label = new javafx.scene.control.Label(emoji);
        label.setFont(Font.font(size * 0.42));
        StackPane glyph = new StackPane(bg, label);
        glyph.setMaxSize(size, size);
        glyph.setMinSize(size, size);
        return glyph;
    }
}
