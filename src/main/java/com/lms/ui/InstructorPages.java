package com.lms.ui;

import com.lms.model.*;
import com.lms.service.*;
import com.lms.security.AuthorizationException;
import com.lms.util.BackgroundTask;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InstructorPages {

    private static final CourseService courseService = new CourseService();
    private static final QuizService quizService = new QuizService();
    private static final AssignmentService assignmentService = new AssignmentService();
    private static final EnrollmentService enrollmentService = new EnrollmentService();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private InstructorPages() {}

    // ============================= DASHBOARD =============================

    public static Node dashboard(DashboardShell shell, User instructor) {
        VBox root = new VBox(24);
        root.getChildren().add(UIComponents.pageTitle("Welcome back, " + firstName(instructor.getFullName())));
        root.getChildren().add(UIComponents.pageSubtitle("Here's how your courses are performing."));

        List<Course> myCourses = courseService.getCoursesByInstructor(instructor.getId());
        int totalStudents = myCourses.stream().mapToInt(Course::getEnrollmentCount).sum();
        int totalQuizzes = myCourses.stream().mapToInt(c -> quizService.getQuizzesForCourse(c.getId()).size()).sum();
        int totalAssignments = myCourses.stream().mapToInt(c -> assignmentService.getForCourse(c.getId()).size()).sum();
        double avgScore = myCourses.stream().mapToDouble(c -> quizService.averageScoreForCourse(c.getId())).average().orElse(0);

        HBox stats = new HBox(18,
                UIComponents.statCard("My Courses", myCourses.size(), "#287BEF", "📚", null, () -> shell.navigate("My Courses")),
                UIComponents.statCard("Total Students", totalStudents, "#4B8FF7", "🎓", null, () -> shell.navigate("Students")),
                UIComponents.statCard("Quizzes", totalQuizzes, "#7B61FF", "📝", null, () -> shell.navigate("Quizzes")),
                UIComponents.statCard("Assignments", totalAssignments, "#FFC857", "📄", null, () -> shell.navigate("Assignments")),
                UIComponents.statCardDecimal("Avg Quiz Score", String.format("%.0f%%", avgScore), "#22C55E", "🎯", null, () -> shell.navigate("Quizzes"))
        );

        VBox coursesCard = UIComponents.card(UIComponents.sectionTitle("Your Courses"));
        VBox list = new VBox(12);
        for (Course c : myCourses) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            Label title = strong(c.getTitle());
            title.setPrefWidth(220);
            row.getChildren().addAll(title,
                    UIComponents.badge(c.isPublished() ? "Published" : "Draft", c.isPublished() ? "badge-success" : "badge-warning"),
                    muted(c.getEnrollmentCount() + " students"));
            list.getChildren().add(row);
        }
        if (myCourses.isEmpty()) list.getChildren().add(UIComponents.emptyState("No courses yet", "Create your first course from My Courses."));
        coursesCard.getChildren().add(list);

        root.getChildren().addAll(stats, coursesCard);
        return root;
    }

    // ============================= MY COURSES =============================

    public static Node myCourses(DashboardShell shell, User instructor) {
        VBox root = new VBox(20);
        HBox headerRow = new HBox(UIComponents.pageTitle("My Courses"), UIComponents.spacer());
        Button createBtn = UIComponents.primaryButton("+ Create Course");
        headerRow.getChildren().add(createBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(headerRow, UIComponents.pageSubtitle("Create and manage your courses, content, and structure."));

        FlowPane grid = new FlowPane(18, 18);
        Runnable refresh = () -> {
            grid.getChildren().clear();
            List<Course> courses = courseService.getCoursesByInstructor(instructor.getId());
            if (courses.isEmpty()) grid.getChildren().add(UIComponents.emptyState("No courses yet", "Click \"Create Course\" to get started."));
            for (Course c : courses) grid.getChildren().add(CourseCardFactory.instructorCard(shell, c, () -> shell.navigate("My Courses")));
        };
        createBtn.setOnAction(e -> CourseEditorDialog.open(shell.getSceneManager().getStage(), null,
                instructor.getId(), false, refresh));
        refresh.run();

        root.getChildren().add(grid);
        return root;
    }

    // ============================= COURSE CONTENT EDITOR =============================

    public static Node courseContent(DashboardShell shell, long courseId) {
        Course course = courseService.getCourse(courseId).orElseThrow();
        VBox root = new VBox(20);

        Button back = UIComponents.ghostButton("← Back to My Courses");
        back.setOnAction(e -> shell.navigate("My Courses"));
        root.getChildren().add(back);
        root.getChildren().add(UIComponents.pageTitle(course.getTitle() + " — Content"));
        root.getChildren().add(UIComponents.pageSubtitle("Organize modules, lessons, and learning materials."));

        VBox modulesBox = new VBox(14);
        Runnable[] refreshHolder = new Runnable[1];
        Runnable refresh = () -> {
            modulesBox.getChildren().clear();
            List<CourseModule> modules = courseService.getModules(courseId);
            if (modules.isEmpty()) modulesBox.getChildren().add(UIComponents.emptyState("No modules yet", "Add your first module below."));
            int pos = 1;
            for (CourseModule m : modules) modulesBox.getChildren().add(buildModuleCard(shell, courseId, m, refreshHolder));
        };
        refreshHolder[0] = refresh;

        TextField newModuleField = new TextField();
        newModuleField.setPromptText("New module title (e.g. \"Week 1: Foundations\")");
        Button addModuleBtn = UIComponents.primaryButton("+ Add Module");
        addModuleBtn.setOnAction(e -> {
            if (newModuleField.getText().isBlank()) { Toast.error("Missing title", "Please enter a module title."); return; }
            String moduleTitle = newModuleField.getText().trim();
            BackgroundTask.run(() -> {
                int nextPos = courseService.getModules(courseId).size() + 1;
                courseService.addModule(courseId, moduleTitle, nextPos);
                return null;
            }, ignored -> {
                newModuleField.clear();
                Toast.success("Module added", "New module created.");
                refresh.run();
            }, error -> Toast.error("Could not add module", safeError(error)));
        });
        HBox addModuleRow = new HBox(10, newModuleField, addModuleBtn);
        HBox.setHgrow(newModuleField, Priority.ALWAYS);

        refresh.run();
        root.getChildren().addAll(modulesBox, UIComponents.card(UIComponents.sectionTitle("Add Module"), addModuleRow));
        return root;
    }

    private static VBox buildModuleCard(DashboardShell shell, long courseId, CourseModule module, Runnable[] refreshHolder) {
        VBox card = UIComponents.card();
        HBox header = new HBox(10, strong(module.getTitle()), UIComponents.spacer());
        Button deleteModuleBtn = UIComponents.ghostButton("Delete Module");
        deleteModuleBtn.getStyleClass().add("error-text");
        deleteModuleBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete module \"" + module.getTitle() + "\" and all its lessons?");
            UIComponents.styleDialog(confirm);
            confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                runDbAction(() -> courseService.deleteModule(module.getId()),
                        () -> { Toast.success("Module deleted", ""); refreshHolder[0].run(); },
                        "Could not delete module");
            });
        });
        header.getChildren().add(deleteModuleBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox lessonsBox = new VBox(8);
        for (Lesson l : module.getLessons()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color: rgba(40,123,239,0.05); -fx-background-radius: 8px;");
            Label lt = new Label(l.getTitle());
            lt.setStyle("-fx-text-fill: -text; -fx-font-size: 12.5px;");
            lt.setPrefWidth(240);
            Button editBtn = UIComponents.ghostButton("Edit");
            editBtn.setOnAction(e -> openLessonDialog(shell, module, l, refreshHolder[0]));
            Button delBtn = UIComponents.ghostButton("Delete");
            delBtn.getStyleClass().add("error-text");
            delBtn.setOnAction(e -> {
                runDbAction(() -> courseService.deleteLesson(l.getId()),
                        () -> { Toast.success("Lesson deleted", ""); refreshHolder[0].run(); },
                        "Could not delete lesson");
            });
            row.getChildren().addAll(lt, muted(l.getDurationMinutes() + " min"), UIComponents.spacer(), editBtn, delBtn);
            lessonsBox.getChildren().add(row);
        }
        Button addLessonBtn = UIComponents.ghostButton("+ Add Lesson");
        addLessonBtn.setOnAction(e -> openLessonDialog(shell, module, null, refreshHolder[0]));

        card.getChildren().addAll(header, lessonsBox, addLessonBtn);
        return card;
    }

    private static void openLessonDialog(DashboardShell shell, CourseModule module, Lesson existing, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        UIComponents.styleDialog(dialog);
        dialog.initOwner(shell.getSceneManager().getStage());
        dialog.setTitle(existing == null ? "Add Lesson" : "Edit Lesson");
        dialog.getDialogPane().getStylesheets().add(InstructorPages.class.getResource("/com/lms/css/theme.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFFFF;");

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Lesson title");
        TextArea contentArea = new TextArea(existing != null ? existing.getContent() : "");
        contentArea.setPromptText("Lesson content / text material");
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);
        Spinner<Integer> durationSpinner = new Spinner<>(1, 300, existing != null ? existing.getDurationMinutes() : 15);
        durationSpinner.setEditable(true);
        TextField materialLinkField = new TextField();
        materialLinkField.setPromptText("Optional resource link (https://...)");

        List<File> selectedAttachments = new ArrayList<>();
        Label attachmentLabel = muted("No attachments selected (0/10)");
        Button attachBtn = UIComponents.ghostButton("📎 Add Attachments");
        attachBtn.setOnAction(e -> {
            int existingFileCount = 0;
            if (existing != null) {
                existingFileCount = (int) courseService.getMaterials(existing.getId()).stream()
                        .filter(m -> m.getType() == Material.Type.FILE)
                        .count();
            }
            int remaining = Math.max(0, 10 - existingFileCount);
            if (remaining == 0) {
                Toast.error("Attachment limit reached", "A lesson can have a maximum of 10 documents.");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Attach lesson notes (up to " + remaining + " more)");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Notes and documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.ppt", "*.pptx"));
            List<File> chosen = chooser.showOpenMultipleDialog(shell.getSceneManager().getStage());
            if (chosen != null && !chosen.isEmpty()) {
                int availableSlots = 10 - existingFileCount - selectedAttachments.size();
                if (chosen.size() > availableSlots) {
                    Toast.error("Attachment limit exceeded", "You can add only " + availableSlots + " more document(s) to this lesson.");
                    return;
                }
                selectedAttachments.addAll(chosen);
                attachmentLabel.setText(selectedAttachments.size() + " attachment(s) selected ("
                        + (existingFileCount + selectedAttachments.size()) + "/10) • "
                        + selectedAttachments.stream().map(File::getName).reduce((a, b) -> a + ", " + b).orElse(""));
            }
        });
        HBox attachmentRow = new HBox(10, attachBtn, attachmentLabel);
        attachmentRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, titleField, contentArea,
                new HBox(10, new Label("Duration (min):") {{ getStyleClass().add("muted-text"); }}, durationSpinner),
                materialLinkField, attachmentRow);
        content.setPadding(new Insets(16));
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isBlank()) {
                Toast.error("Missing title", "Please enter a lesson title.");
                event.consume();
                return;
            }
            int duration = durationSpinner.getValue() == null ? 15 : durationSpinner.getValue();
            if (duration < 1) {
                Toast.error("Invalid duration", "Lesson duration must be at least 1 minute.");
                event.consume();
                return;
            }
            String lessonContent = contentArea.getText();
            String resource = materialLinkField.getText() == null ? "" : materialLinkField.getText().trim();
            List<File> attachments = new ArrayList<>(selectedAttachments);
            runDbAction(() -> {
                if (existing == null) {
                    int pos = module.getLessons().size() + 1;
                    Lesson lesson = courseService.addLesson(module.getId(), title, lessonContent, pos, duration);
                    if (!resource.isBlank()) {
                        courseService.addMaterial(lesson.getId(), "Resource Link", Material.Type.LINK, resource);
                    }
                    for (File attachment : attachments) {
                        Path stored = storeLessonAttachment(attachment);
                        courseService.addMaterial(lesson.getId(), attachment.getName(), Material.Type.FILE, stored.toString());
                    }
                } else {
                    existing.setTitle(title);
                    existing.setContent(lessonContent);
                    existing.setDurationMinutes(duration);
                    courseService.updateLesson(existing);
                    for (File attachment : attachments) {
                        Path stored = storeLessonAttachment(attachment);
                        courseService.addMaterial(existing.getId(), attachment.getName(), Material.Type.FILE, stored.toString());
                    }
                }
            }, () -> {
                Toast.success(existing == null ? "Lesson added" : "Lesson updated",
                        existing == null ? "Lesson created successfully." : "Lesson updated successfully.");
                onSaved.run();
            }, existing == null ? "Could not add lesson" : "Could not update lesson");
            event.consume();
        });
        dialog.showAndWait();
    }

    private static Path storeLessonAttachment(File source) throws IOException {
        Path dir = Path.of(System.getProperty("user.home"), "LumenLMS", "uploads", "materials");
        Files.createDirectories(dir);
        String name = source.getName().replaceAll("[\\/:*?\"<>|]", "_");
        Path target = dir.resolve(name);
        if (Files.exists(target)) {
            String base = name;
            String ext = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) { base = name.substring(0, dot); ext = name.substring(dot); }
            int n = 1;
            do { target = dir.resolve(base + " (" + n++ + ")" + ext); } while (Files.exists(target));
        }
        Files.copy(source.toPath(), target, StandardCopyOption.COPY_ATTRIBUTES);
        return target;
    }

    private static String safeError(RuntimeException ex) {
        return safeError((Throwable) ex);
    }

    private static String safeError(Throwable ex) {
        String message = ex == null ? null : ex.getMessage();
        return message == null || message.isBlank() ? "Please check your database connection and try again." : message;
    }

    // ============================= QUIZZES =============================

    public static Node quizzes(DashboardShell shell, User instructor) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Quizzes"));
        root.getChildren().add(UIComponents.pageSubtitle("Create and manage quizzes across your courses."));

        List<Course> courses = courseService.getCoursesByInstructor(instructor.getId());
        ComboBox<Course> courseBox = new ComboBox<>(FXCollections.observableArrayList(courses));
        courseBox.setConverter(courseConverter());
        if (!courses.isEmpty()) {
            Course remembered = courses.stream()
                    .filter(c -> shell.getSelectedCourseId("Quizzes") != null && c.getId() == shell.getSelectedCourseId("Quizzes"))
                    .findFirst().orElse(courses.get(0));
            courseBox.setValue(remembered);
            shell.rememberSelectedCourse("Quizzes", remembered.getId());
        }
        if (courses.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("No courses yet", "Create a course from My Courses before creating quizzes."));
            return root;
        }

        VBox listBox = new VBox(12);
        Runnable[] refreshHolder = new Runnable[1];
        Runnable refresh = () -> {
            listBox.getChildren().clear();
            if (courseBox.getValue() == null) return;
            List<Quiz> quizzes = quizService.getQuizzesForCourse(courseBox.getValue().getId());
            if (quizzes.isEmpty()) listBox.getChildren().add(UIComponents.emptyState("No quizzes yet", "Create one below."));
            for (Quiz q : quizzes) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14));
                row.getStyleClass().add("card");
                Label title = strong(q.getTitle());
                title.setPrefWidth(220);
                Button editBtn = UIComponents.ghostButton("Edit");
                editBtn.setOnAction(e -> openQuizEditDialog(shell, q, refreshHolder[0]));
                Button manageBtn = UIComponents.ghostButton("Manage Questions");
                manageBtn.setOnAction(e -> { shell.rememberSelectedCourse("Quizzes", courseBox.getValue().getId()); shell.openQuizEditor(q.getId(), courseBox.getValue().getId()); });
                Button publishBtn = UIComponents.ghostButton(q.isPublished() ? "Unpublish" : "Publish");
                publishBtn.setOnAction(e -> {
                    runDbAction(() -> quizService.togglePublish(q),
                            () -> { Toast.success("Quiz updated", "Course: " + courseBox.getValue().getTitle()); refreshHolder[0].run(); },
                            "Could not update quiz");
                });
                Button deleteBtn = UIComponents.ghostButton("Delete");
                deleteBtn.getStyleClass().add("error-text");
                deleteBtn.setOnAction(e -> {
                    runDbAction(() -> quizService.deleteQuiz(q.getId()),
                            () -> { Toast.success("Quiz deleted", "Course: " + courseBox.getValue().getTitle()); refreshHolder[0].run(); },
                            "Could not delete quiz");
                });
                row.getChildren().addAll(title,
                        UIComponents.badge(q.isPublished() ? "Published" : "Draft", q.isPublished() ? "badge-success" : "badge-warning"),
                        muted(q.getDurationMinutes() + " min"), UIComponents.spacer(), editBtn, manageBtn, publishBtn, deleteBtn);
                listBox.getChildren().add(row);
            }
        };
        refreshHolder[0] = refresh;
        courseBox.valueProperty().addListener((o, ov, nv) -> { if (nv != null) shell.rememberSelectedCourse("Quizzes", nv.getId()); refresh.run(); });

        TextField titleField = new TextField();
        titleField.setPromptText("Quiz title");
        TextField descField = new TextField();
        descField.setPromptText("Description");
        Spinner<Integer> durationSpinner = new Spinner<>(1, 180, 15);
        durationSpinner.setEditable(true);
        Button createBtn = UIComponents.primaryButton("+ Create Quiz");
        createBtn.setOnAction(e -> {
            if (courseBox.getValue() == null) { Toast.error("No course", "Create a course first."); return; }
            long selectedCourseId = courseBox.getValue().getId();
            String selectedCourseTitle = courseBox.getValue().getTitle();
            String quizTitle = titleField.getText();
            String quizDescription = descField.getText();
            int quizDuration = durationSpinner.getValue();
            runDbAction(() -> quizService.createQuiz(selectedCourseId, quizTitle, quizDescription, quizDuration),
                    () -> { titleField.clear(); descField.clear();
                        Toast.success("Quiz created", "Course: " + selectedCourseTitle + " • Add questions from Manage Questions.");
                        refresh.run(); },
                    "Could not create quiz");
        });
        HBox createRow = new HBox(10, titleField, descField, durationSpinner, createBtn);
        HBox.setHgrow(titleField, Priority.ALWAYS);
        HBox.setHgrow(descField, Priority.ALWAYS);

        refresh.run();
        root.getChildren().addAll(courseBox, listBox, UIComponents.card(UIComponents.sectionTitle("Create Quiz"), createRow));
        return root;
    }

    private static void openQuizEditDialog(DashboardShell shell, Quiz quiz, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        UIComponents.styleDialog(dialog);
        dialog.initOwner(shell.getSceneManager().getStage());
        dialog.setTitle("Edit Quiz");
        dialog.getDialogPane().getStylesheets().add(InstructorPages.class.getResource("/com/lms/css/theme.css").toExternalForm());
        TextField title = new TextField(quiz.getTitle());
        TextField description = new TextField(quiz.getDescription() == null ? "" : quiz.getDescription());
        Spinner<Integer> duration = new Spinner<>(1, 480, Math.max(1, quiz.getDurationMinutes()));
        duration.setEditable(true);
        VBox content = new VBox(10, title, description, new HBox(10, new Label("Duration (minutes):"), duration));
        content.setPadding(new Insets(16));
        content.setPrefWidth(460);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                quiz.setTitle(title.getText());
                quiz.setDescription(description.getText());
                quiz.setDurationMinutes(duration.getValue());
                runDbAction(() -> quizService.updateQuiz(quiz),
                        () -> { Toast.success("Quiz updated", "Quiz details have been saved."); onSaved.run(); },
                        "Could not update quiz");
            }
            return bt;
        });
        dialog.showAndWait();
    }

    public static Node quizEditor(DashboardShell shell, long quizId, long courseId) {
        Quiz quiz = quizService.getQuizWithQuestions(quizId).orElseThrow();
        VBox root = new VBox(20);

        Button back = UIComponents.ghostButton("← Back to Quizzes");
        back.setOnAction(e -> shell.navigate("Quizzes"));
        root.getChildren().addAll(back, UIComponents.pageTitle(quiz.getTitle() + " — Questions"),
                UIComponents.pageSubtitle("Total marks: " + quiz.totalMarks()));

        VBox questionsBox = new VBox(12);
        Runnable[] refreshHolder = new Runnable[1];
        Runnable refresh = () -> {
            questionsBox.getChildren().clear();
            Quiz reloaded = quizService.getQuizWithQuestions(quizId).orElseThrow();
            int i = 1;
            for (QuizQuestion q : reloaded.getQuestions()) {
                VBox card = UIComponents.card();
                Label qt = strong((i) + ". " + q.getQuestionText());
                qt.setWrapText(true);
                VBox options = new VBox(4,
                        optionLabel('A', q), optionLabel('B', q), optionLabel('C', q), optionLabel('D', q));
                Button delBtn = UIComponents.ghostButton("Delete Question");
                delBtn.getStyleClass().add("error-text");
                delBtn.setOnAction(e -> {
                    runDbAction(() -> quizService.deleteQuestion(q.getId()),
                            () -> { Toast.success("Question removed", ""); refreshHolder[0].run(); },
                            "Could not remove question");
                });
                HBox footer = new HBox(UIComponents.badge(q.getMarks() + " mark(s)", "badge-info"), UIComponents.spacer(), delBtn);
                footer.setAlignment(Pos.CENTER_LEFT);
                card.getChildren().addAll(qt, options, footer);
                questionsBox.getChildren().add(card);
                i++;
            }
            if (reloaded.getQuestions().isEmpty()) questionsBox.getChildren().add(UIComponents.emptyState("No questions yet", "Add one below."));
        };
        refreshHolder[0] = refresh;

        // Add question form
        TextField questionField = new TextField();
        questionField.setPromptText("Question text");
        TextField optA = new TextField(); optA.setPromptText("Option A");
        TextField optB = new TextField(); optB.setPromptText("Option B");
        TextField optC = new TextField(); optC.setPromptText("Option C");
        TextField optD = new TextField(); optD.setPromptText("Option D");
        ComboBox<String> correctBox = new ComboBox<>(FXCollections.observableArrayList("A", "B", "C", "D"));
        correctBox.setValue("A");
        Spinner<Integer> marksSpinner = new Spinner<>(1, 20, 1);
        marksSpinner.setEditable(true);

        Button addQuestionBtn = UIComponents.primaryButton("+ Add Question");
        addQuestionBtn.setOnAction(e -> {
            String questionText = questionField.getText();
            String a = optA.getText(), b = optB.getText(), c = optC.getText(), d = optD.getText();
            char correct = correctBox.getValue().charAt(0);
            int marks = marksSpinner.getValue();
            runDbAction(() -> {
                int pos = quizService.getQuizWithQuestions(quizId).orElseThrow().getQuestions().size() + 1;
                quizService.addQuestion(quizId, questionText, a, b, c, d, correct, marks, pos);
            }, () -> {
                questionField.clear(); optA.clear(); optB.clear(); optC.clear(); optD.clear();
                Toast.success("Question added", ""); refresh.run();
            }, "Could not add question");
        });

        VBox addForm = UIComponents.card(UIComponents.sectionTitle("Add Question"),
                questionField,
                new HBox(10, optA, optB), new HBox(10, optC, optD),
                new HBox(10, new Label("Correct:") {{ getStyleClass().add("muted-text"); }}, correctBox,
                        new Label("Marks:") {{ getStyleClass().add("muted-text"); }}, marksSpinner),
                addQuestionBtn);

        Button submitBtn = UIComponents.primaryButton("✓ Submit / Finish Questions");
        submitBtn.setOnAction(e -> {
            shell.rememberSelectedCourse(courseId);
            Toast.success("Questions saved", "Quiz updated for course: " + quiz.getTitle());
            shell.navigate("Quizzes");
        });
        HBox finishRow = new HBox(UIComponents.spacer(), submitBtn);
        finishRow.setAlignment(Pos.CENTER_RIGHT);

        refresh.run();
        root.getChildren().addAll(questionsBox, addForm, finishRow);
        return root;
    }

    private static Label optionLabel(char letter, QuizQuestion q) {
        boolean correct = q.getCorrectOption() == letter;
        Label l = new Label((correct ? "✓ " : "  ") + letter + ". " + q.optionText(letter));
        l.setStyle("-fx-text-fill: " + (correct ? "-success" : "-muted") + "; -fx-font-size: 12.5px;");
        return l;
    }

    // ============================= ASSIGNMENTS =============================

    public static Node assignments(DashboardShell shell, User instructor) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Assignments"));
        root.getChildren().add(UIComponents.pageSubtitle("Create assignments and grade student submissions."));

        List<Course> courses = courseService.getCoursesByInstructor(instructor.getId());
        ComboBox<Course> courseBox = new ComboBox<>(FXCollections.observableArrayList(courses));
        courseBox.setConverter(courseConverter());
        if (!courses.isEmpty()) {
            Course remembered = courses.stream()
                    .filter(c -> shell.getSelectedCourseId("Assignments") != null && c.getId() == shell.getSelectedCourseId("Assignments"))
                    .findFirst().orElse(courses.get(0));
            courseBox.setValue(remembered);
            shell.rememberSelectedCourse("Assignments", remembered.getId());
        }
        if (courses.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("No courses yet", "Create a course from My Courses before creating assignments."));
            return root;
        }

        VBox listBox = new VBox(12);
        Runnable[] refreshHolder = new Runnable[1];
        Runnable refresh = () -> {
            listBox.getChildren().clear();
            if (courseBox.getValue() == null) return;
            List<Assignment> list = assignmentService.getForCourse(courseBox.getValue().getId());
            if (list.isEmpty()) listBox.getChildren().add(UIComponents.emptyState("No assignments yet", "Create one below."));
            for (Assignment a : list) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14));
                row.getStyleClass().add("card");
                Label title = strong(a.getTitle());
                title.setPrefWidth(200);
                int submissionCount = assignmentService.getSubmissionsForAssignment(a.getId()).size();
                Button editBtn = UIComponents.ghostButton("Edit");
                editBtn.setOnAction(e -> openAssignmentEditDialog(shell, a, refreshHolder[0]));
                Button gradeBtn = UIComponents.ghostButton("View Submissions (" + submissionCount + ")");
                gradeBtn.setOnAction(e -> shell.openGrading(a.getId()));
                Button deleteBtn = UIComponents.ghostButton("Delete");
                deleteBtn.getStyleClass().add("error-text");
                deleteBtn.setOnAction(e -> {
                    runDbAction(() -> assignmentService.delete(a.getId()),
                            () -> { Toast.success("Assignment deleted", "Course: " + courseBox.getValue().getTitle()); refreshHolder[0].run(); },
                            "Could not delete assignment");
                });
                row.getChildren().addAll(title,
                        muted("Due " + a.getDeadline().format(DTF)),
                        UIComponents.badge(a.getMaxMarks() + " marks", "badge-info"),
                        UIComponents.spacer(), editBtn, gradeBtn, deleteBtn);
                listBox.getChildren().add(row);
            }
        };
        refreshHolder[0] = refresh;
        courseBox.valueProperty().addListener((o, ov, nv) -> { if (nv != null) shell.rememberSelectedCourse("Assignments", nv.getId()); refresh.run(); });

        TextField titleField = new TextField(); titleField.setPromptText("Assignment title");
        TextArea descArea = new TextArea(); descArea.setPromptText("Description"); descArea.setPrefRowCount(2); descArea.setWrapText(true);
        TextArea instructionsArea = new TextArea(); instructionsArea.setPromptText("Instructions"); instructionsArea.setPrefRowCount(2); instructionsArea.setWrapText(true);
        DatePicker deadlineDate = new DatePicker(LocalDate.now().plusDays(7));
        Spinner<Integer> maxMarksSpinner = new Spinner<>(1, 1000, 100);
        maxMarksSpinner.setEditable(true);

        Button createBtn = UIComponents.primaryButton("+ Create Assignment");
        createBtn.setOnAction(e -> {
            if (courseBox.getValue() == null) { Toast.error("No course", "Create a course first."); return; }
            long selectedCourseId = courseBox.getValue().getId();
            String selectedCourseTitle = courseBox.getValue().getTitle();
            String assignmentTitle = titleField.getText();
            String descriptionText = descArea.getText();
            String instructionsText = instructionsArea.getText();
            LocalDateTime deadline = LocalDateTime.of(deadlineDate.getValue(), LocalTime.of(23, 59));
            int maxMarks = maxMarksSpinner.getValue();
            runDbAction(() -> assignmentService.create(selectedCourseId, assignmentTitle, descriptionText,
                            instructionsText, deadline, maxMarks),
                    () -> { titleField.clear(); descArea.clear(); instructionsArea.clear();
                        Toast.success("Assignment created", "Course: " + selectedCourseTitle); refresh.run(); },
                    "Could not create assignment");
        });

        VBox createForm = UIComponents.card(UIComponents.sectionTitle("Create Assignment"),
                titleField, descArea, instructionsArea,
                new HBox(10, new Label("Deadline:") {{ getStyleClass().add("muted-text"); }}, deadlineDate,
                        new Label("Max marks:") {{ getStyleClass().add("muted-text"); }}, maxMarksSpinner),
                createBtn);

        refresh.run();
        root.getChildren().addAll(courseBox, listBox, createForm);
        return root;
    }

    private static void openAssignmentEditDialog(DashboardShell shell, Assignment assignment, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        UIComponents.styleDialog(dialog);
        dialog.initOwner(shell.getSceneManager().getStage());
        dialog.setTitle("Edit Assignment");
        dialog.getDialogPane().getStylesheets().add(InstructorPages.class.getResource("/com/lms/css/theme.css").toExternalForm());
        TextField title = new TextField(assignment.getTitle());
        TextArea description = new TextArea(assignment.getDescription() == null ? "" : assignment.getDescription());
        description.setPrefRowCount(3);
        TextArea instructions = new TextArea(assignment.getInstructions() == null ? "" : assignment.getInstructions());
        instructions.setPrefRowCount(3);
        DatePicker deadline = new DatePicker(assignment.getDeadline().toLocalDate());
        Spinner<Integer> marks = new Spinner<>(1, 1000, Math.max(1, assignment.getMaxMarks()));
        marks.setEditable(true);
        VBox content = new VBox(10, title, description, instructions,
                new HBox(10, new Label("Deadline:"), deadline, new Label("Max marks:"), marks));
        content.setPadding(new Insets(16));
        content.setPrefWidth(560);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                assignment.setTitle(title.getText());
                assignment.setDescription(description.getText());
                assignment.setInstructions(instructions.getText());
                assignment.setDeadline(LocalDateTime.of(deadline.getValue(), LocalTime.of(23, 59)));
                assignment.setMaxMarks(marks.getValue());
                runDbAction(() -> assignmentService.update(assignment),
                        () -> { Toast.success("Assignment updated", "Assignment details have been saved."); onSaved.run(); },
                        "Could not update assignment");
            }
            return bt;
        });
        dialog.showAndWait();
    }

    public static Node gradingPage(DashboardShell shell, long assignmentId) {
        Assignment assignment = assignmentService.getById(assignmentId).orElseThrow();
        VBox root = new VBox(20);
        Button back = UIComponents.ghostButton("← Back to Assignments");
        back.setOnAction(e -> shell.navigate("Assignments"));
        root.getChildren().addAll(back, UIComponents.pageTitle(assignment.getTitle() + " — Submissions"),
                UIComponents.pageSubtitle("Grade student work and leave feedback."));

        List<Submission> submissions = assignmentService.getSubmissionsForAssignment(assignmentId);
        if (submissions.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("No submissions yet", "Students haven't submitted this assignment."));
            return root;
        }

        for (Submission s : submissions) {
            VBox card = UIComponents.card();
            HBox header = new HBox(10, strong(s.getStudentName()), UIComponents.spacer(),
                    UIComponents.badge(s.getStatus().name(), s.getStatus() == Submission.Status.GRADED ? "badge-success" : "badge-warning"));
            header.setAlignment(Pos.CENTER_LEFT);
            Label submittedAt = muted("Submitted " + s.getSubmittedAt().format(DTF));
            TextArea textArea = new TextArea(s.getSubmissionText() == null ? "" : s.getSubmissionText());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(4);

            VBox attachmentBox = new VBox(6);
            if (s.getAttachmentName() != null && !s.getAttachmentName().isBlank()) {
                Button download = UIComponents.ghostButton("📎 " + s.getAttachmentName() + " • " + formatFileSize(s.getAttachmentSize()));
                download.setOnAction(e -> {
                    try {
                        Path source = Path.of(s.getAttachmentPath());
                        Path destination = new DownloadService().download(source, s.getAttachmentName(),
                                assignment.getTitle() + " — " + s.getStudentName(), "Assignment Submission");
                        Toast.success("File downloaded", destination.getFileName().toString());
                    } catch (Exception ex) {
                        Toast.error("Could not download file", ex.getMessage());
                    }
                });
                attachmentBox.getChildren().add(download);
            }

            Spinner<Integer> gradeSpinner = new Spinner<>(0, assignment.getMaxMarks(), s.getGrade() != null ? s.getGrade() : 0);
            gradeSpinner.setEditable(true);
            TextField feedbackField = new TextField(s.getFeedback() != null ? s.getFeedback() : "");
            feedbackField.setPromptText("Feedback for student");
            Button gradeBtn = UIComponents.primaryButton("Save Grade");
            gradeBtn.setOnAction(e -> {
                runDbAction(() -> assignmentService.grade(s.getId(), gradeSpinner.getValue(), feedbackField.getText(),
                                s.getStudentId(), assignment.getTitle()),
                        () -> { Toast.success("Grade saved", s.getStudentName() + " has been notified."); shell.openGrading(assignmentId); },
                        "Could not save grade");
            });

            HBox gradeRow = new HBox(10, new Label("Grade (/" + assignment.getMaxMarks() + "):") {{ getStyleClass().add("muted-text"); }},
                    gradeSpinner, feedbackField, gradeBtn);
            gradeRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(feedbackField, Priority.ALWAYS);

            card.getChildren().addAll(header, submittedAt, textArea, attachmentBox, gradeRow);
            root.getChildren().add(card);
        }
        return root;
    }

    // ============================= STUDENTS =============================

    public static Node students(DashboardShell shell, User instructor) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Students"));
        root.getChildren().add(UIComponents.pageSubtitle("View enrolled students and their lesson and quiz progress."));

        List<Course> courses = courseService.getCoursesByInstructor(instructor.getId());
        for (Course c : courses) {
            VBox card = UIComponents.card(UIComponents.sectionTitle(c.getTitle()));
            List<Enrollment> enrollments = enrollmentService.getCourseEnrollments(c.getId());
            if (enrollments.isEmpty()) {
                card.getChildren().add(muted("No students enrolled yet."));
            } else {
                for (Enrollment en : enrollments) {
                    HBox row = new HBox(14);
                    row.setAlignment(Pos.CENTER_LEFT);
                    double pct = enrollmentService.progressFor(en.getId(), c.getId());
                    ProgressBar bar = UIComponents.progress(pct / 100.0);
                    bar.setPrefWidth(160);
                    String studentName = en.getStudentName() == null || en.getStudentName().isBlank()
                            ? "Student #" + en.getStudentId() : en.getStudentName();
                    Button studentBtn = UIComponents.ghostButton(studentName);
                    studentBtn.setOnAction(e -> showStudentProgressDialog(shell, en, c));
                    studentBtn.setMinWidth(180);
                    row.getChildren().addAll(studentBtn, bar,
                            muted(String.format("%.0f%% lesson progress", pct)),
                            UIComponents.badge(en.getStatus().name(), en.getStatus() == Enrollment.Status.COMPLETED ? "badge-success" : "badge-info"));
                    card.getChildren().add(row);
                }
            }
            root.getChildren().add(card);
        }
        if (courses.isEmpty()) root.getChildren().add(UIComponents.emptyState("No courses yet", "Create a course to see enrolled students."));
        return root;
    }

    private static void showStudentProgressDialog(DashboardShell shell, Enrollment enrollment, Course course) {
        int totalLessons = courseService.countLessons(course.getId());
        int completedLessons = enrollmentService.completedLessonIds(enrollment.getId()).size();
        double lessonPct = totalLessons == 0 ? 0 : completedLessons * 100.0 / totalLessons;
        List<Quiz> courseQuizzes = quizService.getQuizzesForCourse(course.getId());
        long publishedCount = courseQuizzes.stream().filter(Quiz::isPublished).count();
        List<com.lms.model.QuizAttempt> attempts = quizService.getAttemptsForStudentInCourse(enrollment.getStudentId(), course.getId());

        Dialog<ButtonType> dialog = new Dialog<>();
        UIComponents.styleDialog(dialog);
        dialog.initOwner(shell.getSceneManager().getStage());
        dialog.setTitle("Student Progress");
        dialog.getDialogPane().getStylesheets().add(InstructorPages.class.getResource("/com/lms/css/theme.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label name = UIComponents.pageTitle(enrollment.getStudentName() == null ? "Student" : enrollment.getStudentName());
        Label courseLabel = new Label("Course: " + course.getTitle());
        courseLabel.getStyleClass().add("muted-text");
        ProgressBar lessonBar = UIComponents.progress(lessonPct / 100.0);
        lessonBar.setPrefWidth(430);
        VBox lessonCard = UIComponents.card(UIComponents.sectionTitle("Lesson Progress"),
                new Label(completedLessons + " of " + totalLessons + " lessons completed"), lessonBar,
                muted(String.format("%.0f%% complete", lessonPct)));

        VBox quizCard = UIComponents.card(UIComponents.sectionTitle("Quiz Progress"),
                new Label(attempts.size() + " quiz attempt(s) completed out of " + publishedCount + " published quiz(es)"));
        if (attempts.isEmpty()) {
            quizCard.getChildren().add(muted("No completed quizzes yet."));
        } else {
            for (com.lms.model.QuizAttempt attempt : attempts) {
                double scorePct = attempt.getTotalMarks() == 0 ? 0 : attempt.getScore() * 100.0 / attempt.getTotalMarks();
                quizCard.getChildren().add(new Label(attempt.getQuizTitle() + "  •  " + String.format("%.0f%%", scorePct)));
            }
        }

        // Assignment progress: show how many course assignments the student has submitted/graded.
        List<Assignment> courseAssignments = assignmentService.getForCourse(course.getId());
        int submittedAssignments = 0;
        int gradedAssignments = 0;
        VBox assignmentCard = UIComponents.card(UIComponents.sectionTitle("Assignment Progress"),
                new Label("0 of " + courseAssignments.size() + " assignments submitted"));
        for (Assignment assignment : courseAssignments) {
            Optional<Submission> submission = assignmentService.findSubmission(assignment.getId(), enrollment.getStudentId());
            if (submission.isPresent()) {
                submittedAssignments++;
                if (submission.get().getGrade() != null) gradedAssignments++;
            }
        }
        assignmentCard.getChildren().clear();
        assignmentCard.getChildren().add(UIComponents.sectionTitle("Assignment Progress"));
        assignmentCard.getChildren().add(new Label(submittedAssignments + " of " + courseAssignments.size() + " assignments submitted"));
        if (courseAssignments.isEmpty()) {
            assignmentCard.getChildren().add(muted("No assignments for this course yet."));
        } else {
            assignmentCard.getChildren().add(muted(gradedAssignments + " graded"));
            for (Assignment assignment : courseAssignments) {
                Optional<Submission> submission = assignmentService.findSubmission(assignment.getId(), enrollment.getStudentId());
                String status;
                if (submission.isEmpty()) {
                    status = "Not submitted";
                } else if (submission.get().getGrade() != null) {
                    status = "Graded: " + submission.get().getGrade() + "/" + assignment.getMaxMarks();
                } else {
                    status = "Submitted";
                }
                assignmentCard.getChildren().add(new Label(assignment.getTitle() + "  •  " + status));
            }
        }

        VBox content = new VBox(16, name, courseLabel, lessonCard, quizCard, assignmentCard);
        content.setPadding(new Insets(20));
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // ============================= helpers =============================

    /** Runs instructor-side database work off the JavaFX Application Thread. */
    private static void runDbAction(CallableAction action, Runnable onSuccess, String title) {
        BackgroundTask.run(() -> {
            action.run();
            return null;
        }, ignored -> onSuccess.run(), error -> Toast.error(title, safeError(error)));
    }

    @FunctionalInterface
    private interface CallableAction {
        void run() throws Exception;
    }

    private static javafx.util.StringConverter<Course> courseConverter() {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(Course c) { return c == null ? "" : c.getTitle(); }
            @Override public Course fromString(String s) { return null; }
        };
    }

    private static Label strong(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: -text; -fx-font-weight: 700; -fx-font-size: 13px;");
        return l;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted-text");
        return l;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String firstName(String fullName) {
        return fullName == null ? "" : fullName.split(" ")[0];
    }
}
