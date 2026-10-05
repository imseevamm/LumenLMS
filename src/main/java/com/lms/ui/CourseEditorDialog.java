package com.lms.ui;

import com.lms.model.Course;
import com.lms.model.Role;
import com.lms.model.User;
import com.lms.service.CourseService;
import com.lms.security.AuthorizationException;
import com.lms.service.UserService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

/** Modal dialog for creating/editing a course. Reused by both Admin and Instructor screens. */
public final class CourseEditorDialog {

    private CourseEditorDialog() {}

    public static void open(Stage owner, Course existing, long defaultInstructorId, boolean allowInstructorPicker, Runnable onSaved) {
        CourseService courseService = new CourseService();
        UserService userService = new UserService();

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(520);

        Label title = UIComponents.sectionTitle(existing == null ? "Create New Course" : "Edit Course");

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Course title");

        TextArea descArea = new TextArea(existing != null ? existing.getDescription() : "");
        descArea.setPromptText("Course description");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        TextArea syllabusArea = new TextArea(existing != null ? existing.getSyllabus() : "");
        syllabusArea.setPromptText("Syllabus outline");
        syllabusArea.setPrefRowCount(2);
        syllabusArea.setWrapText(true);

        TextField categoryField = new TextField(existing != null ? existing.getCategory() : "");
        categoryField.setPromptText("Category (e.g. Programming)");

        ComboBox<Course.Difficulty> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll(Course.Difficulty.values());
        difficultyBox.setValue(existing != null ? existing.getDifficulty() : Course.Difficulty.BEGINNER);

        Spinner<Integer> durationSpinner = new Spinner<>(1, 500, existing != null ? existing.getDurationHours() : 10);
        durationSpinner.setEditable(true);

        ComboBox<String> colorBox = new ComboBox<>();
        colorBox.getItems().addAll("#287BEF", "#4B8FF7", "#7B61FF", "#FFC857", "#22C55E");
        colorBox.setValue(existing != null ? existing.getThumbnailColor() : "#287BEF");

        CheckBox publishedCheck = new CheckBox("Published (visible to students)");
        publishedCheck.setSelected(existing == null || existing.isPublished());

        ComboBox<User> instructorBox = new ComboBox<>();
        if (allowInstructorPicker) {
            List<User> instructors = userService.getAllUsers().stream()
                    .filter(u -> u.getRole() == Role.INSTRUCTOR).toList();
            instructorBox.getItems().addAll(instructors);
            instructorBox.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(User u) { return u == null ? "" : u.getFullName(); }
                @Override public User fromString(String s) { return null; }
            });
            if (existing != null) {
                instructors.stream().filter(u -> u.getId() == existing.getInstructorId()).findFirst()
                        .ifPresent(instructorBox::setValue);
            } else if (!instructors.isEmpty()) {
                instructorBox.setValue(instructors.get(0));
            }
        }

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-text");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button saveBtn = UIComponents.primaryButton(existing == null ? "Create Course" : "Save Changes");
        Button cancelBtn = UIComponents.secondaryButton("Cancel");
        cancelBtn.setOnAction(e -> stage.close());

        saveBtn.setOnAction(e -> {
            try {
                Course c = existing != null ? existing : new Course();
                c.setTitle(titleField.getText());
                c.setDescription(descArea.getText());
                c.setSyllabus(syllabusArea.getText());
                c.setCategory(categoryField.getText());
                c.setDifficulty(difficultyBox.getValue());
                c.setDurationHours(durationSpinner.getValue());
                c.setThumbnailColor(colorBox.getValue());
                c.setPublished(publishedCheck.isSelected());
                c.setInstructorId(allowInstructorPicker && instructorBox.getValue() != null
                        ? instructorBox.getValue().getId() : defaultInstructorId);

                if (existing == null) {
                    courseService.createCourse(c);
                    Toast.success("Course created", "\"" + c.getTitle() + "\" has been added.");
                } else {
                    courseService.updateCourse(c);
                    Toast.success("Course updated", "Changes saved successfully.");
                }
                stage.close();
                if (onSaved != null) onSaved.run();
            } catch (AuthorizationException | CourseService.CourseServiceException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        HBox buttonRow = new HBox(10, cancelBtn, saveBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox fields = new VBox(10, titleField, descArea, syllabusArea,
                new HBox(10, categoryField, difficultyBox),
                new HBox(10, new Label("Duration (hrs):") {{ getStyleClass().add("muted-text"); }}, durationSpinner,
                        new Label("Color:") {{ getStyleClass().add("muted-text"); }}, colorBox));
        if (allowInstructorPicker) fields.getChildren().add(instructorBox);
        fields.getChildren().add(publishedCheck);

        ScrollPane scroller = new ScrollPane(fields);
        scroller.setFitToWidth(true);
        scroller.setMaxHeight(420);
        scroller.getStyleClass().add("scroll-pane");

        card.getChildren().addAll(title, scroller, errorLabel, buttonRow);

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        wrapper.prefWidthProperty().bind(owner.widthProperty());
        wrapper.prefHeightProperty().bind(owner.heightProperty());

        Scene scene = new Scene(wrapper);
        scene.setFill(null);
        scene.getStylesheets().add(CourseEditorDialog.class.getResource("/com/lms/css/theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setX(owner.getX());
        stage.setY(owner.getY());
        stage.setWidth(owner.getWidth());
        stage.setHeight(owner.getHeight());
        com.lms.util.AnimationUtil.scaleIn(card, 240);
        stage.showAndWait();
    }
}
