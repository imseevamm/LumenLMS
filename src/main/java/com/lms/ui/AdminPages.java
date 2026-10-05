package com.lms.ui;

import com.lms.model.Assignment;
import com.lms.model.Course;
import com.lms.model.Enrollment;
import com.lms.model.Quiz;
import com.lms.model.QuizAttempt;
import com.lms.model.Role;
import com.lms.model.Submission;
import com.lms.model.User;
import com.lms.service.Countable;
import com.lms.service.CourseService;
import com.lms.service.DownloadService;
import com.lms.service.EnrollmentService;
import com.lms.service.QuizService;
import com.lms.service.SettingsService;
import com.lms.service.UserService;
import com.lms.util.BackgroundTask;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static page builders for the Administrator role. Kept in one file since pages share small helpers. */
public final class AdminPages {

    private static final UserService userService = new UserService();
    private static final CourseService courseService = new CourseService();
    private static final EnrollmentService enrollmentService = new EnrollmentService();
    private static final com.lms.service.AssignmentService assignmentService = new com.lms.service.AssignmentService();
    private static final SettingsService settingsService = new SettingsService();

    private AdminPages() {}

    // ============================= DASHBOARD =============================

    /** Everything the dashboard shows, read on a worker thread and handed to the UI as an immutable snapshot. */
    private record DashboardData(int totalUsers, int students, int instructors, int publishedCourses,
                                 double completionRate, Map<String, Integer> counts, List<User> recentUsers) {}

    public static Node dashboard(DashboardShell shell) {
        VBox root = new VBox(24);

        VBox header = new VBox(4);
        header.getChildren().addAll(UIComponents.pageTitle("Admin Dashboard"),
                UIComponents.pageSubtitle("Platform-wide overview and key metrics."));

        VBox content = new VBox(24, new ProgressIndicator());
        root.getChildren().addAll(header, content);

        BackgroundTask.run(AdminPages::loadDashboardData,
                data -> content.getChildren().setAll(buildDashboardContent(shell, data)),
                ex -> showLoadFailure(content, "the dashboard", ex));
        return root;
    }

    private static DashboardData loadDashboardData() {
        Map<String, Countable> sources = new LinkedHashMap<>();
        sources.put("Courses", courseService);
        sources.put("Enrollments", enrollmentService);
        sources.put("Assignments", assignmentService);
        Map<String, Integer> counts = new LinkedHashMap<>();
        sources.forEach((label, source) -> counts.put(label, source.countAll()));

        List<User> users = userService.getAllUsers();
        return new DashboardData(users.size(),
                userService.countByRole(Role.STUDENT),
                userService.countByRole(Role.INSTRUCTOR),
                courseService.getPublishedCourses().size(),
                enrollmentService.completionRate(),
                Collections.unmodifiableMap(counts),
                users.stream().limit(5).toList());
    }

    private static VBox buildDashboardContent(DashboardShell shell, DashboardData data) {
        HBox statsRow = new HBox(18);
        statsRow.getChildren().addAll(
                UIComponents.statCard("Total Users", data.totalUsers(), "#287BEF", "👤", null, () -> shell.navigate("Users")),
                UIComponents.statCard("Students", data.students(), "#4B8FF7", "🎓", null, () -> shell.navigateUsers(Role.STUDENT)),
                UIComponents.statCard("Instructors", data.instructors(), "#7B61FF", "🧑‍🏫", null, () -> shell.navigateUsers(Role.INSTRUCTOR)),
                UIComponents.statCard("Courses", data.counts().get("Courses"), "#FFC857", "📚", null, () -> shell.navigate("Courses")),
                UIComponents.statCard("Enrollments", data.counts().get("Enrollments"), "#22C55E", "📈", null, () -> shell.navigate("Analytics"))
        );

        HBox statsRow2 = new HBox(18);
        statsRow2.getChildren().addAll(
                UIComponents.statCardDecimal("Completion Rate", String.format("%.0f%%", data.completionRate()), "#22C55E", "✅", null, () -> shell.navigate("Analytics")),
                UIComponents.statCard("Assignments", data.counts().get("Assignments"), "#E5484D", "📄", null, () -> shell.navigate("Analytics")),
                UIComponents.statCard("Published Courses", data.publishedCourses(), "#287BEF", "🚀", null, () -> shell.navigate("Courses"))
        );

        VBox recentUsersCard = UIComponents.card(UIComponents.sectionTitle("Recent Registrations"));
        VBox recentList = new VBox(10);
        for (User u : data.recentUsers()) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(e -> shell.navigate("Users"));
            row.setAccessibleText("Open user management for " + u.getFullName());
            row.getChildren().addAll(
                    UIComponents.avatarWithInitials(u.initials(), 34),
                    new VBox(0, labelStrong(u.getFullName()), mutedLabel(u.getEmail())),
                    UIComponents.spacer(),
                    UIComponents.badge(u.getRole().display(), badgeClassFor(u.getRole()))
            );
            recentList.getChildren().add(row);
        }
        if (data.recentUsers().isEmpty()) recentList.getChildren().add(UIComponents.emptyState("No users yet", "Registrations will appear here."));
        recentUsersCard.getChildren().add(recentList);

        return new VBox(24, statsRow, statsRow2, recentUsersCard);
    }

    private static void showLoadFailure(VBox target, String what, Throwable ex) {
        target.getChildren().setAll(UIComponents.emptyState("Could not load " + what,
                "Check the database connection and try again."));
        Toast.error("Load failed", ex.getMessage() != null ? ex.getMessage() : "Unexpected error.");
    }

    // ============================= USER MANAGEMENT =============================

    public static Node users(DashboardShell shell) {
        return users(shell, null);
    }

    /** User management page. A non-null initialRole is used when a dashboard role card opens this page. */
    public static Node users(DashboardShell shell, Role initialRole) {
        VBox root = new VBox(18);
        root.getChildren().add(UIComponents.pageTitle("User Management"));
        root.getChildren().add(UIComponents.pageSubtitle("Manage students, instructors, and administrators from one clean workspace."));

        TextField search = new TextField();
        search.setPromptText("Search by name or email…");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(300);

        ComboBox<String> roleFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All Users", "Students", "Instructors", "Administrators"
        ));
        roleFilter.setPrefWidth(165);
        roleFilter.setValue(roleFilterLabel(initialRole));

        Button addUserBtn = UIComponents.primaryButton("+ Add User");
        HBox controls = new HBox(12, search, roleFilter, UIComponents.spacer(), addUserBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        TableView<User> userTable = createUserTable(shell);
        TableView<User> adminTable = createUserTable(shell);

        Label usersHeading = UIComponents.sectionTitle("Students & Instructors");
        Label adminsHeading = UIComponents.sectionTitle("Administrators");
        VBox userSection = UIComponents.card(usersHeading, userTable);
        VBox adminSection = UIComponents.card(adminsHeading, adminTable);
        userSection.getStyleClass().add("table-card");
        adminSection.getStyleClass().add("table-card");

        VBox tableArea = new VBox(14, userSection, adminSection);
        VBox.setVgrow(userTable, Priority.ALWAYS);
        VBox.setVgrow(adminTable, Priority.ALWAYS);

        // Each refresh loads users on a worker thread. The ticket makes sure a slow, older query can never
        // overwrite the results of a newer one (e.g. while the user is still typing in the search box).
        AtomicInteger requestTicket = new AtomicInteger();
        Runnable refresh = () -> {
            final int ticket = requestTicket.incrementAndGet();
            final String selected = roleFilter.getValue();
            final Role filter = roleFromLabel(selected);
            final String keyword = search.getText();
            BackgroundTask.run(() -> userService.search(keyword, filter),
                    results -> {
                        if (ticket != requestTicket.get()) return;
                        applyUserResults(selected, filter, results, userTable, adminTable, userSection, adminSection);
                    },
                    ex -> Toast.error("Could not load users",
                            ex.getMessage() != null ? ex.getMessage() : "Please check the database connection."));
        };

        search.textProperty().addListener((o, ov, nv) -> refresh.run());
        roleFilter.valueProperty().addListener((o, ov, nv) -> refresh.run());
        wireUserTableActions(userTable, shell, refresh);
        wireUserTableActions(adminTable, shell, refresh);
        addUserBtn.setOnAction(e -> openAddUserDialog(shell, refresh));

        refresh.run();
        root.getChildren().addAll(controls, tableArea);
        VBox.setVgrow(tableArea, Priority.ALWAYS);
        return root;
    }

    private static void applyUserResults(String selected, Role filter, List<User> results,
                                         TableView<User> userTable, TableView<User> adminTable,
                                         VBox userSection, VBox adminSection) {
        if ("All Users".equals(selected)) {
            List<User> regularUsers = results.stream().filter(u -> u.getRole() != Role.ADMIN).toList();
            List<User> admins = results.stream().filter(u -> u.getRole() == Role.ADMIN).toList();
            userTable.getItems().setAll(regularUsers);
            adminTable.getItems().setAll(admins);
            userSection.setVisible(true);
            userSection.setManaged(true);
            adminSection.setVisible(true);
            adminSection.setManaged(true);
        } else if (filter == Role.ADMIN) {
            adminTable.getItems().setAll(results);
            userTable.getItems().clear();
            userSection.setVisible(false);
            userSection.setManaged(false);
            adminSection.setVisible(true);
            adminSection.setManaged(true);
        } else {
            userTable.getItems().setAll(results);
            adminTable.getItems().clear();
            userSection.setVisible(true);
            userSection.setManaged(true);
            adminSection.setVisible(false);
            adminSection.setManaged(false);
        }
    }

    private static String roleFilterLabel(Role role) {
        if (role == null) return "All Users";
        return switch (role) {
            case STUDENT -> "Students";
            case INSTRUCTOR -> "Instructors";
            case ADMIN -> "Administrators";
        };
    }

    private static Role roleFromLabel(String label) {
        return switch (label) {
            case "Students" -> Role.STUDENT;
            case "Instructors" -> Role.INSTRUCTOR;
            case "Administrators" -> Role.ADMIN;
            default -> null;
        };
    }

    private static TableView<User> createUserTable(DashboardShell shell) {
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("user-table");
        table.setFixedCellSize(58);
        table.setPlaceholder(UIComponents.emptyState("No users found", "Try another search or filter."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> nameCol = new TableColumn<>("USER");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(230);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                Label name = new Label(u.getFullName());
                name.getStyleClass().add("table-primary-text");
                Label email = new Label(u.getEmail());
                email.getStyleClass().add("table-secondary-text");
                VBox text = new VBox(2, name, email);
                text.setAlignment(Pos.CENTER_LEFT);
                HBox box = new HBox(10, UIComponents.avatarWithInitials(u.initials(), 34), text);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<User, String> roleCol = new TableColumn<>("ROLE");
        roleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getRole().display()));
        roleCol.setPrefWidth(125);
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty || value == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                        ? null
                        : UIComponents.badge(value, badgeClassFor(getTableView().getItems().get(getIndex()).getRole())));
                setText(null);
            }
        });

        TableColumn<User, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().isActive() ? "Active" : "Disabled"));
        statusCol.setPrefWidth(110);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(UIComponents.badge(value, "Active".equals(value) ? "badge-success" : "badge-error"));
                setText(null);
            }
        });

        TableColumn<User, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setPrefWidth(240);
        table.getColumns().addAll(nameCol, roleCol, statusCol, actionsCol);
        return table;
    }

    @SuppressWarnings("unchecked")
    private static void wireUserTableActions(TableView<User> table, DashboardShell shell, Runnable refresh) {
        TableColumn<User, Void> actionsCol = (TableColumn<User, Void>) (TableColumn<?, ?>) table.getColumns().get(3);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = UIComponents.ghostButton("Edit");
            private final Button toggleBtn = UIComponents.ghostButton("Disable");
            private final Button deleteBtn = UIComponents.ghostButton("Delete");
            private final HBox box = new HBox(6, editBtn, toggleBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    openEditUserDialog(shell, u, refresh);
                });
                toggleBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    try {
                        userService.toggleActive(u.getId(), !u.isActive());
                        Toast.info("User updated", (u.isActive() ? "Deactivated " : "Activated ") + u.getFullName());
                        refresh.run();
                    } catch (UserService.UserServiceException ex) {
                        Toast.error("Could not update user", ex.getMessage());
                    } catch (RuntimeException ex) {
                        Toast.error("Could not update user", "The change could not be saved. Please try again.");
                    }
                });
                deleteBtn.getStyleClass().add("error-text");
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete " + u.getFullName() + "? This cannot be undone. " +
                                    (u.getRole() == Role.INSTRUCTOR
                                            ? "All courses owned by this instructor, with their modules, quizzes, assignments and enrollments, will be permanently deleted too."
                                            : "Their enrollments, progress, submissions and quiz attempts will be permanently deleted too."));
                    UIComponents.styleDialog(confirm);
                    confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                        try {
                            userService.deleteUser(u.getId());
                            Toast.success("User deleted", u.getFullName() + " has been removed.");
                            refresh.run();
                        } catch (UserService.UserServiceException ex) {
                            Toast.error("Could not delete user", ex.getMessage());
                        } catch (RuntimeException ex) {
                            Toast.error("Could not delete user", "The user could not be deleted. Please try again.");
                        }
                    });
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                toggleBtn.setText(u.isActive() ? "Disable" : "Activate");
                setGraphic(box);
            }
        });
    }

    private static void openAddUserDialog(DashboardShell shell, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        UIComponents.styleDialog(dialog);
        dialog.initOwner(shell.getSceneManager().getStage());
        dialog.setTitle("Add New User");
        dialog.getDialogPane().getStylesheets().add(AdminPages.class.getResource("/com/lms/css/theme.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFFFF;");

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField pwField = new PasswordField();
        pwField.setPromptText("Temporary password");
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleBox.setValue(Role.STUDENT);
        Label note = new Label("Note: Admin accounts can only be created by an existing administrator, here.");
        note.getStyleClass().add("muted-text");
        note.setWrapText(true);

        VBox content = new VBox(10, nameField, emailField, pwField, roleBox, note);
        content.setPadding(new Insets(16));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    userService.createUser(nameField.getText(), emailField.getText(), pwField.getText(), roleBox.getValue());
                    Toast.success("User created", nameField.getText() + " has been added as " + roleBox.getValue().display() + ".");
                    onSaved.run();
                } catch (UserService.UserServiceException ex) {
                    Toast.error("Could not create user", ex.getMessage());
                } catch (RuntimeException ex) {
                    Toast.error("Could not create user", "The user could not be created. Please try again.");
                }
            }
            return bt;
        });
        dialog.showAndWait();
    }

    private static void openEditUserDialog(DashboardShell shell, User user, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        UIComponents.styleDialog(dialog);
        dialog.initOwner(shell.getSceneManager().getStage());
        dialog.setTitle("Edit User");
        dialog.getDialogPane().getStylesheets().add(AdminPages.class.getResource("/com/lms/css/theme.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFFFF;");

        TextField nameField = new TextField(user.getFullName());
        TextField emailField = new TextField(user.getEmail());
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleBox.setValue(user.getRole());
        PasswordField resetPasswordField = new PasswordField();
        resetPasswordField.setPromptText("New password (leave blank to keep current)");

        VBox content = new VBox(10, nameField, emailField, roleBox, resetPasswordField);
        content.setPadding(new Insets(16));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                user.setFullName(nameField.getText());
                user.setEmail(emailField.getText());
                user.setRole(roleBox.getValue());
                try {
                    userService.updateUser(user);
                    if (!resetPasswordField.getText().isBlank()) {
                        userService.resetPassword(user.getId(), resetPasswordField.getText());
                    }
                    Toast.success("User updated", resetPasswordField.getText().isBlank()
                            ? "Changes saved." : "Changes and password saved.");
                    onSaved.run();
                } catch (UserService.UserServiceException | com.lms.security.AuthorizationException ex) {
                    Toast.error("Update failed", ex.getMessage());
                } catch (RuntimeException ex) {
                    Toast.error("Update failed", "The changes could not be saved. Please try again.");
                }
            }
            return bt;
        });
        dialog.showAndWait();
    }

    // ============================= COURSE MANAGEMENT =============================

    public static Node courses(DashboardShell shell) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("Course Management"));
        root.getChildren().add(UIComponents.pageSubtitle("Oversee every course on the platform."));

        TextField search = new TextField();
        search.setPromptText("Search courses…");
        search.getStyleClass().add("search-field");
        Button createBtn = UIComponents.primaryButton("+ Create Course");
        HBox controls = new HBox(12, search, UIComponents.spacer(), createBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        FlowPane grid = new FlowPane(18, 18);
        Runnable refresh = () -> {
            grid.getChildren().clear();
            List<Course> results = courseService.search(search.getText(), "All", "All");
            if (results.isEmpty()) grid.getChildren().add(UIComponents.emptyState("No courses found", "Try a different search."));
            for (Course c : results) grid.getChildren().add(CourseCardFactory.adminCard(shell, c, () -> {}));
        };
        search.textProperty().addListener((o, ov, nv) -> refresh.run());
        createBtn.setOnAction(e -> CourseEditorDialog.open(shell.getSceneManager().getStage(), null,
                shell.getCurrentUser().getId(), true, refresh));

        refresh.run();
        ScrollPane scroller = new ScrollPane(grid);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("scroll-pane");

        root.getChildren().addAll(controls, grid);
        return root;
    }

    // ============================= ANALYTICS =============================

    private record CoursePerformance(Course course, double avgQuizScore) {}
    private record InstructorLoad(User instructor, int courseCount) {}
    private record CourseEnrollmentStats(Course course, int total, int completed, int active, int dropped) {}
    private record AssignmentDetail(Assignment assignment, List<Submission> submissions) {}
    private record CourseAssignmentStats(Course course, int assignmentCount, int submissions, int uniqueStudents,
                                         int graded, int late, int pendingGrading, List<AssignmentDetail> assignments) {}
    private record CourseQuizStats(Course course, int quizCount, int publishedQuizzes, int attempts,
                                   int uniqueStudents, double avgScore) {}
    private record AnalyticsData(double completionRate, int totalEnrollments, int totalAssignments, int publishedCourses,
                                 int completedEnrollments, int activeEnrollments, int droppedEnrollments,
                                 int totalAssignmentSubmissions, int uniqueAssignmentStudents, int gradedSubmissions,
                                 int lateSubmissions, int pendingGrading, int totalQuizzes, int publishedQuizzes,
                                 int totalQuizAttempts, int uniqueQuizStudents,
                                 List<CoursePerformance> courses, List<InstructorLoad> instructors,
                                 List<CourseEnrollmentStats> enrollmentsByCourse,
                                 List<CourseAssignmentStats> assignmentsByCourse,
                                 List<CourseQuizStats> quizzesByCourse) {}

    public static Node analytics(DashboardShell shell) {
        VBox root = new VBox(24);
        root.getChildren().add(UIComponents.pageTitle("Platform Analytics"));
        root.getChildren().add(UIComponents.pageSubtitle("Every important platform metric, with expandable details for administrators."));

        VBox content = new VBox(24, new ProgressIndicator());
        root.getChildren().add(content);

        BackgroundTask.run(AdminPages::loadAnalyticsData,
                data -> content.getChildren().setAll(buildAnalyticsContent(data)),
                ex -> showLoadFailure(content, "analytics", ex));
        return root;
    }

    private static AnalyticsData loadAnalyticsData() {
        QuizService quizService = new QuizService();
        List<Course> allCourses = courseService.getAllCourses();
        List<CoursePerformance> courses = allCourses.stream()
                .map(c -> new CoursePerformance(c, quizService.averageScoreForCourse(c.getId())))
                .toList();
        List<InstructorLoad> instructors = userService.getAllUsers().stream()
                .filter(u -> u.getRole() == Role.INSTRUCTOR)
                .map(u -> new InstructorLoad(u, courseService.getCoursesByInstructor(u.getId()).size()))
                .toList();

        int completedEnrollments = 0;
        int activeEnrollments = 0;
        int droppedEnrollments = 0;
        List<CourseEnrollmentStats> enrollmentStats = new java.util.ArrayList<>();
        for (Course course : allCourses) {
            int completed = 0, active = 0, dropped = 0;
            for (Enrollment enrollment : enrollmentService.getCourseEnrollments(course.getId())) {
                if (enrollment.getStatus() == Enrollment.Status.COMPLETED) completed++;
                else if (enrollment.getStatus() == Enrollment.Status.DROPPED) dropped++;
                else active++;
            }
            completedEnrollments += completed;
            activeEnrollments += active;
            droppedEnrollments += dropped;
            enrollmentStats.add(new CourseEnrollmentStats(course, completed + active + dropped, completed, active, dropped));
        }

        int totalAssignmentSubmissions = 0;
        int uniqueAssignmentStudents = 0;
        int gradedSubmissions = 0;
        int lateSubmissions = 0;
        int pendingGrading = 0;
        List<CourseAssignmentStats> assignmentStats = new java.util.ArrayList<>();
        for (Course course : allCourses) {
            int assignmentCount = 0, submissions = 0, graded = 0, late = 0;
            Set<Long> students = new HashSet<>();
            List<AssignmentDetail> assignmentDetails = new java.util.ArrayList<>();
            for (Assignment assignment : assignmentService.getForCourse(course.getId())) {
                assignmentCount++;
                List<Submission> assignmentSubmissions = assignmentService.getSubmissionsForAssignment(assignment.getId());
                assignmentDetails.add(new AssignmentDetail(assignment, List.copyOf(assignmentSubmissions)));
                for (Submission submission : assignmentSubmissions) {
                    submissions++;
                    students.add(submission.getStudentId());
                    if (submission.getStatus() == Submission.Status.GRADED) graded++;
                    if (submission.getStatus() == Submission.Status.LATE) late++;
                }
            }
            int pending = Math.max(0, submissions - graded);
            totalAssignmentSubmissions += submissions;
            uniqueAssignmentStudents += students.size();
            gradedSubmissions += graded;
            lateSubmissions += late;
            pendingGrading += pending;
            assignmentStats.add(new CourseAssignmentStats(course, assignmentCount, submissions, students.size(), graded, late, pending, List.copyOf(assignmentDetails)));
        }

        // Build quiz statistics from the existing quiz/student-attempt services so the admin sees
        // real attempts and participants without changing the database schema.
        Map<Long, Long> quizToCourse = new HashMap<>();
        Map<Long, Integer> quizCountsByCourse = new HashMap<>();
        Map<Long, Integer> publishedQuizCountsByCourse = new HashMap<>();
        for (Course course : allCourses) {
            List<Quiz> quizzes = quizService.getQuizzesForCourse(course.getId());
            quizCountsByCourse.put(course.getId(), quizzes.size());
            int published = 0;
            for (Quiz quiz : quizzes) {
                quizToCourse.put(quiz.getId(), course.getId());
                if (quiz.isPublished()) published++;
            }
            publishedQuizCountsByCourse.put(course.getId(), published);
        }

        Map<Long, Integer> attemptsByCourse = new HashMap<>();
        Map<Long, Set<Long>> attemptStudentsByCourse = new HashMap<>();
        Map<Long, Double> scoreSumByCourse = new HashMap<>();
        Map<Long, Integer> scoreCountByCourse = new HashMap<>();
        for (User student : userService.getAllUsers().stream().filter(u -> u.getRole() == Role.STUDENT).toList()) {
            for (QuizAttempt attempt : quizService.getAttemptsForStudent(student.getId())) {
                Long courseId = quizToCourse.get(attempt.getQuizId());
                if (courseId == null) continue;
                attemptsByCourse.merge(courseId, 1, Integer::sum);
                attemptStudentsByCourse.computeIfAbsent(courseId, k -> new HashSet<>()).add(student.getId());
                scoreSumByCourse.merge(courseId, attempt.percentage(), Double::sum);
                scoreCountByCourse.merge(courseId, 1, Integer::sum);
            }
        }

        int totalQuizzes = quizCountsByCourse.values().stream().mapToInt(Integer::intValue).sum();
        int publishedQuizzes = publishedQuizCountsByCourse.values().stream().mapToInt(Integer::intValue).sum();
        int totalQuizAttempts = attemptsByCourse.values().stream().mapToInt(Integer::intValue).sum();
        int uniqueQuizStudents = attemptStudentsByCourse.values().stream().mapToInt(Set::size).sum();
        List<CourseQuizStats> quizStats = new java.util.ArrayList<>();
        for (Course course : allCourses) {
            int attempts = attemptsByCourse.getOrDefault(course.getId(), 0);
            int scoreCount = scoreCountByCourse.getOrDefault(course.getId(), 0);
            double avg = scoreCount == 0 ? 0 : scoreSumByCourse.getOrDefault(course.getId(), 0.0) / scoreCount;
            quizStats.add(new CourseQuizStats(course,
                    quizCountsByCourse.getOrDefault(course.getId(), 0),
                    publishedQuizCountsByCourse.getOrDefault(course.getId(), 0),
                    attempts,
                    attemptStudentsByCourse.getOrDefault(course.getId(), Set.of()).size(),
                    avg));
        }

        return new AnalyticsData(
                enrollmentService.completionRate(),
                enrollmentService.countAll(),
                assignmentService.countAll(),
                courseService.getPublishedCourses().size(),
                completedEnrollments,
                activeEnrollments,
                droppedEnrollments,
                totalAssignmentSubmissions,
                uniqueAssignmentStudents,
                gradedSubmissions,
                lateSubmissions,
                pendingGrading,
                totalQuizzes,
                publishedQuizzes,
                totalQuizAttempts,
                uniqueQuizStudents,
                courses,
                instructors,
                List.copyOf(enrollmentStats),
                List.copyOf(assignmentStats),
                List.copyOf(quizStats));
    }

    private static VBox buildAnalyticsContent(AnalyticsData data) {
        VBox root = new VBox(20);

        Label hint = new Label("Click a metric to view its details below. Assignment and quiz records are grouped by course so the page stays clean.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #5F7194; -fx-font-size: 13px; -fx-padding: 0 4 4 4;");

        VBox completionDetails = new VBox(12,
                summaryGrid(
                        summaryItem("Completion rate", String.format("%.1f%%", data.completionRate())),
                        summaryItem("Completed", String.valueOf(data.completedEnrollments())),
                        summaryItem("Active", String.valueOf(data.activeEnrollments())),
                        summaryItem("Dropped", String.valueOf(data.droppedEnrollments())),
                        summaryItem("Total enrollments", String.valueOf(data.totalEnrollments()))),
                UIComponents.sectionTitle("Completion by course"));
        for (CourseEnrollmentStats stat : data.enrollmentsByCourse()) {
            completionDetails.getChildren().add(detailRow(stat.course().getTitle(),
                    stat.total() + " enrolled  •  " + stat.completed() + " completed  •  "
                            + stat.active() + " active  •  " + stat.dropped() + " dropped  •  completion "
                            + String.format("%.0f%%", stat.total() == 0 ? 0 : stat.completed() * 100.0 / stat.total())));
        }
        if (data.enrollmentsByCourse().isEmpty()) completionDetails.getChildren().add(emptyAnalytics("No enrollment data yet."));

        VBox enrollmentDetails = new VBox(12,
                summaryGrid(
                        summaryItem("Total enrollments", String.valueOf(data.totalEnrollments())),
                        summaryItem("Completed", String.valueOf(data.completedEnrollments())),
                        summaryItem("Active", String.valueOf(data.activeEnrollments())),
                        summaryItem("Dropped", String.valueOf(data.droppedEnrollments()))),
                UIComponents.sectionTitle("Enrollment by course"));
        for (CourseEnrollmentStats stat : data.enrollmentsByCourse()) {
            enrollmentDetails.getChildren().add(detailRow(stat.course().getTitle(),
                    stat.total() + " enrolled  •  " + stat.completed() + " completed  •  " + stat.active() + " active  •  " + stat.dropped() + " dropped"));
        }
        if (data.enrollmentsByCourse().isEmpty()) enrollmentDetails.getChildren().add(emptyAnalytics("No courses have enrollment data yet."));

        VBox assignmentDetails = new VBox(12,
                summaryGrid(
                        summaryItem("Assignments", String.valueOf(data.totalAssignments())),
                        summaryItem("Students attempted", String.valueOf(data.uniqueAssignmentStudents())),
                        summaryItem("Submissions", String.valueOf(data.totalAssignmentSubmissions())),
                        summaryItem("Graded", String.valueOf(data.gradedSubmissions())),
                        summaryItem("Pending grading", String.valueOf(data.pendingGrading())),
                        summaryItem("Late submissions", String.valueOf(data.lateSubmissions()))),
                UIComponents.sectionTitle("Assignments by course"));
        for (CourseAssignmentStats stat : data.assignmentsByCourse()) {
            assignmentDetails.getChildren().add(expandableSection(
                    stat.course().getTitle(),
                    stat.assignmentCount() + " assignments  •  " + stat.uniqueStudents() + " students attempted  •  "
                            + stat.submissions() + " submissions  •  " + stat.graded() + " graded  •  "
                            + stat.pendingGrading() + " pending  •  " + stat.late() + " late",
                    buildAssignmentDetails(stat.assignments())));
        }
        if (data.assignmentsByCourse().isEmpty()) assignmentDetails.getChildren().add(emptyAnalytics("No assignments are currently recorded."));

        VBox courseDetails = new VBox(12,
                summaryGrid(
                        summaryItem("Active courses", String.valueOf(data.publishedCourses())),
                        summaryItem("All courses", String.valueOf(data.courses().size())),
                        summaryItem("Published quizzes", String.valueOf(data.publishedQuizzes())),
                        summaryItem("Total quizzes", String.valueOf(data.totalQuizzes())),
                        summaryItem("Quiz attempts", String.valueOf(data.totalQuizAttempts())),
                        summaryItem("Quiz participants", String.valueOf(data.uniqueQuizStudents()))),
                UIComponents.sectionTitle("Published course details"));
        for (Course c : data.courses().stream().map(CoursePerformance::course).filter(Course::isPublished).toList()) {
            CourseEnrollmentStats enrollment = data.enrollmentsByCourse().stream().filter(x -> x.course().getId() == c.getId()).findFirst().orElse(null);
            CourseAssignmentStats assignment = data.assignmentsByCourse().stream().filter(x -> x.course().getId() == c.getId()).findFirst().orElse(null);
            CourseQuizStats quiz = data.quizzesByCourse().stream().filter(x -> x.course().getId() == c.getId()).findFirst().orElse(null);
            String instructor = c.getInstructorName() == null || c.getInstructorName().isBlank() ? "Instructor not assigned" : c.getInstructorName();

            VBox details = new VBox(12);
            details.getChildren().add(summaryGrid(
                    summaryItem("Instructor", instructor),
                    summaryItem("Enrolled", String.valueOf(c.getEnrollmentCount())),
                    summaryItem("Assignments", String.valueOf(assignment == null ? 0 : assignment.assignmentCount())),
                    summaryItem("Submissions", String.valueOf(assignment == null ? 0 : assignment.submissions())),
                    summaryItem("Quizzes", String.valueOf(quiz == null ? 0 : quiz.quizCount())),
                    summaryItem("Quiz attempts", String.valueOf(quiz == null ? 0 : quiz.attempts())),
                    summaryItem("Avg quiz score", String.format("%.0f%%", quiz == null ? 0 : quiz.avgScore())),
                    summaryItem("Completion", String.format("%.0f%%", enrollment == null || enrollment.total() == 0 ? 0 : enrollment.completed() * 100.0 / enrollment.total()))));

            if (assignment != null && !assignment.assignments().isEmpty()) {
                details.getChildren().add(UIComponents.sectionTitle("Assignments & submissions"));
                details.getChildren().add(buildAssignmentDetails(assignment.assignments()));
            }

            if (quiz != null && quiz.quizCount() > 0) {
                details.getChildren().add(UIComponents.sectionTitle("Quizzes & attempts"));
                details.getChildren().add(buildQuizDetails(c.getId(), quiz, data));
            }

            courseDetails.getChildren().add(expandableSection(c.getTitle(),
                    instructor + "  •  " + c.getEnrollmentCount() + " enrolled  •  "
                            + (assignment == null ? 0 : assignment.assignmentCount()) + " assignments  •  "
                            + (quiz == null ? 0 : quiz.quizCount()) + " quizzes",
                    details));
        }
        if (data.publishedCourses() == 0) courseDetails.getChildren().add(emptyAnalytics("There are no published courses currently."));

        // Keep each metric in its own vertical column. When a card opens, only the
        // content below that card is pushed down; the neighbouring column keeps its
        // original position. Collapsing the card restores the original layout.
        VBox leftColumn = new VBox(14);
        VBox rightColumn = new VBox(14);
        leftColumn.setFillWidth(true);
        rightColumn.setFillWidth(true);

        VBox completionHost = analyticsDetailsHost();
        VBox enrollmentHost = analyticsDetailsHost();
        VBox assignmentHost = analyticsDetailsHost();
        VBox courseHost = analyticsDetailsHost();

        VBox completionCard = analyticsMetricCard("Completion Rate", String.format("%.1f%%", data.completionRate()),
                data.completedEnrollments() + " completed • " + data.activeEnrollments() + " active", "◔", "#22C55E",
                "Completion Rate", completionDetails, completionHost);
        VBox enrollmentCard = analyticsMetricCard("Total Enrollments", String.valueOf(data.totalEnrollments()),
                data.enrollmentsByCourse().size() + " courses with enrollment data", "↗", "#287BEF",
                "Total Enrollments", enrollmentDetails, enrollmentHost);
        VBox assignmentCard = analyticsMetricCard("Assignments", String.valueOf(data.totalAssignments()),
                data.totalAssignmentSubmissions() + " submissions • " + data.pendingGrading() + " pending grading", "□", "#E5484D",
                "Assignments", assignmentDetails, assignmentHost);
        VBox courseCard = analyticsMetricCard("Active Courses", String.valueOf(data.publishedCourses()),
                data.totalQuizzes() + " quizzes • " + data.totalQuizAttempts() + " quiz attempts", "▣", "#4B8FF7",
                "Active Courses", courseDetails, courseHost);

        leftColumn.getChildren().addAll(completionCard, completionHost, assignmentCard, assignmentHost);
        rightColumn.getChildren().addAll(enrollmentCard, enrollmentHost, courseCard, courseHost);
        completionHost.setManaged(false); completionHost.setVisible(false);
        enrollmentHost.setManaged(false); enrollmentHost.setVisible(false);
        assignmentHost.setManaged(false); assignmentHost.setVisible(false);
        courseHost.setManaged(false); courseHost.setVisible(false);

        HBox metricColumns = new HBox(14, leftColumn, rightColumn);
        metricColumns.setFillHeight(false);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        VBox coursePerf = UIComponents.card(UIComponents.sectionTitle("Course Performance"));
        VBox perfList = new VBox(12);
        for (CoursePerformance cp : data.courses()) {
            Course c = cp.course();
            double avgQuiz = cp.avgQuizScore();
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = labelStrong(c.getTitle());
            name.setMinWidth(140);
            name.setMaxWidth(220);
            name.setWrapText(true);
            ProgressBar bar = UIComponents.progress(Math.max(avgQuiz, 0) / 100.0);
            bar.setPrefWidth(220);
            Label pct = mutedLabel(String.format("Avg quiz score: %.0f%%  •  %d enrolled", avgQuiz, c.getEnrollmentCount()));
            pct.setWrapText(true);
            HBox.setHgrow(pct, Priority.ALWAYS);
            row.getChildren().addAll(name, bar, pct);
            perfList.getChildren().add(row);
        }
        if (data.courses().isEmpty()) perfList.getChildren().add(UIComponents.emptyState("No data yet", "Create courses to see analytics."));
        coursePerf.getChildren().add(perfList);

        VBox instructorPerf = UIComponents.card(UIComponents.sectionTitle("Instructor Performance"));
        VBox instrList = new VBox(10);
        for (InstructorLoad load : data.instructors()) {
            User instr = load.instructor();
            HBox row = new HBox(12, UIComponents.avatarWithInitials(instr.initials(), 30),
                    labelStrong(instr.getFullName()), UIComponents.spacer(),
                    mutedLabel(load.courseCount() + " course(s)"));
            row.setAlignment(Pos.CENTER_LEFT);
            instrList.getChildren().add(row);
        }
        instructorPerf.getChildren().add(instrList);

        root.getChildren().addAll(hint, metricColumns, coursePerf, instructorPerf);
        return root;
    }

    private static VBox buildQuizDetails(long courseId, CourseQuizStats quizStats, AnalyticsData data) {
        VBox box = new VBox(10);
        QuizService quizService = new QuizService();
        List<Quiz> quizzes = quizService.getQuizzesForCourse(courseId);
        Map<Long, User> students = userService.getAllUsers().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        if (quizzes.isEmpty()) {
            box.getChildren().add(emptyAnalytics("No quizzes are currently recorded for this course."));
            return box;
        }

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> attempts = new java.util.ArrayList<>();
            for (User student : students.values()) {
                attempts.addAll(quizService.getAttemptsForStudent(student.getId()).stream()
                        .filter(a -> a.getQuizId() == quiz.getId())
                        .toList());
            }
            double average = attempts.stream().mapToDouble(QuizAttempt::percentage).average().orElse(0);
            long participants = attempts.stream().map(QuizAttempt::getStudentId).distinct().count();

            VBox attemptDetails = new VBox(8);
            if (attempts.isEmpty()) {
                attemptDetails.getChildren().add(emptyAnalytics("No students have attempted this quiz yet."));
            } else {
                for (QuizAttempt attempt : attempts) {
                    User student = students.get(attempt.getStudentId());
                    String studentName = student == null ? "Student #" + attempt.getStudentId() : student.getFullName();
                    String submitted = attempt.getSubmittedAt() == null ? "Time unavailable"
                            : attempt.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
                    attemptDetails.getChildren().add(detailRow(studentName,
                            String.format("%.0f%%  •  %.1f/%.1f marks  •  %s", attempt.percentage(), attempt.getScore(), attempt.getTotalMarks(), submitted)));
                }
            }

            VBox quizBody = new VBox(10,
                    summaryGrid(
                            summaryItem("Status", quiz.isPublished() ? "Published" : "Unpublished"),
                            summaryItem("Attempts", String.valueOf(attempts.size())),
                            summaryItem("Participants", String.valueOf(participants)),
                            summaryItem("Average score", String.format("%.0f%%", average))),
                    UIComponents.sectionTitle("Student attempts"),
                    attemptDetails);

            box.getChildren().add(expandableSection(quiz.getTitle(),
                    (quiz.isPublished() ? "Published" : "Unpublished") + "  •  " + attempts.size() + " attempts  •  "
                            + participants + " participants  •  avg " + String.format("%.0f%%", average),
                    quizBody));
        }
        return box;
    }

    private static VBox buildAssignmentDetails(List<AssignmentDetail> assignments) {
        VBox box = new VBox(10);
        if (assignments.isEmpty()) {
            box.getChildren().add(emptyAnalytics("No assignments in this course."));
            return box;
        }
        for (AssignmentDetail detail : assignments) {
            Assignment a = detail.assignment();
            List<Submission> submissions = detail.submissions();
            long graded = submissions.stream().filter(s -> s.getStatus() == Submission.Status.GRADED).count();
            long late = submissions.stream().filter(s -> s.getStatus() == Submission.Status.LATE).count();
            VBox submissionDetails = new VBox(8);
            if (submissions.isEmpty()) {
                submissionDetails.getChildren().add(emptyAnalytics("No students have submitted this assignment yet."));
            } else {
                for (Submission submission : submissions) {
                    String studentName = submission.getStudentName() == null || submission.getStudentName().isBlank()
                            ? "Student #" + submission.getStudentId() : submission.getStudentName();
                    String grade = submission.getGrade() == null ? "Not graded" : submission.getGrade() + "/" + a.getMaxMarks();
                    String submitted = submission.getSubmittedAt() == null ? "Time unavailable"
                            : submission.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

                    VBox studentCard = new VBox(10);
                    studentCard.setPadding(new Insets(14));
                    studentCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-border-color: #E3ECFA; -fx-border-radius: 12;");

                    HBox studentHeader = new HBox(10);
                    studentHeader.setAlignment(Pos.CENTER_LEFT);
                    Label name = labelStrong(studentName);
                    HBox.setHgrow(name, Priority.ALWAYS);
                    Label status = UIComponents.badge(statusText(submission.getStatus()),
                            submission.getStatus() == Submission.Status.GRADED ? "badge-success" : "badge-warning");
                    Label gradeLabel = mutedLabel("Grade: " + grade);
                    studentHeader.getChildren().addAll(name, gradeLabel, status);

                    Label submittedLabel = mutedLabel("Submitted " + submitted);

                    VBox responseBox = new VBox(5);
                    Label responseTitle = labelStrong("Student response");
                    TextArea response = new TextArea(submission.getSubmissionText() == null ? "" : submission.getSubmissionText());
                    response.setEditable(false);
                    response.setWrapText(true);
                    response.setPrefRowCount(3);
                    response.setMaxWidth(Double.MAX_VALUE);
                    if (response.getText().isBlank()) {
                        response.setPromptText("No written response provided.");
                    }
                    responseBox.getChildren().addAll(responseTitle, response);

                    VBox attachmentBox = new VBox(5);
                    if (submission.getAttachmentName() != null && !submission.getAttachmentName().isBlank()) {
                        Label attachmentTitle = labelStrong("Submitted file");
                        HBox fileRow = new HBox(10);
                        fileRow.setAlignment(Pos.CENTER_LEFT);
                        Label fileName = mutedLabel("📎 " + submission.getAttachmentName() + "  •  " + formatFileSize(submission.getAttachmentSize()));
                        HBox.setHgrow(fileName, Priority.ALWAYS);
                        Button download = UIComponents.ghostButton("Download");
                        download.setOnAction(e -> downloadSubmissionAttachment(submission));
                        fileRow.getChildren().addAll(fileName, download);
                        attachmentBox.getChildren().addAll(attachmentTitle, fileRow);
                    }

                    studentCard.getChildren().addAll(studentHeader, submittedLabel, responseBox);
                    if (!attachmentBox.getChildren().isEmpty()) studentCard.getChildren().add(attachmentBox);
                    submissionDetails.getChildren().add(studentCard);
                }
            }
            String deadline = a.getDeadline() == null ? "No deadline" : a.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
            VBox assignmentBody = new VBox(10,
                    summaryGrid(
                            summaryItem("Maximum marks", String.valueOf(a.getMaxMarks())),
                            summaryItem("Deadline", deadline),
                            summaryItem("Submissions", String.valueOf(submissions.size())),
                            summaryItem("Graded", String.valueOf(graded)),
                            summaryItem("Pending", String.valueOf(Math.max(0, submissions.size() - graded))),
                            summaryItem("Late", String.valueOf(late))),
                    UIComponents.sectionTitle("Student submissions"),
                    submissionDetails);
            box.getChildren().add(expandableSection(a.getTitle(),
                    submissions.size() + " submissions  •  " + graded + " graded  •  " + Math.max(0, submissions.size() - graded) + " pending  •  " + late + " late  •  max " + a.getMaxMarks() + " marks",
                    assignmentBody));
        }
        return box;
    }

    private static void downloadSubmissionAttachment(Submission submission) {
        try {
            Path source = resolveSubmissionAttachment(submission);
            if (source == null || !Files.isRegularFile(source)) {
                Toast.error("Could not download file", "The submitted file is no longer available on this computer.");
                return;
            }
            Path destination = new DownloadService().download(
                    source,
                    submission.getAttachmentName(),
                    submission.getAssignmentTitle() + " — " + submission.getStudentName(),
                    "Assignment Submission");
            Toast.success("File downloaded", destination.getFileName().toString());
        } catch (Exception ex) {
            Toast.error("Could not download file", ex.getMessage() == null ? "Please try again." : ex.getMessage());
        }
    }

    private static Path uniqueDownloadPath(Path directory, String fileName) {
        Path target = directory.resolve(fileName);
        if (!Files.exists(target)) return target;

        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        int counter = 1;
        do {
            target = directory.resolve(base + " (" + counter++ + ")" + extension);
        } while (Files.exists(target));
        return target;
    }

    private static Path resolveSubmissionAttachment(Submission submission) {
        if (submission.getAttachmentPath() == null || submission.getAttachmentPath().isBlank()) return null;

        Path stored = Path.of(submission.getAttachmentPath());
        if (Files.isRegularFile(stored)) return stored;

        Path persistent = Path.of(System.getProperty("user.home"), ".lumenlms", "uploads", "submissions",
                stored.getFileName().toString());
        if (Files.isRegularFile(persistent)) return persistent;

        // Older project versions stored a relative path inside the project folder.
        // Try the current project and its parent folders before asking the admin to locate it.
        String fileName = stored.getFileName().toString();
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path dir = current; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("uploads").resolve("submissions").resolve(fileName);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String statusText(Submission.Status status) {
        if (status == null) return "Status unknown";
        return switch (status) {
            case GRADED -> "Graded";
            case LATE -> "Late";
            case SUBMITTED -> "Submitted";
        };
    }

    private static VBox expandableSection(String title, String subtitle, Node details) {
        VBox box = new VBox(0);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #DCE8F8; -fx-border-radius: 12;");
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14));
        header.setCursor(javafx.scene.Cursor.HAND);
        VBox text = new VBox(3);
        Label titleLabel = labelStrong(title);
        titleLabel.setWrapText(true);
        Label subtitleLabel = mutedLabel(subtitle);
        subtitleLabel.setWrapText(true);
        text.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label arrow = new Label("›");
        arrow.setStyle("-fx-font-size: 22px; -fx-text-fill: #6B7A99;");
        header.getChildren().addAll(text, arrow);
        VBox content = new VBox(details);
        content.setPadding(new Insets(0, 14, 14, 14));
        content.setVisible(false);
        content.setManaged(false);
        header.setOnMouseClicked(e -> {
            boolean expanded = !content.isVisible();
            content.setVisible(expanded);
            content.setManaged(expanded);
            arrow.setText(expanded ? "⌄" : "›");
        });
        box.getChildren().addAll(header, content);
        return box;
    }

    private static int uniqueStudentCount(List<CourseEnrollmentStats> stats) {
        // Course enrollment counts are intentionally summed here because the same student may be enrolled
        // in multiple courses; the metric is "enrollments", not unique platform users.
        return stats.stream().mapToInt(CourseEnrollmentStats::total).sum();
    }

    private static VBox analyticsDetailsHost() {
        VBox host = new VBox(12);
        host.setFillWidth(true);
        host.setPadding(new Insets(14));
        host.setStyle("-fx-background-color: #F8FBFF; -fx-background-radius: 14; -fx-border-color: #E3ECFA; -fx-border-radius: 14;");
        return host;
    }

    private static VBox analyticsMetricCard(String title, String value, String subtitle, String icon,
                                            String accent, String detailsTitle, Node details, VBox detailsHost) {
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefHeight(112);
        card.setMinHeight(112);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #E3ECFA; -fx-border-radius: 16;");

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setCursor(javafx.scene.Cursor.HAND);

        StackPane chip = UIComponents.iconChip(icon, accent);
        VBox text = new VBox(2);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        Label subtitleLabel = mutedLabel(subtitle);
        subtitleLabel.setWrapText(true);
        text.getChildren().addAll(valueLabel, titleLabel, subtitleLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label arrow = new Label("→");
        arrow.setStyle("-fx-font-size: 20px; -fx-text-fill: #6B7A99;");
        header.getChildren().addAll(chip, text, arrow);
        card.getChildren().add(header);

        header.setOnMouseClicked(e -> {
            boolean expanded = detailsHost.isManaged();
            if (expanded) {
                detailsHost.getChildren().clear();
                detailsHost.setManaged(false);
                detailsHost.setVisible(false);
                arrow.setText("→");
            } else {
                detailsHost.getChildren().setAll(
                        labelledDetailsHeading(detailsTitle), details);
                detailsHost.setManaged(true);
                detailsHost.setVisible(true);
                arrow.setText("⌄");
            }
        });
        header.setAccessibleText(title + " analytics. Click to view details below.");
        return card;
    }

    private static Label labelledDetailsHeading(String title) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #18243D;");
        return heading;
    }

    private static GridPane summaryGrid(Node... items) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < items.length; i++) {
            grid.add(items[i], i % 2, i / 2);
        }
        return grid;
    }

    private static VBox summaryItem(String label, String value) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E3ECFA; -fx-border-radius: 10;");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-font-size: 18px;");
        Label labelText = mutedLabel(label);
        box.getChildren().addAll(valueLabel, labelText);
        return box;
    }

    private static HBox detailRow(String label, String value) {
        Label left = labelStrong(label);
        left.setWrapText(true);
        Label right = mutedLabel(value);
        right.setWrapText(true);
        HBox row = new HBox(12, left, UIComponents.spacer(), right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E3ECFA; -fx-border-radius: 10;");
        return row;
    }

    private static Node emptyAnalytics(String message) {
        return UIComponents.emptyState("No data", message);
    }

    // ============================= SETTINGS =============================

    public static Node settings(DashboardShell shell) {
        VBox root = new VBox(20);
        root.getChildren().add(UIComponents.pageTitle("System Settings"));
        root.getChildren().add(UIComponents.pageSubtitle("Configure platform-wide preferences."));

        Map<String, String> current = settingsService.getAll();

        TextField platformName = new TextField(current.getOrDefault("platform_name", "LumenLMS"));
        TextField supportEmail = new TextField(current.getOrDefault("support_email", "support@lumenlms.com"));
        CheckBox allowRegistration = new CheckBox("Allow public self-registration");
        allowRegistration.setSelected("true".equalsIgnoreCase(current.getOrDefault("allow_registration", "true")));
        Label themeInfo = new Label("LumenLMS uses its original light theme.");
        themeInfo.getStyleClass().add("muted-text");

        Button saveBtn = UIComponents.primaryButton("Save Settings");
        saveBtn.setOnAction(e -> {
            try {
                settingsService.saveGeneral(platformName.getText(), supportEmail.getText(), allowRegistration.isSelected());
                shell.refreshBranding();
                shell.getSceneManager().applyTheme();
                Toast.success("Settings saved", "Platform settings have been updated.");
            } catch (SettingsService.SettingsException | com.lms.security.AuthorizationException ex) {
                Toast.error("Could not save settings", ex.getMessage());
            } catch (RuntimeException ex) {
                Toast.error("Could not save settings", "The settings could not be saved. Please try again.");
            }
        });

        VBox card = UIComponents.card(
                UIComponents.sectionTitle("General"),
                labeled("Platform Name", platformName),
                labeled("Support Email", supportEmail),
                themeInfo,
                allowRegistration,
                saveBtn
        );
        card.setMaxWidth(480);

        root.getChildren().add(card);
        return root;
    }

    // ============================= helpers =============================

    private static VBox labeled(String label, Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("muted-text");
        return new VBox(4, l, field);
    }

    private static Label labelStrong(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: -text; -fx-font-weight: 700; -fx-font-size: 12.5px;");
        return l;
    }

    private static Label mutedLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted-text");
        return l;
    }

    static String badgeClassFor(Role role) {
        return switch (role) {
            case ADMIN -> "badge-error";
            case INSTRUCTOR -> "badge-accent";
            case STUDENT -> "badge-info";
        };
    }
}
