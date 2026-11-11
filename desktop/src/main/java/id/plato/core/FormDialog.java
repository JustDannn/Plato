package id.plato.core;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Dialog formulir yang tetap terbuka dan menampilkan pesan bila penyimpanan gagal. */
public final class FormDialog {
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final VBox body = new VBox(14);
    private final Label error = Ui.wrapped("", "form-error");

    public FormDialog(String title) {
        dialog.initOwner(PlatoApp.stage());
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().getStylesheets().setAll(PlatoApp.stage().getScene().getStylesheets());
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().setPrefWidth(520);
        error.setVisible(false);
        error.setManaged(false);
    }

    public FormDialog field(String label, Node control) {
        body.getChildren().add(new VBox(6, Ui.label(label, "form-label"), control));
        return this;
    }

    public FormDialog node(Node node) {
        body.getChildren().add(node);
        return this;
    }

    /**
     * @param prepare dijalankan di thread UI untuk membaca dan memvalidasi input, lalu mengembalikan pekerjaan jaringan
     * @param onSaved dijalankan setelah berhasil
     */
    public <T> void show(String submitLabel, Supplier<Callable<T>> prepare, Consumer<T> onSaved) {
        body.getChildren().add(error);
        ButtonType submit = new ButtonType(submitLabel, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE), submit);
        Button button = (Button) dialog.getDialogPane().lookupButton(submit);
        button.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            submit(button, prepare, onSaved);
        });
        dialog.showAndWait();
    }

    private <T> void submit(Button button, Supplier<Callable<T>> prepare, Consumer<T> onSaved) {
        Callable<T> work;
        try {
            work = prepare.get();
        } catch (RuntimeException e) {
            showError(e);
            return;
        }
        button.setDisable(true);
        Async.run(work, result -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
            onSaved.accept(result);
        }, e -> {
            button.setDisable(false);
            showError(e);
        });
    }

    private void showError(Throwable e) {
        error.setText(Dialogs.messageOf(e));
        error.setVisible(true);
        error.setManaged(true);
        dialog.getDialogPane().getScene().getWindow().sizeToScene();
    }
}
