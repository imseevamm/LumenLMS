package com.lms.ui;

import com.lms.model.Course;
import com.lms.service.CourseService;
import com.lms.util.AnimationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Builds consistently-styled course cards for admin, instructor, and student contexts. */
public final class CourseCardFactory {

    private static final CourseService courseService = new CourseService();

    private CourseCardFactory() {}

    private static VBox baseCard(Course c) {
        VBox card = new VBox(10);
        card.getStyleClass().add("course-card");
        card.setPrefWidth(300);
        card.setPadding(new Insets(0));

        Region thumb = new Region();
        thumb.setPrefHeight(100);
        String baseHex = c.getThumbnailColor() == null || c.getThumbnailColor().isBlank() ? "#287BEF" : c.getThumbnailColor();
        String lighterHex = toHex(javafx.scene.paint.Color.web(baseHex).brighter());
        thumb.setStyle("-fx-background-color: linear-gradient(to bottom right, " + baseHex + ", " + lighterHex + "); " +
                "-fx-background-radius: 20px 20px 0 0;");

        StackPane glyphOverlay = new StackPane(Illustrations.topicGlyph(topicEmoji(c), "#FFFFFF", 44));
        glyphOverlay.setAlignment(Pos.CENTER_RIGHT);
        glyphOverlay.setPadding(new Insets(0, 16, 0, 0));
        StackPane thumbStack = new StackPane(thumb, glyphOverlay);

        VBox body = new VBox(8);
        body.setPadding(new Insets(16));

        Label category = UIComponents.badge(c.getCategory(), "badge-info");
        Label title = new Label(c.getTitle());
        title.setStyle("-fx-text-fill: -text; -fx-font-weight: 700; -fx-font-size: 15px;");
        title.setWrapText(true);
        Label instructor = new Label("By " + (c.getInstructorName() != null ? c.getInstructorName() : "—"));
        instructor.getStyleClass().add("muted-text");

        HBox meta = new HBox(10);
        meta.getChildren().addAll(
                UIComponents.badge(c.getDifficulty().name(), "badge-accent"),
                new Label(c.getDurationHours() + "h") {{ getStyleClass().add("muted-text"); }},
                new Label("•  " + c.getEnrollmentCount() + " enrolled") {{ getStyleClass().add("muted-text"); }}
        );
        meta.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(category, title, instructor, meta);
        card.getChildren().addAll(thumbStack, body);
        AnimationUtil.applyHoverScale(card);
        return card;
    }

    private static String toHex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private static String topicEmoji(Course c) {
        String haystack = ((c.getTitle() == null ? "" : c.getTitle()) + " " + (c.getCategory() == null ? "" : c.getCategory())).toLowerCase();
        if (haystack.contains("java") || haystack.contains("python") || haystack.contains("programming") || haystack.contains("code")) return "💻";
        if (haystack.contains("data structure") || haystack.contains("algorithm")) return "🧩";
        if (haystack.contains("ai") || haystack.contains("machine learning") || haystack.contains("artificial")) return "🤖";
        if (haystack.contains("database") || haystack.contains("sql")) return "🗄️";
        if (haystack.contains("web")) return "🌐";
        if (haystack.contains("security") || haystack.contains("cyber")) return "🛡️";
        return "📘";
    }

    public static VBox studentCard(Course c, boolean enrolled, Runnable onEnroll, Runnable onOpen) {
        VBox card = baseCard(c);
        VBox body = (VBox) card.getChildren().get(1);
        Button action = enrolled ? UIComponents.secondaryButton("Continue Learning") : UIComponents.primaryButton("Enroll Now");
        action.setMaxWidth(Double.MAX_VALUE);
        action.setOnAction(e -> { if (enrolled) onOpen.run(); else onEnroll.run(); });
        body.getChildren().add(action);

        // Make the whole student course card actionable, not only the bottom button.
        // Ignore clicks originating from the button so the action is not executed twice.
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof javafx.scene.control.Button)) {
                if (enrolled) onOpen.run();
                else onEnroll.run();
            }
        });
        card.setAccessibleText(c.getTitle() + (enrolled ? ". Continue learning." : ". Enroll now."));
        return card;
    }

    public static VBox instructorCard(DashboardShell shell, Course c, Runnable onChanged) {
        VBox card = baseCard(c);
        VBox body = (VBox) card.getChildren().get(1);
        Label status = UIComponents.badge(c.isPublished() ? "Published" : "Draft", c.isPublished() ? "badge-success" : "badge-warning");
        body.getChildren().add(status);

        HBox actions = new HBox(6);
        Button manageBtn = UIComponents.ghostButton("Manage Content");
        manageBtn.setOnAction(e -> shell.openCourseContentEditor(c.getId()));
        Button editBtn = UIComponents.ghostButton("Edit");
        editBtn.setOnAction(e -> CourseEditorDialog.open(shell.getSceneManager().getStage(), c,
                shell.getCurrentUser().getId(), false, onChanged));
        Button deleteBtn = UIComponents.ghostButton("Delete");
        deleteBtn.getStyleClass().add("error-text");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + c.getTitle() + "\"? This removes all its content.");
            UIComponents.styleDialog(confirm);
            confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                courseService.deleteCourse(c.getId());
                Toast.success("Course deleted", c.getTitle() + " has been removed.");
                onChanged.run();
            });
        });
        actions.getChildren().addAll(manageBtn, editBtn, deleteBtn);
        body.getChildren().add(actions);
        return card;
    }

    public static VBox adminCard(DashboardShell shell, Course c, Runnable onChanged) {
        VBox card = baseCard(c);
        VBox body = (VBox) card.getChildren().get(1);
        Label status = UIComponents.badge(c.isPublished() ? "Published" : "Draft", c.isPublished() ? "badge-success" : "badge-warning");
        body.getChildren().add(status);

        HBox actions = new HBox(6);
        Button editBtn = UIComponents.ghostButton("Edit");
        editBtn.setOnAction(e -> CourseEditorDialog.open(shell.getSceneManager().getStage(), c,
                c.getInstructorId(), true, () -> shell.navigate("Courses")));
        Button deleteBtn = UIComponents.ghostButton("Delete");
        deleteBtn.getStyleClass().add("error-text");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + c.getTitle() + "\"? This removes all its content.");
            UIComponents.styleDialog(confirm);
            confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                courseService.deleteCourse(c.getId());
                Toast.success("Course deleted", c.getTitle() + " has been removed.");
                shell.navigate("Courses");
            });
        });
        actions.getChildren().addAll(editBtn, deleteBtn);
        body.getChildren().add(actions);

        // Make the admin course card itself actionable as well as its buttons.
        // Clicking the card opens the editor; clicking Edit/Delete keeps its own action.
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof javafx.scene.control.Button)) {
                CourseEditorDialog.open(shell.getSceneManager().getStage(), c,
                        c.getInstructorId(), true, () -> shell.navigate("Courses"));
            }
        });
        card.setAccessibleText(c.getTitle() + ". Open course editor.");
        return card;
    }
}
