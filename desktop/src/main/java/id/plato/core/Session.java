package id.plato.core;

import id.plato.user.User;

/** Menyimpan pengguna aktif sebagai referensi bertipe User (polymorphism). */
public final class Session {
    private static User current;

    private Session() {
    }

    public static void start(User user, String token) {
        current = user;
        ApiClient.get().setToken(token);
    }

    public static User user() {
        return current;
    }

    public static boolean isActive() {
        return current != null;
    }

    public static void end() {
        current = null;
        ApiClient.get().setToken(null);
    }
}
