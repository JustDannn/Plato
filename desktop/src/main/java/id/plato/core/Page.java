package id.plato.core;

/** Controller halaman yang dapat dibuka Router. */
public interface Page {
    /** @param param data navigasi, misalnya id tugas; boleh null */
    void onShow(Object param);
}
