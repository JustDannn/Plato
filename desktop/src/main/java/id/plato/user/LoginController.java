package id.plato.user;

import id.plato.core.Async;
import id.plato.core.Dialogs;
import id.plato.core.PlatoApp;
import id.plato.core.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label message;
    @FXML private Button submitButton;

    private final UserService service = new UserService();

    @FXML
    private void submit() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Email dan password wajib diisi.");
            return;
        }
        submitButton.setDisable(true);
        submitButton.setText("Memproses…");
        Async.run(() -> service.signIn(email, password), result -> {
            Session.start(result.user(), result.token());
            PlatoApp.showShell();
        }, error -> {
            submitButton.setDisable(false);
            submitButton.setText("Masuk");
            showMessage(Dialogs.messageOf(error));
        });
    }

    public void showMessage(String text) {
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
    }
}
