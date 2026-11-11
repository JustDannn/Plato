package id.plato.monitoring;

import id.plato.core.Async;
import id.plato.core.Formats;
import id.plato.core.Page;
import id.plato.core.Route;
import id.plato.core.Router;
import id.plato.core.ShellController;
import id.plato.core.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class NotificationsController implements Page {
    private static final String ALL = "Semua";
    private static final String UNREAD = "Belum dibaca";

    @FXML private ComboBox<String> filterBox;
    @FXML private VBox list;

    private final MonitoringService service = new MonitoringService();
    private List<Notification> notifications = List.of();

    @FXML
    private void initialize() {
        filterBox.getItems().setAll(ALL, UNREAD);
        filterBox.setValue(ALL);
        filterBox.setOnAction(event -> render());
    }

    @Override
    public void onShow(Object param) {
        load();
    }

    private void load() {
        Async.run(service::notifications, result -> {
            notifications = result;
            render();
            ShellController.refreshBadge();
        });
    }

    private void render() {
        List<Notification> visible = Notification.getNotification(notifications, UNREAD.equals(filterBox.getValue()));
        list.getChildren().clear();
        if (visible.isEmpty()) {
            list.getChildren().add(Ui.empty("fth-bell", "Tidak ada notifikasi", "Pemberitahuan tugas akan tampil di sini."));
        }
        visible.forEach(item -> list.getChildren().add(row(item)));
    }

    private HBox row(Notification item) {
        HBox row = Ui.row(14,
                Ui.icon(item.getIcon(), 20),
                new VBox(3, Ui.wrapped(item.getMessage(), item.isRead() ? "muted" : "h3"),
                        Ui.label(Formats.dateTime(item.getCreatedAt()), "muted", "small")),
                Ui.spacer());
        if (!item.isRead()) {
            row.getChildren().add(Ui.chip("Baru", "green"));
        }
        row.getStyleClass().addAll("list-row", "list-row-clickable");
        row.setOnMouseClicked(event -> open(item));
        return row;
    }

    private void open(Notification item) {
        Runnable next = () -> {
            item.markAsRead();
            ShellController.refreshBadge();
            if (item.getTaskId() != null) {
                Router.navigate(Route.TASK_DETAIL, item.getTaskId());
            } else {
                render();
            }
        };
        if (item.isRead()) {
            next.run();
        } else {
            Async.runVoid(() -> service.markRead(item.getNotificationId()), next);
        }
    }

    @FXML
    private void markAll() {
        Async.runVoid(service::markAllRead, this::load);
    }
}
