package com.lms.ui;

import com.lms.model.Role;
import com.lms.security.PasswordUtil;
import com.lms.service.AuthService;
import com.lms.util.AnimationUtil;
import com.lms.util.AppSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Animated sign-up screen with role selection (Student/Instructor only), strength meter and validation. */
public class SignupView {

    private final SceneManager sceneManager;
    private final AuthService authService = new AuthService();

    public SignupView(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public StackPane build() {
        StackPane root = new StackPane();
        root.getStyleClass().add("app-background");

        Circle glow1 = new Circle(240, Color.web("#7B61FF", 0.10));
        glow1.setTranslateX(-500); glow1.setTranslateY(300);
        Circle glow2 = new Circle(200, Color.web("#4B8FF7", 0.10));
        glow2.setTranslateX(520); glow2.setTranslateY(-260);

        VBox panel = new VBox(14);
        panel.setMaxWidth(520);
        panel.setPadding(new Insets(44));
        panel.getStyleClass().add("glass-card");
        panel.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Create your account");
        title.getStyleClass().add("hero-title");
        title.setStyle("-fx-font-size: 26px;");
        Label sub = UIComponents.pageSubtitle("Join " + AppSettings.platformName() + " as a student or instructor.");

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email address");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");

        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        Label strengthLabel = new Label("Password strength");
        strengthLabel.getStyleClass().add("muted-text");
        passwordField.textProperty().addListener((obs, old, val) -> {
            int score = PasswordUtil.strengthScore(val);
            strengthBar.setProgress(score / 4.0);
            String[] labels = {"Very weak", "Weak", "Fair", "Good", "Strong"};
            strengthLabel.setText("Password strength: " + labels[score]);
        });

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton studentRadio = new RadioButton("Student");
        studentRadio.setToggleGroup(roleGroup);
        studentRadio.setSelected(true);
        studentRadio.setStyle("-fx-text-fill: -text;");
        RadioButton instructorRadio = new RadioButton("Instructor");
        instructorRadio.setToggleGroup(roleGroup);
        instructorRadio.setStyle("-fx-text-fill: -text;");
        HBox roleRow = new HBox(20, studentRadio, instructorRadio);
        roleRow.setAlignment(Pos.CENTER_LEFT);
        Label roleLabel = new Label("I am signing up as a:");
        roleLabel.getStyleClass().add("muted-text");

        CheckBox termsCheck = new CheckBox("I agree to the Terms of Service and Privacy Policy");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-text");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button signupButton = UIComponents.primaryButton("Create Account");
        signupButton.setMaxWidth(Double.MAX_VALUE);

        signupButton.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            if (!termsCheck.isSelected()) {
                showError(errorLabel, "You must accept the Terms of Service to continue.");
                return;
            }
            Role role = studentRadio.isSelected() ? Role.STUDENT : Role.INSTRUCTOR;
            try {
                authService.signup(nameField.getText(), emailField.getText(),
                        passwordField.getText(), confirmField.getText(), role);
                Toast.success("Account created!", "You can now sign in with your new account.");
                sceneManager.goToLogin();
            } catch (AuthService.AuthException ex) {
                showError(errorLabel, ex.getMessage());
            } catch (Exception ex) {
                showError(errorLabel, "Registration failed. Please try again.");
            }
        });

        Label loginPrompt = new Label("Already have an account?");
        loginPrompt.getStyleClass().add("muted-text");
        Label loginLink = new Label("Sign in");
        loginLink.getStyleClass().add("link-text");
        loginLink.setOnMouseClicked(e -> sceneManager.goToLogin());
        HBox loginRow = new HBox(6, loginPrompt, loginLink);
        loginRow.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(title, sub, nameField, emailField, passwordField,
                strengthBar, strengthLabel, confirmField, roleLabel, roleRow, termsCheck,
                errorLabel, signupButton, new Separator(), loginRow);

        StackPane wrapper = new StackPane(panel);
        wrapper.setPadding(new Insets(30));
        root.getChildren().addAll(glow1, glow2, wrapper);
        Toast.attach(root);

        AnimationUtil.scaleIn(panel, 420);
        return root;
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }
}
