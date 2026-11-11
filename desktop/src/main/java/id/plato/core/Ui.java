package id.plato.core;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Komponen tampilan kecil yang dipakai lintas fitur. */
public final class Ui {
    private Ui() {
    }

    public static FontIcon icon(String code, int size) {
        FontIcon icon = new FontIcon(code);
        icon.setIconSize(size);
        icon.getStyleClass().add("icon");
        return icon;
    }

    public static Label label(String text, String... styles) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styles);
        return label;
    }

    public static Label wrapped(String text, String... styles) {
        Label label = label(text, styles);
        label.setWrapText(true);
        return label;
    }

    /** @param tone green, amber, red, atau gray */
    public static Label chip(String text, String tone) {
        Label chip = label(text, "chip", "chip-" + tone);
        chip.setMinWidth(Region.USE_PREF_SIZE);
        return chip;
    }

    public static VBox card(Node... children) {
        VBox card = new VBox(children);
        card.getStyleClass().add("card");
        return card;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    public static HBox row(double spacing, Node... children) {
        HBox row = new HBox(spacing, children);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static Button primary(String text, String icon) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-primary");
        if (icon != null) {
            button.setGraphic(icon(icon, 16));
        }
        return button;
    }

    public static Button secondary(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-secondary");
        return button;
    }

    public static Button danger(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-danger");
        return button;
    }

    public static Button iconButton(String icon) {
        Button button = new Button();
        button.setGraphic(icon(icon, 16));
        button.getStyleClass().add("btn-icon");
        return button;
    }

    public static StackPane avatar(String name, double size) {
        Circle circle = new Circle(size / 2);
        circle.getStyleClass().add("avatar-circle");
        Label initials = label(initials(name), "avatar-text");
        initials.setStyle("-fx-font-size: " + Math.round(size * 0.36) + "px;");
        return new StackPane(circle, initials);
    }

    public static String initials(String name) {
        return Arrays.stream(name.trim().split("\\s+"))
                .limit(2)
                .map(part -> part.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }

    public static HBox progress(int value) {
        Region fill = new Region();
        fill.getStyleClass().add("progress-fill");
        Region rest = new Region();
        rest.getStyleClass().add("progress-track");
        HBox bar = new HBox(fill, rest);
        bar.getStyleClass().add("progress");
        bar.setMinHeight(6);
        bar.setMaxHeight(6);
        bar.widthProperty().addListener((obs, old, width) -> {
            double filled = width.doubleValue() * value / 100.0;
            fill.setMinWidth(filled);
            fill.setPrefWidth(filled);
            fill.setMaxWidth(filled);
        });
        HBox.setHgrow(rest, Priority.ALWAYS);
        HBox.setHgrow(bar, Priority.ALWAYS);
        return bar;
    }

    public static VBox empty(String icon, String title, String hint) {
        VBox box = new VBox(8, icon(icon, 28), label(title, "empty-title"), wrapped(hint, "muted"));
        box.getStyleClass().add("empty");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static VBox loading() {
        VBox box = new VBox(label("Memuat data…", "muted"));
        box.getStyleClass().add("empty");
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
