package id.plato.core;

import id.plato.user.LoginController;
import id.plato.user.User;
import id.plato.user.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class PlatoApp extends Application {
    private static Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    public static void loadFonts() {
        for (String weight : List.of("Regular", "Medium", "SemiBold", "Bold")) {
            Font.loadFont(PlatoApp.class.getResourceAsStream("/id/plato/fonts/Inter-" + weight + ".ttf"), 14);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        loadFonts();
        User.useGateway(new UserService());
        stage.setTitle("PLATO");
        stage.setMinWidth(1120);
        stage.setMinHeight(720);
        stage.setScene(new Scene(load("/id/plato/views/user/login.fxml"), 1320, 860));
        applyTheme(stage.getScene());
        stage.show();
    }

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().setAll(PlatoApp.class.getResource("/id/plato/css/theme.css").toExternalForm());
    }

    public static Stage stage() {
        return stage;
    }

    public static void showShell() {
        stage.getScene().setRoot(load("/id/plato/views/shell.fxml"));
    }

    public static void showLogin() {
        stage.getScene().setRoot(load("/id/plato/views/user/login.fxml"));
    }

    public static void sessionExpired() {
        Session.end();
        FXMLLoader loader = loader("/id/plato/views/user/login.fxml");
        stage.getScene().setRoot(loadFrom(loader));
        ((LoginController) loader.getController()).showMessage("Sesi berakhir. Silakan masuk kembali.");
    }

    private static FXMLLoader loader(String resource) {
        return new FXMLLoader(PlatoApp.class.getResource(resource));
    }

    private static Parent load(String resource) {
        return loadFrom(loader(resource));
    }

    private static Parent loadFrom(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memuat tampilan", e);
        }
    }
}
