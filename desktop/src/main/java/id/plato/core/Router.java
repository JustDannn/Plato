package id.plato.core;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/** Mengganti halaman di area konten dan mengingat riwayatnya. */
public final class Router {
    private static Pane outlet;
    private static Consumer<Route> onChange = route -> { };
    private static final Deque<Entry> HISTORY = new ArrayDeque<>();

    private record Entry(Route route, Object param) {
    }

    private Router() {
    }

    public static void attach(Pane target, Consumer<Route> listener) {
        outlet = target;
        onChange = listener;
        HISTORY.clear();
    }

    public static void navigate(Route route) {
        navigate(route, null);
    }

    public static void navigate(Route route, Object param) {
        HISTORY.push(new Entry(route, param));
        show(route, param);
    }

    public static void back() {
        if (HISTORY.size() > 1) {
            HISTORY.pop();
            Entry previous = HISTORY.peek();
            show(previous.route(), previous.param());
        }
    }

    public static void reload() {
        Entry current = HISTORY.peek();
        if (current != null) {
            show(current.route(), current.param());
        }
    }

    private static void show(Route route, Object param) {
        try {
            FXMLLoader loader = new FXMLLoader(Router.class.getResource(route.resource()));
            Node view = loader.load();
            outlet.getChildren().setAll(view);
            onChange.accept(route);
            if (loader.getController() instanceof Page page) {
                page.onShow(param);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memuat halaman " + route, e);
        }
    }
}
