package id.plato.task;

public enum TaskStatus {
    DRAFT("Draf"),
    PENDING("Menunggu"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
