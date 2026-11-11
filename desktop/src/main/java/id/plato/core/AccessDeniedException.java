package id.plato.core;

/** Dilempar model ketika pengguna bertindak di luar hak aksesnya. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
