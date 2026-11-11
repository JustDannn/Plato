package id.plato.task;

public enum TaskType {
    INDIVIDUAL("Individu"),
    GROUP("Kelompok"),
    PROJECT("Proyek");

    private final String label;

    TaskType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
