package com.lms.ui;

import com.lms.model.*;
import com.lms.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.*;
import javafx.util.Duration;

import com.lms.util.BackgroundTask;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StudentPages {

    private static final CourseService courseService = new CourseService();
    private static final EnrollmentService enrollmentService = new EnrollmentService();
    private static final QuizService quizService = new QuizService();
    private static final AssignmentService assignmentService = new AssignmentService();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private StudentPages() {}

    // ============================= DASHBOARD =============================

    public static Node dashboard(DashboardShell shell, User student) {
        VBox root = new VBox(24);

        List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student.getId());
        double avgProgress = enrollments.stream().mapToDouble(Enrollment::getProgressPercent).average().orElse(0);
        List<Assignment> upcoming = assignmentService.getForStudent(student.getId()).stream()
                .filter(a -> !a.isOverdue()).limit(3).toList();
        int quizzesTaken = quizService.getAttemptsForStudent(student.getId()).size();

        root.getChildren().add(heroWelcomeCard(student, enrollments.size(), avgProgress));

        HBox stats = new HBox(18,
                UIComponents.statCard("Total Courses", enrollments.size(), "#287BEF", "📚", null,
                        () -> shell.navigate("My Courses")),
                UIComponents.statCardDecimal("Learning Progress", String.format("%.0f%%", avgProgress), "#22C55E", "📈", null,
                        () -> shell.navigate("My Courses")),
                UIComponents.statCard("Assignments", upcoming.size(), "#FFC857", "📄", null,
                        () -> shell.navigate("Assignments")),
                UIComponents.statCard("Quizzes Taken", quizzesTaken, "#7B61FF", "📝", null,
                        () -> shell.navigate("Quiz Results"))
        );

        VBox continueCard = UIComponents.card(UIComponents.sectionTitle("Continue Learning"));
        VBox continueList = new VBox(14);
        List<Enrollment> inProgress = enrollments.stream().filter(en -> en.getStatus() != Enrollment.Status.COMPLETED).limit(4).toList();
        for (Enrollment e : inProgress) {
            continueList.getChildren().add(continueLearningRow(shell, e));
        }
        if (inProgress.isEmpty()) continueList.getChildren().add(UIComponents.emptyState("No courses yet", "Browse courses to get started."));
        continueCard.getChildren().add(continueList);
        HBox.setHgrow(continueCard, Priority.ALWAYS);

        VBox profileWidget = learningProfileWidget(student, enrollments, quizzesTaken, avgProgress);

        HBox topRow = new HBox(20, continueCard, profileWidget);
        continueCard.setPrefWidth(560);
        profileWidget.setPrefWidth(300);
        profileWidget.setMinWidth(280);

        VBox upcomingCard = UIComponents.card(UIComponents.sectionTitle("Upcoming Assignments"));
        VBox upList = new VBox(10);
        for (Assignment a : upcoming) {
            HBox row = new HBox(12, strong(a.getTitle()), UIComponents.spacer(), muted("Due " + a.getDeadline().format(DTF)));
            row.setAlignment(Pos.CENTER_LEFT);
            upList.getChildren().add(row);
        }
        if (upcoming.isEmpty()) upList.getChildren().add(UIComponents.emptyState("Nothing due soon", "You're all caught up."));
        upcomingCard.getChildren().add(upList);

        root.getChildren().addAll(stats, topRow, upcomingCard);
        return root;
    }

    /** Bright hero "welcome" card with a greeting, a short encouragement line, and a soft 3D-style illustration. */
    private static VBox heroWelcomeCard(User student, int courseCount, double avgProgress) {
        VBox hero = new VBox(14);
        hero.getStyleClass().add("hero-card");
        hero.setPadding(new Insets(28, 32, 28, 32));

        Label greeting = new Label(timeOfDayGreeting() + ", " + firstName(student.getFullName()) + " 👋");
        greeting.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: 800;");
        Label sub = new Label(courseCount == 0
                ? "Browse the catalog and start your learning journey today."
                : "Continue your learning journey and reach your goals.");
        sub.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px;");
        sub.setWrapText(true);

        VBox textCol = new VBox(8, greeting, sub);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        StackPane illustration = Illustrations.learnerComposition(150);

        HBox row = new HBox(20, textCol, illustration);
        row.setAlignment(Pos.CENTER_LEFT);
        hero.getChildren().add(row);
        return hero;
    }

    private static String timeOfDayGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    /** Rich "Continue Learning" row: topic glyph, course/instructor info, animated progress bar and a Continue button. */
    private static HBox continueLearningRow(DashboardShell shell, Enrollment e) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));

        StackPane glyph = Illustrations.topicGlyph(emojiForCourse(e.getCourseTitle()), "#287BEF", 46);

        VBox info = new VBox(4);
        Label title = strong(e.getCourseTitle());
        ProgressBar bar = UIComponents.progress(e.getProgressPercent() / 100.0);
        bar.setPrefWidth(220);
        HBox progressRow = new HBox(10, bar, muted(String.format("%.0f%%", e.getProgressPercent())));
        progressRow.setAlignment(Pos.CENTER_LEFT);
        info.getChildren().addAll(title, progressRow);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button continueBtn = UIComponents.primaryButton("Continue →");
        continueBtn.setOnAction(ev -> shell.openLearning(e.getCourseId()));

        row.getChildren().addAll(glyph, info, continueBtn);
        return row;
    }

    /** Right-side "Your Learning Profile" widget: avatar, name and a few quick learner stats. */
    private static VBox learningProfileWidget(User student, List<Enrollment> enrollments, int quizzesTaken, double avgProgress) {
        VBox card = new VBox(16);
        card.getStyleClass().add("profile-widget-card");
        card.setPadding(new Insets(20));

        Label heading = UIComponents.sectionTitle("Your Learning Profile");

        HBox who = new HBox(12, UIComponents.avatarWithInitials(student.initials(), 52));
        VBox nameBox = new VBox(2);
        Label name = strong(student.getFullName());
        Label role = muted(student.getRole().display());
        nameBox.getChildren().addAll(name, role);
        who.getChildren().add(nameBox);
        who.setAlignment(Pos.CENTER_LEFT);

        long completed = enrollments.stream().filter(en -> en.getStatus() == Enrollment.Status.COMPLETED).count();

        VBox statsList = new VBox(10,
                profileStatRow("📚", "Courses enrolled", String.valueOf(enrollments.size())),
                profileStatRow("🏆", "Completed courses", String.valueOf(completed)),
                profileStatRow("📈", "Overall progress", String.format("%.0f%%", avgProgress)),
                profileStatRow("📝", "Quizzes taken", String.valueOf(quizzesTaken))
        );

        card.getChildren().addAll(heading, who, new javafx.scene.control.Separator(), statsList);
        return card;
    }

    private static HBox profileStatRow(String icon, String label, String value) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 15px;");
        Label labelText = muted(label);
        Label valueText = strong(value);
        HBox row = new HBox(10, iconLabel, labelText, UIComponents.spacer(), valueText);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String emojiForCourse(String title) {
        if (title == null) return "📘";
        String t = title.toLowerCase();
        if (t.contains("java") || t.contains("python") || t.contains("programming") || t.contains("code")) return "💻";
        if (t.contains("data structure") || t.contains("algorithm")) return "🧩";
        if (t.contains("ai") || t.contains("machine learning") || t.contains("artificial")) return "🤖";
        if (t.contains("database") || t.contains("sql")) return "🗄️";
        if (t.contains("web")) return "🌐";
        if (t.contains("security") || t.contains("cyber")) return "🛡️";
        return "📘";
    }

    // ============================= BROWSE COURSES =============================

    public static Node browseCourses(DashboardShell shell, User student) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Browse Courses"));
        root.getChildren().add(UIComponents.pageSubtitle("Discover and enroll in new courses."));

        TextField search = new TextField();
        search.setPromptText("Search courses…");
        search.getStyleClass().add("search-field");
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "All", "Programming", "Web Development", "Data Science", "General"));
        categoryBox.setValue("All");
        ComboBox<String> difficultyBox = new ComboBox<>(FXCollections.observableArrayList("All", "BEGINNER", "INTERMEDIATE", "ADVANCED"));
        difficultyBox.setValue("All");
        HBox controls = new HBox(12, search, categoryBox, difficultyBox);
        controls.setAlignment(Pos.CENTER_LEFT);

        FlowPane grid = new FlowPane(18, 18);
        Runnable refresh = () -> {
            grid.getChildren().clear();
            List<Course> results = courseService.search(search.getText(), categoryBox.getValue(), difficultyBox.getValue())
                    .stream().filter(Course::isPublished).toList();
            if (results.isEmpty()) grid.getChildren().add(UIComponents.emptyState("No courses found", "Try adjusting your filters."));
            for (Course c : results) {
                boolean enrolled = enrollmentService.isEnrolled(student.getId(), c.getId());
                grid.getChildren().add(CourseCardFactory.studentCard(c, enrolled,
                        () -> {
                            try {
                                enrollmentService.enroll(student.getId(), c.getId(), c.getTitle());
                                Toast.success("Enrolled!", "You're now enrolled in \"" + c.getTitle() + "\".");
                                shell.navigate("Browse Courses");
                            } catch (EnrollmentService.EnrollmentException ex) {
                                Toast.warning("Already enrolled", ex.getMessage());
                            }
                        },
                        () -> shell.openLearning(c.getId())));
            }
        };
        search.textProperty().addListener((o, ov, nv) -> refresh.run());
        categoryBox.valueProperty().addListener((o, ov, nv) -> refresh.run());
        difficultyBox.valueProperty().addListener((o, ov, nv) -> refresh.run());
        refresh.run();

        root.getChildren().addAll(controls, grid);
        return root;
    }

    // ============================= MY COURSES =============================

    public static Node myCourses(DashboardShell shell, User student) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("My Courses"));
        root.getChildren().add(UIComponents.pageSubtitle("Your enrolled courses and progress."));

        List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student.getId());
        if (enrollments.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("You haven't enrolled in any courses", "Browse the catalog to get started."));
            return root;
        }
        FlowPane grid = new FlowPane(18, 18);
        for (Enrollment e : enrollments) {
            Course c = courseService.getCourse(e.getCourseId()).orElse(null);
            if (c == null) continue;
            VBox card = CourseCardFactory.studentCard(c, true, () -> {}, () -> shell.openLearning(c.getId()));
            ((VBox) card.getChildren().get(1)).getChildren().add(1,
                    UIComponents.badge(e.getStatus().name(), e.getStatus() == Enrollment.Status.COMPLETED ? "badge-success" : "badge-info"));
            grid.getChildren().add(card);
        }
        root.getChildren().add(grid);
        return root;
    }

    // ============================= QUIZZES =============================

    public static Node quizzes(DashboardShell shell, User student) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Quizzes"));
        root.getChildren().add(UIComponents.pageSubtitle("Published quizzes available in your enrolled courses."));

        List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(student.getId());
        if (enrollments.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("No enrolled courses", "Enroll in a course to see its quizzes."));
            return root;
        }

        VBox list = new VBox(14);
        int quizCount = 0;
        for (Enrollment enrollment : enrollments) {
            Course course = courseService.getCourse(enrollment.getCourseId()).orElse(null);
            if (course == null) continue;

            List<Quiz> quizzes = quizService.getQuizzesForCourse(course.getId()).stream()
                    .filter(Quiz::isPublished)
                    .toList();
            if (quizzes.isEmpty()) continue;

            VBox courseBox = UIComponents.card(
                    UIComponents.sectionTitle(course.getTitle() + " · Quizzes"));
            VBox quizRows = new VBox(8);

            for (Quiz quiz : quizzes) {
                quizCount++;
                boolean attempted = quizService.hasAttempted(student.getId(), quiz.getId());
                Label title = strong(quiz.getTitle());
                Label meta = muted(quiz.getDurationMinutes() + " min" + (attempted ? " · Attempted" : " · Not attempted"));
                VBox info = new VBox(3, title, meta);
                HBox.setHgrow(info, Priority.ALWAYS);

                Button action = UIComponents.primaryButton(attempted ? "View Result" : "Start Quiz");
                if (attempted) {
                    action.setOnAction(e -> shell.navigate("Quiz Results"));
                } else {
                    action.setOnAction(e -> shell.openQuiz(quiz.getId(), course.getId()));
                }

                HBox row = new HBox(14, info, action);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10));
                row.setStyle("-fx-background-color: rgba(40,123,239,0.05); -fx-background-radius: 10px;");
                quizRows.getChildren().add(row);
            }

            courseBox.getChildren().add(quizRows);
            list.getChildren().add(courseBox);
        }

        if (quizCount == 0) {
            root.getChildren().add(UIComponents.emptyState("No quizzes available", "Your enrolled courses do not have any published quizzes yet."));
        } else {
            root.getChildren().add(list);
        }
        return root;
    }

    // ============================= LEARNING PAGE =============================

    public static Node learning(DashboardShell shell, User student, long courseId) {
        Course course = courseService.getCourse(courseId).orElseThrow();
        Enrollment enrollment = enrollmentService.getStudentEnrollments(student.getId()).stream()
                .filter(e -> e.getCourseId() == courseId).findFirst()
                .orElseThrow(() -> new IllegalStateException("Not enrolled"));

        BorderPane layout = new BorderPane();
        Button back = UIComponents.ghostButton("← Back to My Courses");
        back.setOnAction(e -> shell.navigate("My Courses"));

        List<CourseModule> modules = courseService.getModules(courseId);
        VBox lessonNav = new VBox(6);
        lessonNav.setPrefWidth(260);
        lessonNav.getStyleClass().add("card");
        lessonNav.setPadding(new Insets(14));
        lessonNav.getChildren().add(UIComponents.sectionTitle("Modules"));

        VBox centerPane = new VBox(16);
        centerPane.setPadding(new Insets(0, 20, 0, 20));
        HBox.setHgrow(centerPane, Priority.ALWAYS);

        VBox progressPane = new VBox(12);
        progressPane.setPrefWidth(240);
        progressPane.getStyleClass().add("card");
        progressPane.setPadding(new Insets(16));

        Lesson[] currentLesson = new Lesson[1];

        Runnable[] renderLessonHolder = new Runnable[1];
        Runnable renderProgress = () -> {
            progressPane.getChildren().setAll(UIComponents.sectionTitle("Your Progress"));
            double pct = enrollmentService.progressFor(enrollment.getId(), courseId);
            ProgressBar bar = UIComponents.progress(pct / 100.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            Label pctLabel = strong(String.format("%.0f%% complete", pct));
            progressPane.getChildren().addAll(bar, pctLabel);
            if (pct >= 100) {
                progressPane.getChildren().add(UIComponents.badge("🎉 Course Completed!", "badge-success"));
            }
        };

        renderLessonHolder[0] = () -> {
            if (currentLesson[0] == null) {
                centerPane.getChildren().setAll(UIComponents.emptyState("Select a lesson", "Choose a lesson from the left to begin."));
                return;
            }
            Lesson lesson = currentLesson[0];
            Label title = UIComponents.pageTitle(lesson.getTitle());
            Label duration = muted(lesson.getDurationMinutes() + " min read");
            TextArea contentView = new TextArea(lesson.getContent());
            contentView.setEditable(false);
            contentView.setWrapText(true);
            contentView.setPrefRowCount(14);

            List<Material> materials = courseService.getMaterials(lesson.getId());
            VBox materialsBox = new VBox(6);
            if (!materials.isEmpty()) {
                materialsBox.getChildren().add(UIComponents.sectionTitle("Resources & Notes"));
                for (Material m : materials) {
                    HBox materialRow = new HBox(10);
                    materialRow.setAlignment(Pos.CENTER_LEFT);
                    materialRow.setPadding(new Insets(8));
                    materialRow.setStyle("-fx-background-color: rgba(40,123,239,0.05); -fx-background-radius: 8px;");
                    Label name = new Label((m.getType() == Material.Type.FILE ? "📎 " : "🔗 ") + m.getTitle());
                    name.setStyle("-fx-text-fill: -text; -fx-font-weight: 600;");
                    HBox.setHgrow(name, Priority.ALWAYS);
                    if (m.getType() == Material.Type.FILE && m.getUrlOrPath() != null && !m.getUrlOrPath().isBlank()) {
                        Button downloadBtn = UIComponents.ghostButton("Download");
                        downloadBtn.setOnAction(e -> {
                            try {
                                Path source = Path.of(m.getUrlOrPath());
                                Path downloaded = new DownloadService().download(source, source.getFileName().toString(), m.getTitle(), "Course Material");
                                Toast.success("File downloaded", downloaded.getFileName().toString());
                            } catch (Exception ex) {
                                Toast.error("Could not download file", ex.getMessage() == null ? "The attachment is unavailable." : ex.getMessage());
                            }
                        });
                        materialRow.getChildren().addAll(name, downloadBtn);
                    } else if (m.getType() == Material.Type.LINK && m.getUrlOrPath() != null && !m.getUrlOrPath().isBlank()) {
                        Button openBtn = UIComponents.ghostButton("Open Link");
                        openBtn.setOnAction(e -> {
                            try { Desktop.getDesktop().browse(java.net.URI.create(m.getUrlOrPath())); }
                            catch (Exception ex) { Toast.error("Could not open link", "Please check the resource URL."); }
                        });
                        materialRow.getChildren().addAll(name, openBtn);
                    } else {
                        materialRow.getChildren().add(name);
                    }
                    materialsBox.getChildren().add(materialRow);
                }
            }

            boolean completed = isLessonCompleted(enrollment.getId(), lesson.getId());
            Button completeBtn = UIComponents.primaryButton(completed ? "✓ Completed" : "Mark as Complete");
            completeBtn.setDisable(completed);
            completeBtn.setOnAction(e -> {
                enrollmentService.markLessonComplete(enrollment.getId(), lesson.getId());
                Toast.success("Lesson complete!", lesson.getTitle() + " marked as done.");
                shell.openLearning(courseId);
            });

            centerPane.getChildren().setAll(title, duration, contentView, materialsBox, completeBtn);
        };

        int moduleIndex = 1;
        for (CourseModule m : modules) {
            Label modTitle = strong(moduleIndex + ". " + m.getTitle());
            lessonNav.getChildren().add(modTitle);
            for (Lesson l : m.getLessons()) {
                boolean done = isLessonCompleted(enrollment.getId(), l.getId());
                Button lessonBtn = new Button((done ? "✓  " : "○  ") + l.getTitle());
                lessonBtn.getStyleClass().add("nav-item");
                lessonBtn.setMaxWidth(Double.MAX_VALUE);
                lessonBtn.setAlignment(Pos.CENTER_LEFT);
                lessonBtn.setOnAction(e -> {
                    currentLesson[0] = l;
                    renderLessonHolder[0].run();
                });
                lessonNav.getChildren().add(lessonBtn);
            }
            moduleIndex++;
        }
        if (modules.isEmpty()) lessonNav.getChildren().add(muted("No lessons yet."));
        else if (!modules.get(0).getLessons().isEmpty()) currentLesson[0] = modules.get(0).getLessons().get(0);

        renderProgress.run();
        renderLessonHolder[0].run();

        // Quizzes for this course
        VBox quizzesBox = new VBox(10);
        List<Quiz> quizzes = quizService.getQuizzesForCourse(courseId).stream().filter(Quiz::isPublished).toList();
        if (!quizzes.isEmpty()) {
            quizzesBox.getChildren().add(UIComponents.sectionTitle("Quizzes"));
            for (Quiz q : quizzes) {
                boolean attempted = quizService.hasAttempted(student.getId(), q.getId());
                HBox row = new HBox(10, strong(q.getTitle()), UIComponents.spacer());
                Button takeBtn = UIComponents.ghostButton(attempted ? "Attempted" : "Start Quiz");
                takeBtn.setDisable(attempted);
                takeBtn.setOnAction(e -> shell.openQuiz(q.getId(), courseId));
                row.getChildren().add(takeBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                quizzesBox.getChildren().add(row);
            }
        }
        progressPane.getChildren().add(quizzesBox);

        VBox header = new VBox(6, back, UIComponents.pageTitle(course.getTitle()));
        layout.setTop(header);
        layout.setLeft(lessonNav);
        layout.setCenter(centerPane);
        layout.setRight(progressPane);
        BorderPane.setMargin(header, new Insets(0, 0, 20, 0));

        return layout;
    }

    private static boolean isLessonCompleted(long enrollmentId, long lessonId) {
        return enrollmentService.completedLessonIds(enrollmentId).contains(lessonId);
    }

    // ============================= QUIZ TAKING =============================

    public static Node takeQuiz(DashboardShell shell, User student, long quizId, long courseId) {
        Quiz quiz = quizService.getQuizWithQuestions(quizId).orElseThrow();

        // Start (or resume) the server-side clock. Re-opening a quiz never resets the timer.
        final Instant startedAt;
        try {
            startedAt = quizService.beginAttempt(student.getId(), quizId);
        } catch (QuizService.QuizException | com.lms.security.AuthorizationException ex) {
            Toast.error("Quiz unavailable", ex.getMessage());
            return UIComponents.emptyState("Quiz unavailable", ex.getMessage());
        }
        final int totalSeconds = quiz.getDurationMinutes() * 60;
        final long elapsedAtOpen = Math.max(0, Instant.now().getEpochSecond() - startedAt.getEpochSecond());
        final int initialRemaining = (int) Math.max(0, Math.min(totalSeconds, totalSeconds - elapsedAtOpen));

        VBox root = new VBox(20);

        Label title = UIComponents.pageTitle(quiz.getTitle());
        Label timerLabel = new Label(String.format("Time remaining: %02d:%02d", initialRemaining / 60, initialRemaining % 60));
        timerLabel.getStyleClass().add("muted-text");

        Map<Long, Character> answers = new HashMap<>();
        VBox questionsBox = new VBox(16);
        int i = 1;
        for (QuizQuestion q : quiz.getQuestions()) {
            VBox qCard = UIComponents.card();
            Label qLabel = strong(i + ". " + q.getQuestionText());
            qLabel.setWrapText(true);
            ToggleGroup group = new ToggleGroup();
            VBox options = new VBox(6);
            for (char opt : new char[]{'A','B','C','D'}) {
                RadioButton rb = new RadioButton(opt + ". " + q.optionText(opt));
                rb.setStyle("-fx-text-fill: -text;");
                rb.setToggleGroup(group);
                rb.setOnAction(e -> answers.put(q.getId(), opt));
                options.getChildren().add(rb);
            }
            qCard.getChildren().addAll(qLabel, options, muted(q.getMarks() + " mark(s)"));
            questionsBox.getChildren().add(qCard);
            i++;
        }

        Button submitBtn = UIComponents.primaryButton("Submit Quiz");
        final boolean[] finished = {false};
        final Timeline[] timelineRef = {null};

        // Shared by the manual Submit button and the automatic time-out. Grading and the database write
        // run on a background worker; the result screen is shown back on the FX thread.
        Runnable submitNow = () -> {
            if (finished[0]) return;
            finished[0] = true;
            submitBtn.setDisable(true);
            if (timelineRef[0] != null) timelineRef[0].stop();
            final Map<Long, Character> snapshot = new HashMap<>(answers); // immutable view for the worker thread
            BackgroundTask.run(
                    () -> quizService.submitAttempt(student.getId(), quiz, snapshot),
                    attempt -> showQuizResult(shell, attempt, quiz, courseId),
                    ex -> {
                        finished[0] = false;
                        submitBtn.setDisable(false);
                        String msg = (ex instanceof QuizService.QuizException || ex instanceof com.lms.security.AuthorizationException)
                                && ex.getMessage() != null ? ex.getMessage()
                                : "Your answers could not be saved. Please check the connection and try again.";
                        Toast.error("Could not submit", msg);
                    });
        };

        submitBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Submit your answers? This cannot be changed afterward.");
            UIComponents.styleDialog(confirm);
            confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> submitNow.run());
        });

        // Countdown that is actually enforced: when it reaches zero the quiz is submitted automatically.
        // It stops itself if the student navigates away (the quiz page is no longer in a scene), while
        // the server-side clock in QuizService keeps running so leaving the page cannot reset the timer.
        final int[] remaining = {initialRemaining};
        final boolean[] wasAttached = {false};
        Runnable onTimeUp = () -> {
            Toast.warning("Time's up", "Your quiz was submitted automatically.");
            submitNow.run();
        };
        if (initialRemaining <= 0) {
            Platform.runLater(onTimeUp);
        } else {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (root.getScene() != null) wasAttached[0] = true;
                else if (wasAttached[0]) { if (timelineRef[0] != null) timelineRef[0].stop(); return; }
                if (finished[0]) return;
                remaining[0]--;
                int m = Math.max(remaining[0], 0) / 60, sec = Math.max(remaining[0], 0) % 60;
                timerLabel.setText(String.format("Time remaining: %02d:%02d", m, sec));
                if (remaining[0] <= 0) onTimeUp.run();
            }));
            timeline.setCycleCount(initialRemaining);
            timelineRef[0] = timeline;
            timeline.play();
        }

        root.getChildren().addAll(title, timerLabel, questionsBox, submitBtn);
        return root;
    }

    private static void showQuizResult(DashboardShell shell, QuizAttempt attempt, Quiz quiz, long courseId) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Quiz Results — " + quiz.getTitle()));
        double pct = attempt.percentage();
        VBox scoreCard = UIComponents.card(
                UIComponents.sectionTitle(String.format("Score: %.0f / %.0f (%.0f%%)", attempt.getScore(), attempt.getTotalMarks(), pct)),
                UIComponents.progress(pct / 100.0)
        );
        VBox breakdown = new VBox(10);
        int i = 1;
        for (QuizQuestion q : quiz.getQuestions()) {
            QuizAnswer ans = attempt.getAnswers().stream().filter(a -> a.getQuestionId() == q.getId()).findFirst().orElse(null);
            boolean correct = ans != null && ans.isCorrect();
            Label label = new Label((correct ? "✓ " : "✗ ") + i + ". " + q.getQuestionText());
            label.setStyle("-fx-text-fill: " + (correct ? "-success" : "-error") + "; -fx-font-size: 12.5px;");
            label.setWrapText(true);
            Label correctAns = muted("Correct answer: " + q.getCorrectOption() + ". " + q.optionText(q.getCorrectOption()));
            breakdown.getChildren().addAll(label, correctAns);
            i++;
        }
        Button doneBtn = UIComponents.primaryButton("Back to Course");
        doneBtn.setOnAction(e -> shell.openLearning(courseId));
        root.getChildren().addAll(scoreCard, UIComponents.card(UIComponents.sectionTitle("Answer Breakdown"), breakdown), doneBtn);

        shell.showTransientContent(root);
    }

    // ============================= QUIZ RESULTS (list) =============================

    public static Node quizResults(DashboardShell shell, User student) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Quiz Results"));
        root.getChildren().add(UIComponents.pageSubtitle("Your past quiz attempts and scores."));

        List<QuizAttempt> attempts = quizService.getAttemptsForStudent(student.getId());
        if (attempts.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("No quiz attempts yet", "Take a quiz from one of your courses."));
            return root;
        }
        for (QuizAttempt a : attempts) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14));
            row.getStyleClass().add("card");
            Label title = strong(a.getQuizTitle());
            title.setPrefWidth(220);
            row.getChildren().addAll(title,
                    UIComponents.badge(String.format("%.0f%%", a.percentage()), a.percentage() >= 60 ? "badge-success" : "badge-error"),
                    muted(a.getSubmittedAt() != null ? a.getSubmittedAt().format(DTF) : ""));
            root.getChildren().add(row);
        }
        return root;
    }

    // ============================= ASSIGNMENTS =============================

    public static Node assignments(DashboardShell shell, User student) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Assignments"));
        root.getChildren().add(UIComponents.pageSubtitle("View and submit your coursework."));

        List<Assignment> list = assignmentService.getForStudent(student.getId());
        if (list.isEmpty()) {
            root.getChildren().add(UIComponents.emptyState("No assignments yet", "Enroll in a course to see assignments."));
            return root;
        }
        for (Assignment a : list) {
            Optional<Submission> sub = assignmentService.findSubmission(a.getId(), student.getId());
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14));
            row.getStyleClass().add("card");
            Label title = strong(a.getTitle());
            title.setPrefWidth(200);
            String status = sub.isPresent() ? sub.get().getStatus().name() : (a.isOverdue() ? "OVERDUE" : "PENDING");
            String badgeClass = sub.isPresent() ? (sub.get().getStatus() == Submission.Status.GRADED ? "badge-success" : "badge-info")
                    : (a.isOverdue() ? "badge-error" : "badge-warning");
            Button viewBtn = UIComponents.ghostButton("View →");
            viewBtn.setOnAction(e -> shell.openAssignmentDetail(a.getId()));
            row.getChildren().addAll(title, muted("Due " + a.getDeadline().format(DTF)),
                    UIComponents.badge(status, badgeClass), UIComponents.spacer(), viewBtn);
            root.getChildren().add(row);
        }
        return root;
    }

    public static Node assignmentDetail(DashboardShell shell, User student, long assignmentId) {
        Assignment a = assignmentService.getById(assignmentId).orElseThrow();
        VBox root = new VBox(20);
        Button back = UIComponents.ghostButton("← Back to Assignments");
        back.setOnAction(e -> shell.navigate("Assignments"));
        root.getChildren().addAll(back, UIComponents.pageTitle(a.getTitle()));

        VBox infoCard = UIComponents.card(
                UIComponents.sectionTitle("Instructions"),
                new Label(a.getInstructions() == null ? a.getDescription() : a.getInstructions()) {{ setWrapText(true); }},
                muted("Deadline: " + a.getDeadline().format(DTF) + "   •   Max marks: " + a.getMaxMarks())
        );
        root.getChildren().add(infoCard);

        Optional<Submission> existing = assignmentService.findSubmission(assignmentId, student.getId());
        if (existing.isPresent()) {
            Submission s = existing.get();
            VBox subCard = UIComponents.card(
                    UIComponents.sectionTitle("Your Submission"),
                    new Label(s.getSubmissionText()) {{ setWrapText(true); }},
                    UIComponents.badge(s.getStatus().name(), s.getStatus() == Submission.Status.GRADED ? "badge-success" : "badge-info")
            );
            if (s.getAttachmentName() != null && !s.getAttachmentName().isBlank()) {
                subCard.getChildren().add(muted("📎 " + s.getAttachmentName() + " • " + formatFileSize(s.getAttachmentSize())));
            }
            if (s.getStatus() == Submission.Status.GRADED) {
                subCard.getChildren().addAll(
                        strong("Grade: " + s.getGrade() + " / " + a.getMaxMarks()),
                        muted("Feedback: " + (s.getFeedback() == null || s.getFeedback().isBlank() ? "No feedback provided." : s.getFeedback()))
                );
            }
            root.getChildren().add(subCard);
        } else {
            TextArea submissionArea = new TextArea();
            submissionArea.setPromptText("Write or paste your submission here…");
            submissionArea.setPrefRowCount(8);
            submissionArea.setWrapText(true);

            Label selectedFile = new Label("No PDF attached");
            selectedFile.getStyleClass().add("muted-text");
            final File[] attachment = { null };
            Button choosePdf = UIComponents.ghostButton("📎 Attach PDF");
            choosePdf.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Attach Assignment PDF");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf", "*.PDF"));
                Window window = choosePdf.getScene() == null ? null : choosePdf.getScene().getWindow();
                File file = chooser.showOpenDialog(window);
                if (file == null) return;
                long maxBytes = 10L * 1024 * 1024;
                if (file.length() > maxBytes) {
                    Toast.error("PDF too large", "Maximum allowed size is 10 MB.");
                    return;
                }
                attachment[0] = file;
                selectedFile.setText(file.getName() + " • " + formatFileSize(file.length()));
                selectedFile.getStyleClass().remove("muted-text");
            });

            Label limit = new Label("PDF only • Maximum size: 10 MB");
            limit.getStyleClass().add("muted-text");
            Button submitBtn = UIComponents.primaryButton("Submit Assignment");
            submitBtn.setOnAction(e -> {
                final File chosen = attachment[0];
                final String text = submissionArea.getText();
                final boolean overdue = a.isOverdue();
                submitBtn.setDisable(true);

                // Copying the PDF and writing to the database can take a moment, so both happen on a
                // background worker; the UI is updated afterwards on the FX thread.
                BackgroundTask.run(() -> {
                    String attachmentPath = null;
                    String attachmentName = null;
                    long attachmentSize = 0;
                    if (chosen != null) {
                        if (!chosen.getName().toLowerCase().endsWith(".pdf")) {
                            throw new AssignmentService.AssignmentException("Only PDF files can be attached.");
                        }
                        if (chosen.length() > 10L * 1024 * 1024) {
                            throw new AssignmentService.AssignmentException("The PDF is larger than the 10 MB limit.");
                        }
                        try {
                            Path uploadDir = Path.of(System.getProperty("user.home"), ".lumenlms", "uploads", "submissions");
                            Files.createDirectories(uploadDir);
                            String safeName = chosen.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
                            String storedName = student.getId() + "_" + assignmentId + "_" + System.currentTimeMillis() + "_" + safeName;
                            Path target = uploadDir.resolve(storedName);
                            Files.copy(chosen.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                            attachmentPath = target.toString();
                            attachmentName = chosen.getName();
                            attachmentSize = chosen.length();
                        } catch (IOException io) {
                            throw new AssignmentService.AssignmentException("The PDF could not be saved. Please try again.");
                        }
                    }
                    try {
                        return assignmentService.submit(assignmentId, student.getId(), text, overdue,
                                attachmentPath, attachmentName, attachmentSize);
                    } catch (RuntimeException ex) {
                        // The database write failed after the file was copied: remove only this upload.
                        if (attachmentPath != null) {
                            try { Files.deleteIfExists(Path.of(attachmentPath)); } catch (IOException ignored) { }
                        }
                        throw ex;
                    }
                }, submission -> {
                    // The submission is stored. Do not treat a refresh problem as a failed submission.
                    Toast.success("Submitted!", "Your assignment has been submitted.");
                    try {
                        shell.openAssignmentDetail(assignmentId);
                    } catch (RuntimeException refreshEx) {
                        Toast.error("Submitted, but could not refresh",
                                "Your submission was saved. Please open Assignments again.");
                    }
                }, ex -> {
                    submitBtn.setDisable(false);
                    String message = ex.getMessage();
                    if (!(ex instanceof AssignmentService.AssignmentException
                            || ex instanceof com.lms.security.AuthorizationException)
                            || message == null || message.isBlank()) {
                        message = "Please check your database connection and try again.";
                    }
                    Toast.error("Submission failed", message);
                });
            });
            root.getChildren().add(UIComponents.card(UIComponents.sectionTitle("Submit Your Work"), submissionArea,
                    new HBox(10, choosePdf, selectedFile), limit, submitBtn));
        }
        return root;
    }

    // ============================= helpers =============================

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

    private static String firstName(String fullName) {
        return fullName == null ? "" : fullName.split(" ")[0];
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
