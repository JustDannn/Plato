package id.plato.core;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

public class HelpController {
    private record Topic(String question, String answer) {
    }

    private static final List<Topic> TOPICS = List.of(
            new Topic("Bagaimana cara mengajukan tugas?",
                    "Buka menu Tugas lalu tekan Ajukan Tugas. Pengajuan tampil ke anggota kelas setelah disetujui Admin."),
            new Topic("Mengapa tugas saya belum muncul di kalender?",
                    "Kalender hanya menampilkan tugas yang sudah disetujui dari kelas yang kamu ikuti."),
            new Topic("Bagaimana progres proyek dihitung?",
                    "Progres proyek adalah rata-rata progres seluruh milestone. Tugas tanpa milestone diisi secara manual."),
            new Topic("Bagaimana cara bergabung ke kelas?",
                    "Buka menu Kelas, lalu pilih Gabung pada kelas yang tersedia di angkatanmu."),
            new Topic("Aplikasi tidak dapat terhubung ke server?",
                    "Pastikan Docker Desktop aktif dan jalankan docker compose up -d di folder proyek."));

    @FXML private VBox list;

    @FXML
    private void initialize() {
        for (Topic topic : TOPICS) {
            list.getChildren().add(Ui.card(Ui.label(topic.question(), "h3"), Ui.wrapped(topic.answer(), "muted")));
        }
    }
}
