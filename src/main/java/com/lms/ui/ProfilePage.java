package com.lms.ui;

import com.lms.model.User;
import com.lms.service.AuthService;
import com.lms.service.UserService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class ProfilePage {

    private final DashboardShell shell;
    private final User user;
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();

    public ProfilePage(DashboardShell shell, User user) {
        this.shell = shell;
        this.user = user;
    }

    public Node build() {
        VBox root = new VBox(24);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = UIComponents.avatarWithInitials(user.initials(), 84);
        VBox names = new VBox(4);
        Label name = UIComponents.pageTitle(user.getFullName());
        Label role = UIComponents.badge(user.getRole().display(), "badge-info");
        Label email = new Label(user.getEmail());
        email.getStyleClass().add("muted-text");
        names.getChildren().addAll(name, role, email);
        header.getChildren().addAll(avatar, names);

        Label since = new Label("Member since " + (user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) : "—"));
        since.getStyleClass().add("muted-text");

        // Edit profile card
        TextField nameField = new TextField(user.getFullName());
        TextField emailField = new TextField(user.getEmail());
        TextField bioField = new TextField(user.getBio() == null ? "" : user.getBio());
        bioField.setPromptText("Short bio");

        var saveBtn = UIComponents.primaryButton("Save Changes");
        saveBtn.setOnAction(e -> {
            try {
                user.setFullName(nameField.getText());
                user.setEmail(emailField.getText());
                user.setBio(bioField.getText());
                userService.updateUser(user);
                Toast.success("Profile updated", "Your changes have been saved.");
                shell.refreshCurrent();
            } catch (UserService.UserServiceException ex) {
                Toast.error("Update failed", ex.getMessage());
            }
        });

        VBox editCard = UIComponents.card(
                UIComponents.sectionTitle("Edit Profile"),
                labeled("Full name", nameField),
                labeled("Email", emailField),
                labeled("Bio", bioField),
                saveBtn
        );

        // Change password card
        PasswordField currentPw = new PasswordField();
        currentPw.setPromptText("Current password");
        PasswordField newPw = new PasswordField();
        newPw.setPromptText("New password");
        PasswordField confirmPw = new PasswordField();
        confirmPw.setPromptText("Confirm new password");

        var changePwBtn = UIComponents.secondaryButton("Update Password");
        changePwBtn.setOnAction(e -> {
            try {
                authService.changePassword(user.getId(), currentPw.getText(), newPw.getText(), confirmPw.getText());
                currentPw.clear(); newPw.clear(); confirmPw.clear();
                Toast.success("Password updated", "Your password has been changed.");
            } catch (AuthService.AuthException ex) {
                Toast.error("Could not update password", ex.getMessage());
            }
        });

        VBox pwCard = UIComponents.card(
                UIComponents.sectionTitle("Change Password"),
                currentPw, newPw, confirmPw, changePwBtn
        );

        String rollDisplay = user.getRole() == com.lms.model.Role.STUDENT
                ? ((user.getRollNumber() == null || user.getRollNumber().isBlank())
                    ? String.format("ST25%06d", user.getId())
                    : user.getRollNumber())
                : "Not applicable";
        VBox rollCard = UIComponents.card(
                UIComponents.sectionTitle("Account Information"),
                labeled("Roll number", new Label(rollDisplay)),
                labeled("Role", new Label(user.getRole().display()))
        );
        rollCard.setPrefWidth(260);

        HBox cardsRow = new HBox(20, editCard, pwCard, rollCard);
        HBox.setHgrow(editCard, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(pwCard, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(rollCard, javafx.scene.layout.Priority.SOMETIMES);
        editCard.setPrefWidth(400);
        pwCard.setPrefWidth(400);

        root.getChildren().addAll(header, since, cardsRow);
        return root;
    }

    private VBox labeled(String label, Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("muted-text");
        return new VBox(4, l, field);
    }
}
