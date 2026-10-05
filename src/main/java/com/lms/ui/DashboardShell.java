package com.lms.ui;

import com.lms.model.Course;
import com.lms.model.Notification;
import com.lms.model.Role;
import com.lms.model.User;
import com.lms.service.AuthService;
import com.lms.service.CourseService;
import com.lms.service.NotificationService;
import com.lms.util.AnimationUtil;
import com.lms.util.AppSettings;
import com.lms.util.BackgroundTask;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Post-login application shell: collapsible sidebar, top bar (search, notifications, profile),
 * and a swappable content region. Delegates page content construction to role-specific page classes.
 */
public class DashboardShell {

    private final SceneManager sceneManager;
    private final User currentUser;
    private final NotificationService notificationService = new NotificationService();
    private final CourseService courseService = new CourseService();

    private StackPane contentArea;
    private VBox sidebar;
    private boolean collapsed = false;
    private String activeNav = "Dashboard";
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private Label notificationBadge;
    private Label breadcrumbLabel;
    private Label logoLabel;
    // Keeps the instructor's selected course stable when opening a nested page and returning.
    private final Map<String, Long> selectedCourseIds = new HashMap<>();

    public DashboardShell(SceneManager sceneManager, User currentUser) {
        this.sceneManager = sceneManager;
        this.currentUser = currentUser;
    }

    public StackPane build() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-background");

        sidebar = buildSidebar();
        root.setLeft(sidebar);

        VBox center = new VBox();
        HBox topbar = buildTopbar();
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(28));
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        center.getChildren().addAll(topbar, scrollPane);
        root.setCenter(center);

        StackPane wrapper = new StackPane(root);
        Toast.attach(wrapper);

        navigate("Dashboard");
        AnimationUtil.fadeIn(root, 320);
        return wrapper;
    }

    // ============================= SIDEBAR =============================

    private VBox buildSidebar() {
        VBox box = new VBox(4);
        box.getStyleClass().add("sidebar");
        box.setPrefWidth(240);
        box.setMinWidth(240);
        box.setPadding(new Insets(20, 12, 20, 12));

        HBox logoRow = new HBox(8);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        logoRow.setPadding(new Insets(4, 8, 24, 8));
        logoLabel = new Label("✦ " + AppSettings.platformName());
        Label logo = logoLabel;
        logo.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 18));
        logo.setTextFill(Color.web("#287BEF"));
        logoRow.getChildren().add(logo);

        VBox navBox = new VBox(4);
        for (String item : navItemsForRole()) {
            Button btn = new Button(iconFor(item) + "   " + item);
            btn.getStyleClass().add("nav-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setOnAction(e -> navigate(item));
            navButtons.put(item, btn);
            navBox.getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("⏻   Logout");
        logoutBtn.getStyleClass().add("logout-button");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setAlignment(Pos.CENTER_LEFT);
        logoutBtn.setOnAction(e -> {
            new AuthService().logout();
            sceneManager.goToLogin();
        });

        box.getChildren().addAll(logoRow, navBox, spacer, logoutBtn);
        highlightActive();
        return box;
    }

    private List<String> navItemsForRole() {
        return switch (currentUser.getRole()) {
            case ADMIN -> List.of("Dashboard", "Users", "Courses", "Analytics", "Settings", "Profile", "Downloads");
            case INSTRUCTOR -> List.of("Dashboard", "My Courses", "Quizzes", "Assignments", "Students", "Profile", "Downloads");
            case STUDENT -> List.of("Dashboard", "Browse Courses", "My Courses", "Quizzes", "Assignments", "Quiz Results", "Profile", "Downloads");
        };
    }

    private String iconFor(String item) {
        return switch (item) {
            case "Dashboard" -> "▦";
            case "Users" -> "👤";
            case "Courses", "My Courses", "Browse Courses" -> "📚";
            case "Analytics" -> "📊";
            case "Settings" -> "⚙";
            case "Quizzes", "Quiz Results" -> "📝";
            case "Assignments" -> "📄";
            case "Students" -> "🎓";
            case "Profile" -> "🙍";
            case "Downloads" -> "↓";
            default -> "•";
        };
    }

    private void highlightActive() {
        navButtons.forEach((name, btn) -> {
            btn.getStyleClass().remove("nav-item-active");
            btn.getStyleClass().remove("nav-item");
            btn.getStyleClass().add(name.equals(activeNav) ? "nav-item-active" : "nav-item");
        });
    }

    // ============================= TOP BAR =============================

    private HBox buildTopbar() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(14, 28, 14, 28));
        bar.setAlignment(Pos.CENTER_LEFT);

        Button collapseBtn = new Button("☰");
        collapseBtn.getStyleClass().add("icon-button");
        collapseBtn.setOnAction(e -> toggleSidebar());

        breadcrumbLabel = new Label("Home  ›  " + activeNav);
        breadcrumbLabel.getStyleClass().add("breadcrumb");

        TextField search = new TextField();
        search.setPromptText("Search courses, users, assignments…");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(320);
        wireGlobalSearch(search);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane bellWrap = new StackPane();
        Button bell = new Button("🔔");
        bell.getStyleClass().add("icon-button");
        notificationBadge = new Label();
        notificationBadge.getStyleClass().addAll("badge", "badge-error");
        notificationBadge.setStyle(notificationBadge.getStyle() + "-fx-padding: 1px 6px; -fx-font-size: 9px;");
        StackPane.setAlignment(notificationBadge, Pos.TOP_RIGHT);
        refreshNotificationBadge();
        bellWrap.getChildren().addAll(bell, notificationBadge);
        bell.setOnAction(e -> showNotificationPanel(bell));

        StackPane avatar = UIComponents.avatarWithInitials(currentUser.initials(), 38);
        VBox nameBox = new VBox(0);
        Label name = new Label(currentUser.getFullName());
        name.setStyle("-fx-text-fill: -text; -fx-font-weight: 700; -fx-font-size: 12.5px;");
        Label role = new Label(currentUser.getRole().display());
        role.getStyleClass().add("muted-text");
        nameBox.getChildren().addAll(name, role);
        HBox profileBox = new HBox(10, avatar, nameBox);
        profileBox.setAlignment(Pos.CENTER_LEFT);
        profileBox.setCursor(javafx.scene.Cursor.HAND);
        profileBox.setOnMouseClicked(e -> navigate("Profile"));

        bar.getChildren().addAll(collapseBtn, breadcrumbLabel, search, spacer, bellWrap, profileBox);
        return bar;
    }

    private void wireGlobalSearch(TextField search) {
        Popup popup = new Popup();
        VBox results = new VBox(4);
        results.setPadding(new Insets(10));
        results.setPrefWidth(360);
        results.getStyleClass().add("card");
        popup.getContent().add(results);
        popup.setAutoHide(true);

        // Searching hits the database, so it runs on a worker thread. The ticket discards results of
        // older queries that finish after a newer keystroke.
        AtomicInteger searchTicket = new AtomicInteger();
        Runnable refresh = () -> {
            String query = search.getText() == null ? "" : search.getText().trim();
            final int ticket = searchTicket.incrementAndGet();
            if (query.isBlank()) {
                results.getChildren().clear();
                popup.hide();
                return;
            }

            BackgroundTask.run(() -> courseService.search(query, "All", "All").stream()
                    .filter(Course::isPublished)
                    .filter(c -> currentUser.getRole() != Role.INSTRUCTOR || c.getInstructorId() == currentUser.getId())
                    .limit(6)
                    .toList(), matches -> {
            if (ticket != searchTicket.get() || !search.isFocused()) return;
            results.getChildren().clear();

            if (matches.isEmpty()) {
                Label empty = new Label("No matching courses found");
                empty.getStyleClass().add("muted-text");
                results.getChildren().add(empty);
            } else {
                for (Course course : matches) {
                    Button item = new Button(course.getTitle());
                    item.setMaxWidth(Double.MAX_VALUE);
                    item.setAlignment(Pos.CENTER_LEFT);
                    item.setStyle("-fx-background-color: transparent; -fx-text-fill: -text; -fx-font-weight: 600; -fx-padding: 10px 12px;");
                    item.setCursor(javafx.scene.Cursor.HAND);
                    item.setOnAction(e -> {
                        popup.hide();
                        search.clear();
                        if (currentUser.getRole() == Role.STUDENT) {
                            if (new com.lms.service.EnrollmentService().isEnrolled(currentUser.getId(), course.getId())) {
                                openLearning(course.getId());
                            } else {
                                navigate("Browse Courses");
                            }
                        } else if (currentUser.getRole() == Role.INSTRUCTOR) {
                            openCourseContentEditor(course.getId());
                        } else {
                            navigate("Courses");
                        }
                    });
                    results.getChildren().add(item);
                }
            }

            if (!popup.isShowing() && search.getScene() != null) {
                var bounds = search.localToScreen(search.getBoundsInLocal());
                if (bounds != null) popup.show(search, bounds.getMinX(), bounds.getMaxY() + 6);
            }
            }, ex -> { /* search is best-effort: a failed lookup simply shows no suggestions */ });
        };

        search.textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        search.setOnAction(e -> refresh.run());
        search.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) popup.hide();
        });
    }

    private void toggleSidebar() {
        collapsed = !collapsed;
        AnimationUtil.animateSidebarWidth(sidebar, collapsed ? 240 : 72, collapsed ? 72 : 240);
        navButtons.forEach((name, btn) -> btn.setText(collapsed ? iconFor(name) : iconFor(name) + "   " + name));
    }

    private void refreshNotificationBadge() {
        int unread = notificationService.unreadCount(currentUser.getId());
        notificationBadge.setText(String.valueOf(unread));
        notificationBadge.setVisible(unread > 0);
    }

    private void showNotificationPanel(Button anchor) {
        List<Notification> notifications = notificationService.getForUser(currentUser.getId());
        Popup popup = new Popup();
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(16));
        box.setPrefWidth(320);
        box.setMaxHeight(400);

        HBox header = new HBox(UIComponents.sectionTitle("Notifications"), UIComponents.spacer());
        Button markRead = UIComponents.ghostButton("Mark all read");
        markRead.setOnAction(e -> {
            notificationService.markAllRead(currentUser.getId());
            refreshNotificationBadge();
            popup.hide();
        });
        header.getChildren().add(markRead);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(8);
        if (notifications.isEmpty()) {
            list.getChildren().add(UIComponents.emptyState("No notifications", "You're all caught up."));
        } else {
            for (Notification n : notifications) {
                VBox item = new VBox(2);
                Label t = new Label(n.getTitle());
                t.setStyle("-fx-text-fill: -text; -fx-font-weight: 700; -fx-font-size: 12.5px;");
                Label m = new Label(n.getMessage());
                m.getStyleClass().add("muted-text");
                m.setWrapText(true);
                item.getChildren().addAll(t, m);
                item.setPadding(new Insets(8));
                item.setStyle("-fx-background-color: rgba(40,123,239,0.05); -fx-background-radius: 8px;");
                list.getChildren().add(item);
            }
        }
        ScrollPane scroller = new ScrollPane(list);
        scroller.setFitToWidth(true);
        scroller.setMaxHeight(320);
        scroller.getStyleClass().add("scroll-pane");

        box.getChildren().addAll(header, scroller);
        popup.getContent().add(box);
        popup.setAutoHide(true);
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, bounds.getMinX() - 260, bounds.getMaxY() + 10);
    }

    // ============================= NAVIGATION =============================

    /** Opens the Admin user-management page with a specific role filter. */
    public void navigateUsers(Role roleFilter) {
        activeNav = "Users";
        highlightActive();
        if (breadcrumbLabel != null) breadcrumbLabel.setText("Home  ›  Users");
        try {
            javafx.scene.Node content = AdminPages.users(this, roleFilter);
            contentArea.getChildren().setAll(content);
            AnimationUtil.slideInFromBottom(content, 18, 320);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Something went wrong while loading user management."
                    : ex.getMessage();
            Toast.error("Could not open Users", message);
            javafx.scene.Node fallback = UIComponents.emptyState("Unable to load users", "Please try again.");
            contentArea.getChildren().setAll(fallback);
        }
    }

    public void navigate(String page) {
        activeNav = page;
        highlightActive();
        if (breadcrumbLabel != null) breadcrumbLabel.setText("Home  ›  " + page);

        try {
            javafx.scene.Node content = buildPageContent(page);
            contentArea.getChildren().setAll(content);
            AnimationUtil.slideInFromBottom(content, 18, 320);
        } catch (RuntimeException ex) {
            // A page-level database/UI failure must never make the rest of the dashboard unusable.
            String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Something went wrong while loading this section."
                    : ex.getMessage();
            Toast.error("Could not open " + page, message);
            javafx.scene.Node fallback = UIComponents.emptyState("Unable to load this section", "The previous page is still available. Please try again.");
            contentArea.getChildren().setAll(fallback);
        }
    }

    private javafx.scene.Node buildPageContent(String page) {
        Role role = currentUser.getRole();
        if (page.equals("Profile")) return new ProfilePage(this, currentUser).build();
        if (page.equals("Downloads")) return DownloadsPage.build(this);

        if (role == Role.ADMIN) {
            return switch (page) {
                case "Dashboard" -> AdminPages.dashboard(this);
                case "Users" -> AdminPages.users(this);
                case "Courses" -> AdminPages.courses(this);
                case "Analytics" -> AdminPages.analytics(this);
                case "Settings" -> AdminPages.settings(this);
                default -> UIComponents.emptyState("Coming soon", "This section is under construction.");
            };
        } else if (role == Role.INSTRUCTOR) {
            return switch (page) {
                case "Dashboard" -> InstructorPages.dashboard(this, currentUser);
                case "My Courses" -> InstructorPages.myCourses(this, currentUser);
                case "Quizzes" -> InstructorPages.quizzes(this, currentUser);
                case "Assignments" -> InstructorPages.assignments(this, currentUser);
                case "Students" -> InstructorPages.students(this, currentUser);
                default -> UIComponents.emptyState("Coming soon", "This section is under construction.");
            };
        } else {
            return switch (page) {
                case "Dashboard" -> StudentPages.dashboard(this, currentUser);
                case "Browse Courses" -> StudentPages.browseCourses(this, currentUser);
                case "My Courses" -> StudentPages.myCourses(this, currentUser);
                case "Quizzes" -> StudentPages.quizzes(this, currentUser);
                case "Assignments" -> StudentPages.assignments(this, currentUser);
                case "Quiz Results" -> StudentPages.quizResults(this, currentUser);
                default -> UIComponents.emptyState("Coming soon", "This section is under construction.");
            };
        }
    }

    public void refreshCurrent() { navigate(activeNav); }

    public void rememberSelectedCourse(long courseId) { rememberSelectedCourse("Quizzes", courseId); }

    public void rememberSelectedCourse(String page, long courseId) { selectedCourseIds.put(page, courseId); }

    public Long getSelectedCourseId() { return getSelectedCourseId("Quizzes"); }

    public Long getSelectedCourseId(String page) { return selectedCourseIds.get(page); }
    public void openLearning(long courseId) {
        contentArea.getChildren().setAll(StudentPages.learning(this, currentUser, courseId));
    }
    public void openQuiz(long quizId, long courseId) {
        contentArea.getChildren().setAll(StudentPages.takeQuiz(this, currentUser, quizId, courseId));
    }
    public void openAssignmentDetail(long assignmentId) {
        contentArea.getChildren().setAll(StudentPages.assignmentDetail(this, currentUser, assignmentId));
    }
    public void openCourseContentEditor(long courseId) {
        contentArea.getChildren().setAll(InstructorPages.courseContent(this, courseId));
    }
    public void openQuizEditor(long quizId, long courseId) {
        contentArea.getChildren().setAll(InstructorPages.quizEditor(this, quizId, courseId));
    }
    public void openGrading(long assignmentId) {
        contentArea.getChildren().setAll(InstructorPages.gradingPage(this, assignmentId));
    }
    /** Pushes arbitrary transient content (e.g. a quiz result screen) into the content area without a named nav page. */
    public void showTransientContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    public void refreshBranding() {
        if (logoLabel != null) logoLabel.setText("✦ " + AppSettings.platformName());
    }

    public SceneManager getSceneManager() { return sceneManager; }
    public User getCurrentUser() { return currentUser; }
}
