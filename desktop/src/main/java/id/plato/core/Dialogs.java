package id.plato.core;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public final class Dialogs {
    private Dialogs() {
    }

    public static void error(Throwable error) {
        Alert alert = prepare(new Alert(Alert.AlertType.ERROR), "Terjadi Kesalahan", messageOf(error));
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message, String confirmLabel) {
        ButtonType yes = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = prepare(new Alert(Alert.AlertType.CONFIRMATION, message, yes, no), title, message);
        return alert.showAndWait().filter(yes::equals).isPresent();
    }

    public static String messageOf(Throwable error) {
        return error.getMessage() != null ? error.getMessage() : "Terjadi kesalahan yang tidak diketahui.";
    }

    private static Alert prepare(Alert alert, String title, String message) {
        alert.initOwner(PlatoApp.stage());
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.setGraphic(null);
        alert.getDialogPane().getStylesheets().setAll(PlatoApp.stage().getScene().getStylesheets());
        return alert;
    }
}
