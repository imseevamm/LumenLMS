package com.lms.ui;

import com.lms.model.User;
import com.lms.model.Role;
import com.lms.service.AuthService;
import com.lms.util.AppSettings;
import com.lms.util.RememberMeStore;
import com.lms.util.BackgroundTask;
import com.lms.util.AnimationUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

/** Animated login screen: split hero panel + credential form, with validation and loading state. */
public class LoginView {

    private final SceneManager sceneManager;
    private final AuthService authService = new AuthService();

    public LoginView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public StackPane build() {
        StackPane root = new StackPane();
        root.getStyleClass().add("app-background");

        // Decorative floating glow shapes
        Circle glow1 = new Circle(220, Color.web("#287BEF", 0.12));
        glow1.setTranslateX(-480); glow1.setTranslateY(-260);
        Circle glow2 = new Circle(260, Color.web("#4B8FF7", 0.10));
        glow2.setTranslateX(520); glow2.setTranslateY(280);
        Circle glow3 = new Circle(180, Color.web("#7B61FF", 0.10));
        glow3.setTranslateX(460); glow3.setTranslateY(-320);

        HBox split = new HBox();
        split.setAlignment(Pos.CENTER);

        VBox hero = buildHeroPanel();
        VBox formPanel = buildFormPanel();

        split.getChildren().addAll(hero, formPanel);

        root.getChildren().addAll(glow1, glow2, glow3, split);
        Toast.attach(root);

        AnimationUtil.fadeIn(split, 420);
        return root;
    }

    private VBox buildHeroPanel() {
        VBox hero = new VBox(18);
        hero.setPrefWidth(520);
        hero.setPadding(new Insets(58));
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("hero-card");
        hero.setMaxHeight(560);

        Label brand = new Label("Lumen LMS");
        brand.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 24));
        brand.setTextFill(Color.WHITE);

        Label tagline = new Label("Learn. Build. Grow.");
        tagline.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 15));
        tagline.setStyle("-fx-font-weight: 700;");
        tagline.setTextFill(Color.web("#FFE29A"));

        Label headline = new Label("Learning Made Easy");
        headline.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 38));
        headline.setStyle("-fx-font-style: italic;");
        headline.setTextFill(Color.WHITE);
        headline.setWrapText(true);

        Label sub = new Label(
                "Your journey to better learning starts here. Explore courses, build valuable skills, " +
                "track your progress, and turn your knowledge into growth.");
        sub.setWrapText(true);
        sub.setTextFill(Color.web("#EEF3FF"));
        sub.setFont(Font.font("Segoe UI", 14));
        sub.setLineSpacing(3);

        StackPane illustration = Illustrations.learnerComposition(150);
        HBox illustrationRow = new HBox(illustration);
        illustrationRow.setAlignment(Pos.CENTER_RIGHT);
        illustrationRow.setMaxWidth(Double.MAX_VALUE);
        illustrationRow.setPadding(new Insets(2, 0, 0, 0));

        hero.getChildren().addAll(brand, tagline, headline, sub, illustrationRow);
        AnimationUtil.slideInFromBottom(hero, 30, 500);
        return hero;
    }

    private HBox featureRow(String text) {
        Label dot = new Label("●");
        dot.setTextFill(Color.web("#FFC857"));
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Segoe UI", 13));
        HBox row = new HBox(10, dot, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildFormPanel() {
        VBox panel = new VBox(16);
        panel.setPrefWidth(540);
        panel.setMaxWidth(540);
        panel.setPadding(new Insets(46));
        panel.setAlignment(Pos.CENTER);

        Label welcome = new Label("Welcome back");
        welcome.getStyleClass().add("hero-title");
        welcome.setStyle("-fx-font-size: 28px;");
        Label sub = UIComponents.pageSubtitle("Sign in to continue to your dashboard.");

        Label roleTitle = new Label("Choose your role");
        roleTitle.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 13));
        roleTitle.setTextFill(Color.web("#33425F"));

        final Role[] selectedRole = {
                RememberMeStore.hasRememberedLogin() ? RememberMeStore.role() : Role.STUDENT
        };
        StackPane studentCard = roleCard("01", "Student", "Learn, practice & track progress", Role.STUDENT);
        StackPane instructorCard = roleCard("02", "Instructor", "Create, teach & grade", Role.INSTRUCTOR);
        HBox roleCards = new HBox(12, studentCard, instructorCard);
        roleCards.setAlignment(Pos.CENTER);
        HBox.setHgrow(studentCard, Priority.ALWAYS);
        HBox.setHgrow(instructorCard, Priority.ALWAYS);
        if (selectedRole[0] == Role.STUDENT) {
            studentCard.getStyleClass().add("role-card-selected");
        } else if (selectedRole[0] == Role.INSTRUCTOR) {
            instructorCard.getStyleClass().add("role-card-selected");
        }

        Runnable refreshRoleCards = () -> {
            studentCard.getStyleClass().remove("role-card-selected");
            instructorCard.getStyleClass().remove("role-card-selected");
            if (selectedRole[0] == Role.STUDENT) {
                studentCard.getStyleClass().add("role-card-selected");
            } else if (selectedRole[0] == Role.INSTRUCTOR) {
                instructorCard.getStyleClass().add("role-card-selected");
            }
        };
        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        String rememberedEmail = RememberMeStore.email();
        String rememberedPassword = RememberMeStore.password();
        if (!rememberedEmail.isBlank()) {
            emailField.setText(rememberedEmail);
        }
        emailField.getStyleClass().add("text-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");
        if (!rememberedPassword.isBlank()) {
            passwordField.setText(rememberedPassword);
        }
        TextField shownPassword = new TextField();
        shownPassword.setPromptText("Password");
        shownPassword.getStyleClass().add("text-field");
        shownPassword.setManaged(false);
        shownPassword.setVisible(false);
        shownPassword.textProperty().bindBidirectional(passwordField.textProperty());

        CheckBox showPassword = new CheckBox("Show password");
        showPassword.selectedProperty().addListener((obs, was, show) -> {
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            shownPassword.setVisible(show);
            shownPassword.setManaged(show);
        });

        StackPane passwordStack = new StackPane(passwordField, shownPassword);

        CheckBox rememberMe = new CheckBox("Remember me");
        rememberMe.setSelected(!rememberedEmail.isBlank() && !rememberedPassword.isBlank());
        Label forgotPassword = new Label("Need a password reset?");
        forgotPassword.getStyleClass().add("link-text");
        HBox optionsRow = new HBox(rememberMe, UIComponents.spacer(), forgotPassword);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        forgotPassword.setOnMouseClicked(e -> showForgotPasswordInfo());

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-text");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(18, 18);
        spinner.setVisible(false);
        spinner.setManaged(false);

        Button loginButton = UIComponents.primaryButton("Sign In");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox buttonRow = new HBox(10, loginButton, spinner);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        loginButton.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            loginButton.setDisable(true);
            spinner.setVisible(true);
            spinner.setManaged(true);

            String email = emailField.getText().trim();
            String password = passwordField.getText();
            BackgroundTask.run(
                    () -> {
                        try {
                            Thread.sleep(320);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Login was interrupted.", ex);
                        }
                        return authService.login(email, password, selectedRole[0]);
                    },
                    user -> {
                        loginButton.setDisable(false);
                        spinner.setVisible(false); spinner.setManaged(false);
                        if (rememberMe.isSelected()) {
                            RememberMeStore.save(emailField.getText(), passwordField.getText(), selectedRole[0]);
                        } else {
                            RememberMeStore.clear();
                        }
                        Toast.success("Welcome back!", "Signed in successfully.");
                        sceneManager.goToDashboard();
                    },
                    ex -> {
                        loginButton.setDisable(false);
                        spinner.setVisible(false); spinner.setManaged(false);
                        String msg = ex instanceof AuthService.AuthException
                                ? ex.getMessage() : "Something went wrong. Please try again.";
                        errorLabel.setText(msg);
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                    });
        });

        Label signupPrompt = new Label("Don't have an account?");
        signupPrompt.getStyleClass().add("muted-text");
        Label signupLink = new Label("Create one");
        signupLink.getStyleClass().add("link-text");
        signupLink.setOnMouseClicked(e -> sceneManager.goToSignup());
        HBox signupRow = new HBox(6, signupPrompt, signupLink);
        signupRow.setAlignment(Pos.CENTER);

        Label adminLink = new Label("Administrator sign-in");
        adminLink.getStyleClass().add("link-text");
        adminLink.setStyle("-fx-font-size: 11.5px;");
        adminLink.setOnMouseClicked(e -> {
            selectedRole[0] = Role.ADMIN;
            studentCard.getStyleClass().remove("role-card-selected");
            instructorCard.getStyleClass().remove("role-card-selected");
            // Only switches the login type; credentials are never pre-filled.
            adminLink.setText("Administrator selected");
            emailField.requestFocus();
        });
        studentCard.setOnMouseClicked(e -> {
            selectedRole[0] = Role.STUDENT;
            refreshRoleCards.run();
            adminLink.setText("Administrator sign-in");
        });
        instructorCard.setOnMouseClicked(e -> {
            selectedRole[0] = Role.INSTRUCTOR;
            refreshRoleCards.run();
            adminLink.setText("Administrator sign-in");
        });

        panel.getChildren().addAll(welcome, sub, roleTitle, roleCards, emailField, passwordStack, showPassword,
                optionsRow, errorLabel, buttonRow, new Separator(), signupRow, adminLink);

        AnimationUtil.slideInFromRight(panel, 40, 480);
        return panel;
    }

    private StackPane roleCard(String number, String title, String description, Role role) {
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(14, 16, 14, 16));

        Label numberLabel = new Label(number);
        numberLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 10));
        numberLabel.setTextFill(Color.web("#7B61FF"));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 15));
        titleLabel.setTextFill(Color.web("#16233F"));

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setFont(Font.font("Segoe UI", 11));
        descriptionLabel.setTextFill(Color.web("#6B7A99"));

        content.getChildren().addAll(numberLabel, titleLabel, descriptionLabel);
        StackPane card = new StackPane(content);
        card.setPrefHeight(94);
        card.setMinHeight(94);
        card.setMaxHeight(94);
        card.setPrefWidth(205);
        card.getStyleClass().add("role-card");
        card.setUserData(role);

        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.035);
            st.setToY(1.035);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        return card;
    }

    private void showForgotPasswordInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        UIComponents.styleDialog(alert);
        alert.setTitle("Password reset");
        alert.setHeaderText("Need a new password?");
        alert.setContentText(
                "For security, password resets are handled by an administrator.\n\n" +
                "Contact: " + AppSettings.supportEmail() + "\n\n" +
                "An administrator can reset your password from Admin → Users → Edit User.\n\n" +
                "After signing in, you can also change it from Profile → Change Password."
        );
        alert.showAndWait();
    }
}
