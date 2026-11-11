package id.plato.core;

/** Kegagalan saat berkomunikasi dengan backend. Status 0 berarti server tidak terjangkau. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
