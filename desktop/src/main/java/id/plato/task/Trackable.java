package id.plato.task;

/** Objek yang progresnya dapat diperbarui. */
public interface Trackable {
    void updateProgress(int value);

    int getProgress();
}
