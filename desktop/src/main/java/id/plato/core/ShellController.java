package id.plato.core;

import id.plato.monitoring.MonitoringService;
import id.plato.user.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Kerangka utama: sidebar dan bilah atas mengikuti menu milik peran pengguna. */
public class ShellController {
    private static final List<Route> OTHER_MENU = List.of(Route.SETTINGS, Route.HELP);
    private static final int EXPANDED_WIDTH = 240;
    private static final int COLLAPSED_WIDTH = 76;
    private static ShellController active;

    @FXML private VBox sidebar;
    @FXML private HBox brand;
    @FXML private Label mainHeading;
    @FXML private Label otherHeading;
    @FXML private VBox mainMenu;
    @FXML private VBox otherMenu;
    @FXML private StackPane content;
    @FXML private TextField searchField;
    @FXML private Button bellButton;
    @FXML private Label bellBadge;
    @FXML private StackPane avatarHolder;
    @FXML private Label brandName;

    private final Map<Route, Button> buttons = new EnumMap<>(Route.class);
    private final MonitoringService monitoring = new MonitoringService();
    private final MonitoringBadgePoller poller = new MonitoringBadgePoller();
    private boolean collapsed;

    @FXML
    private void initialize() {
        active = this;
        User user = Session.user();
        user.getMenu().forEach(route -> mainMenu.getChildren().add(menuButton(route)));
        OTHER_MENU.forEach(route -> otherMenu.getChildren().add(menuButton(route)));
        otherMenu.getChildren().add(logoutButton());

        avatarHolder.getChildren().setAll(Ui.avatar(user.getName(), 40));
        avatarHolder.setOnMouseClicked(event -> Router.navigate(Route.SETTINGS));
        bellButton.setGraphic(Ui.icon("fth-bell", 18));
        bellButton.setOnAction(event -> Router.navigate(Route.NOTIFICATIONS));
        searchField.setPromptText(user.getSearchPrompt());
        searchField.setOnAction(event -> {
            Router.navigate(user.getSearchRoute(), searchField.getText().trim());
            searchField.clear();
        });

        Router.attach(content, this::highlight);
        Router.navigate(Route.DASHBOARD);
        poller.start();
    }

    /** Memperbarui lencana lonceng setelah notifikasi berubah. */
    public static void refreshBadge() {
        if (active != null) {
            active.poller.refresh();
        }
    }

    @FXML
    private void toggleSidebar() {
        collapsed = !collapsed;
        sidebar.setPrefWidth(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH);
        sidebar.setMinWidth(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH);
        sidebar.setMaxWidth(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH);
        for (Node node : List.of(mainHeading, otherHeading, brandName)) {
            node.setVisible(!collapsed);
            node.setManaged(!collapsed);
        }
        for (VBox menu : List.of(mainMenu, otherMenu)) {
            for (Node node : menu.getChildren()) {
                ((Button) node).setContentDisplay(collapsed
                        ? javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
                        : javafx.scene.control.ContentDisplay.LEFT);
            }
        }
    }

    private Button menuButton(Route route) {
        Button button = baseMenuButton(route.title(), route.icon());
        button.setOnAction(event -> Router.navigate(route));
        buttons.put(route, button);
        return button;
    }

    private Button logoutButton() {
        Button button = baseMenuButton("Keluar", "fth-log-out");
        button.setOnAction(event -> {
            poller.stop();
            Async.run(() -> {
                Session.user().logout();
                return true;
            }, ok -> finishLogout(), error -> finishLogout());
        });
        return button;
    }

    private void finishLogout() {
        Session.end();
        PlatoApp.showLogin();
    }

    private Button baseMenuButton(String title, String icon) {
        Button button = new Button(title, Ui.icon(icon, 18));
        button.getStyleClass().add("menu-item");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private void highlight(Route route) {
        Route selected = switch (route) {
            case CLASS_DETAIL -> Route.CLASSES;
            case TASK_DETAIL -> Route.TASKS;
            default -> route;
        };
        buttons.forEach((key, button) -> button.getStyleClass().remove("menu-item-active"));
        Button button = buttons.get(selected);
        if (button != null) {
            button.getStyleClass().add("menu-item-active");
        }
    }

    /** Menanyakan jumlah notifikasi belum dibaca secara berkala. */
    private final class MonitoringBadgePoller {
        private final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(20), event -> refresh()));

        void start() {
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
            refresh();
        }

        void stop() {
            timeline.stop();
        }

        void refresh() {
            Async.run(monitoring::unreadCount, count -> {
                bellBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                bellBadge.setVisible(count > 0);
            }, error -> { });
        }
    }
}
