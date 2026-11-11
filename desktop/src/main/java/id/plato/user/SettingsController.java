package id.plato.user;

import id.plato.core.Async;
import id.plato.core.Dialogs;
import id.plato.core.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SettingsController {
    @FXML private Label roleLine;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private Label profileMessage;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private Label passwordMessage;

    private final UserService service = new UserService();

    @FXML
    private void initialize() {
        User user = Session.user();
        roleLine.setText(user.getRoleLabel() + " · " + user.getDetailLine());
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
    }

    @FXML
    private void saveProfile() {
        User user = Session.user();
        String name = nameField.getText();
        String email = emailField.getText();
        try {
            User.validateProfile(name, email);
        } catch (IllegalArgumentException e) {
            show(profileMessage, e.getMessage(), false);
            return;
        }
        Async.run(() -> {
            user.updateProfile(name, email);
            return true;
        }, ok -> show(profileMessage, "Profil tersimpan.", true), e -> show(profileMessage, Dialogs.messageOf(e), false));
    }

    @FXML
    private void savePassword() {
        User user = Session.user();
        String current = currentPassword.getText();
        String next = newPassword.getText();
        if (current.isEmpty() || next.length() < 8) {
            show(passwordMessage, "Isi password saat ini dan password baru minimal 8 karakter.", false);
            return;
        }
        Async.run(() -> {
            if (!user.login(user.getEmail(), current)) {
                throw new IllegalArgumentException("Password saat ini salah.");
            }
            service.changePassword(current, next);
            return true;
        }, ok -> {
            currentPassword.clear();
            newPassword.clear();
            show(passwordMessage, "Password berhasil diganti.", true);
        }, e -> show(passwordMessage, Dialogs.messageOf(e), false));
    }

    private void show(Label label, String text, boolean success) {
        label.setText(text);
        label.getStyleClass().removeAll("form-error", "positive");
        label.getStyleClass().add(success ? "positive" : "form-error");
    }
}
