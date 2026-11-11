# PLATO: Platform Pelacakan dan Pengarsipan Tugas Kuliah

Aplikasi desktop untuk mencatat, memvalidasi, dan memantau tugas kuliah. Dikembangkan oleh Kelompok 9, Sains Data, Telkom University Surabaya.

| Bagian | Teknologi | Cara jalan |
|---|---|---|
| Aplikasi desktop | Java 21, JavaFX 21, Maven | Langsung di komputer masing-masing |
| Backend REST API | Bun, TypeScript | Docker |
| Database | PostgreSQL 17 | Docker |

```
┌───────────────────┐    REST/JSON     ┌──────────────┐     SQL     ┌──────────────┐
│  JavaFX (desktop) │ ───────────────► │ Bun (backend)│ ──────────► │  PostgreSQL  │
│  di komputer kamu │  localhost:8080  │    Docker    │             │    Docker    │
└───────────────────┘                  └──────────────┘             └──────────────┘
```

Backend dan database berjalan di Docker agar versinya sama di semua perangkat. Aplikasi desktop berjalan di luar Docker karena membutuhkan layar komputer.

Setiap anggota menjalankan salinan backend dan database sendiri. Data di komputermu tidak terlihat oleh anggota lain, sedangkan struktur database dibagikan lewat Git.

## Daftar isi

1. [Yang perlu dipasang](#1-yang-perlu-dipasang)
2. [Menjalankan pertama kali](#2-menjalankan-pertama-kali)
3. [Akun contoh](#3-akun-contoh)
4. [Perintah Docker sehari-hari](#4-perintah-docker-sehari-hari)
5. [Mengembangkan backend](#5-mengembangkan-backend)
6. [Mengembangkan aplikasi desktop](#6-mengembangkan-aplikasi-desktop)
7. [Pengujian](#7-pengujian)
8. [Pembagian tugas dan folder](#8-pembagian-tugas-dan-folder)
9. [Alur kerja Git](#9-alur-kerja-git)
10. [Daftar API](#10-daftar-api)
11. [Konfigurasi](#11-konfigurasi)
12. [Mengatasi masalah](#12-mengatasi-masalah)

## 1. Yang perlu dipasang

Hanya tiga hal berikut. Bun, PostgreSQL, dan Maven **tidak perlu** dipasang.

| Alat | Windows | macOS | Linux (Ubuntu/Debian) |
|---|---|---|---|
| Git | `winget install Git.Git` | `brew install git` | `sudo apt install git` |
| JDK 21 | `winget install EclipseAdoptium.Temurin.21.JDK` | `brew install --cask temurin@21` | `sudo apt install openjdk-21-jdk` |
| Docker | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | [Docker Engine dan plugin Compose](https://docs.docker.com/engine/install/) |

Catatan:

- **Windows:** saat memasang Docker Desktop, pilih backend WSL 2. Restart komputer bila diminta.
- **Mac Apple Silicon (M1 ke atas):** pastikan JDK berarsitektur `aarch64`. Pemasangan lewat perintah di atas sudah otomatis benar.
- **Linux:** agar `docker` tidak perlu `sudo`, jalankan `sudo usermod -aG docker $USER` lalu login ulang.

Periksa pemasangan di terminal baru:

```bash
git --version
java -version            # harus menampilkan versi 21
docker --version
docker compose version
```

## 2. Menjalankan pertama kali

**Langkah 1. Buka Docker Desktop** dan tunggu sampai statusnya *running*. Di Linux, pastikan layanan `docker` aktif.

**Langkah 2. Ambil kode dan masuk ke folder proyek.**

```bash
git clone <URL_REPOSITORY> plato
cd plato
```

**Langkah 3. Buat file konfigurasi.**

```bash
# macOS / Linux / Git Bash
cp .env.example .env

# Windows PowerShell atau CMD
copy .env.example .env
```

**Langkah 4. Jalankan backend dan database.**

```bash
docker compose up -d --build
```

Proses pertama memerlukan beberapa menit untuk mengunduh image. Database, tabel, dan data contoh dibuat otomatis.

**Langkah 5. Pastikan backend sudah hidup.**

```bash
docker compose ps
curl http://localhost:8080/api/v1/health
```

Kedua layanan harus berstatus `healthy`, dan `curl` harus membalas `{"status":"ok"}`. Di Windows PowerShell, gunakan `curl.exe` atau buka alamat tersebut di browser.

**Langkah 6. Jalankan aplikasi desktop.**

```bash
cd desktop

# macOS / Linux / Git Bash
./mvnw javafx:run

# Windows PowerShell atau CMD
.\mvnw.cmd javafx:run
```

Proses pertama mengunduh Maven dan dependensi secara otomatis. Setelah jendela terbuka, masuk memakai akun di bawah.

## 3. Akun contoh

Password semua akun: **`plato12345`**

| Peran | Email | Keterangan |
|---|---|---|
| Mahasiswa | `fattah@plato.test` | Anggota 3 kelas, memiliki tugas dan progres |
| Mahasiswa | `annisa@plato.test`, `talitha@plato.test`, `nashwa@plato.test` | Anggota 3 kelas |
| Admin | `admin.pbo@plato.test` | Admin kelas Pemrograman Berorientasi Objek |
| Admin | `admin.stat@plato.test` | Admin kelas Statistika Inferensial |
| Admin | `admin.db@plato.test` | Admin kelas Basis Data |
| SuperAdmin | `superadmin@plato.test` | Tingkat global, seluruh angkatan |
| SuperAdmin | `koordinator2024@plato.test` | Hanya angkatan Sains Data 2024 |

Akun ini hanya untuk pengembangan lokal. Tidak ada registrasi mandiri, karena pengguna baru dibuat oleh SuperAdmin.

Untuk mencoba alur utama:

1. Masuk sebagai `fattah@plato.test`, buka **Tugas**, lalu **Ajukan Tugas** pada kelas Pemrograman Berorientasi Objek.
2. Keluar, lalu masuk sebagai `admin.pbo@plato.test`, buka **Validasi**, lalu **Setujui**.
3. Masuk kembali sebagai mahasiswa. Tugas tampil di **Kalender** dan notifikasi muncul di ikon lonceng.

## 4. Perintah Docker sehari-hari

Jalankan dari folder utama proyek.

| Tujuan | Perintah |
|---|---|
| Menyalakan | `docker compose up -d` |
| Mematikan, data tetap ada | `docker compose down` |
| Melihat status | `docker compose ps` |
| Melihat log backend | `docker compose logs -f backend` |
| Restart backend | `docker compose restart backend` |
| Build ulang setelah `package.json` atau `Dockerfile` berubah | `docker compose up -d --build` |
| Membuka PostgreSQL | `docker compose exec db psql -U plato -d plato` |
| **Menghapus database dan mengisi ulang** | `docker compose down -v` lalu `docker compose up -d` |

`docker compose down -v` menghapus seluruh data lokal, termasuk data yang kamu buat sendiri.

Untuk memakai aplikasi database seperti DBeaver atau pgAdmin:

| Host | Port | Database | User | Password |
|---|---|---|---|---|
| `localhost` | `5433` | `plato` | `plato` | `plato_dev_password` |

## 5. Mengembangkan backend

Folder `backend/src`, `backend/migrations`, dan `backend/tests` terhubung langsung ke container. Saat file `.ts` disimpan, backend memuat ulang sendiri. Di Windows perubahan terkadang tidak terdeteksi. Bila itu terjadi, jalankan `docker compose restart backend`.

**Menambah dependensi:**

```bash
docker run --rm -v "$PWD/backend":/app -w /app oven/bun:1.3-alpine bun add <nama-paket>
docker compose up -d --build
```

Hapus folder `backend/node_modules` bila terbentuk, lalu commit `package.json` dan `bun.lock`.

**Mengubah struktur database:**

1. Buat file baru dengan nomor berikutnya, misalnya `backend/migrations/005_tambah_kolom.sql`.
2. Jalankan `docker compose restart backend`. Migrasi dijalankan otomatis saat backend menyala.
3. Commit file tersebut. Anggota lain mendapatkannya setelah `git pull` dan restart backend.

Jangan mengubah file migrasi yang sudah di-merge. Setiap file hanya dijalankan satu kali, sehingga perubahannya tidak akan sampai ke komputer anggota lain. Bila dua anggota memakai nomor yang sama, ganti nomor milik salah satunya sebelum merge.

## 6. Mengembangkan aplikasi desktop

Lokasi kode ada di `desktop/src/main`:

- `java/id/plato/<fitur>`: Model dan Controller.
- `resources/id/plato/views/<fitur>`: View berformat FXML.
- `resources/id/plato/css/theme.css`: gaya tampilan bersama.

**IntelliJ IDEA:** pilih *Open*, buka folder `desktop`, tunggu Maven selesai, lalu jalankan class `id.plato.core.Launcher`. Pakai `Launcher`, bukan `PlatoApp`, agar tidak muncul galat *JavaFX runtime components are missing*.

**Menambah halaman baru:**

1. Buat FXML di `views/<fitur>/` dan controller yang mengimplementasikan `Page`.
2. Tambahkan entri ke enum `Route`.
3. Tambahkan entri itu ke `getMenu()` pada peran yang boleh membukanya (`Student`, `Admin`, atau `SuperAdmin`).

Panggilan ke backend harus memakai `Async.run(...)` supaya tampilan tidak membeku.

## 7. Pengujian

```bash
# Backend: backend dan database harus sedang berjalan
docker compose exec backend bun test

# Desktop: dari folder desktop
./mvnw test            # Windows: .\mvnw.cmd test
```

Pengujian backend menambahkan data ke database lokal. Bersihkan dengan `docker compose down -v` lalu `docker compose up -d`.

## 8. Pembagian tugas dan folder

Kode dipisah per fitur agar setiap anggota mengerjakan folder yang berbeda dan konflik saat merge berkurang.

| Anggota | Komponen | Desktop (`id.plato.*`) | Backend (`src/modules/*`) | Migrasi |
|---|---|---|---|---|
| Nur Fattah Hamdani | User Management | `user` | `users` | `001_users.sql` |
| Annisa Sofia Albana | Class Management | `classroom` | `classes` | `002_classes.sql` |
| Talitha Ineztasia | Task and Milestone Management | `task` | `tasks` | `003_tasks.sql` |
| Nashwa Aqeela Irtisa Rudin | Information and Monitoring | `monitoring` | `monitoring` | `004_monitoring.sql` |

Folder `id.plato.core`, `backend/src/core`, dan `theme.css` dipakai bersama. Beri tahu anggota lain sebelum mengubahnya.

Sepuluh class pada class diagram berada di:

| Class | File |
|---|---|
| `User` (abstract), `Student`, `Admin`, `SuperAdmin` | `id/plato/user/` |
| `ClassRoom`, `Membership` | `id/plato/classroom/` |
| `Task`, `Milestone` | `id/plato/task/` |
| `Notification`, `Dashboard` | `id/plato/monitoring/` |

Struktur proyek:

```
plato/
├── docker-compose.yml
├── .env.example
├── backend/
│   ├── Dockerfile
│   ├── migrations/          # struktur database, dijalankan berurutan
│   ├── src/
│   │   ├── core/            # database, router, autentikasi
│   │   ├── modules/         # users, classes, tasks, monitoring
│   │   └── seed.ts          # data contoh
│   └── tests/
└── desktop/
    ├── mvnw, mvnw.cmd       # Maven tanpa perlu dipasang
    ├── pom.xml
    └── src/
        ├── main/java/id/plato/    # core, user, classroom, task, monitoring
        ├── main/resources/id/plato/  # views, css, fonts
        └── test/java/id/plato/
```

## 9. Alur kerja Git

Setiap anggota bekerja di branch sendiri:

| Anggota | Branch |
|---|---|
| Nur Fattah Hamdani | `feature/user-management` |
| Annisa Sofia Albana | `feature/class-management` |
| Talitha Ineztasia | `feature/task-milestone` |
| Nashwa Aqeela Irtisa Rudin | `feature/monitoring` |

```bash
# Pertama kali
git checkout main
git pull
git checkout -b feature/user-management

# Sebelum mulai bekerja: ambil perubahan terbaru
git fetch origin
git merge origin/main
docker compose up -d --build     # memuat migrasi dan dependensi baru

# Menyimpan pekerjaan
git add .
git commit -m "feat(user): tambah validasi email"
git push -u origin feature/user-management
```

Setelah itu, buat Pull Request ke `main`. Jalankan pengujian sebelum merge. File `.env` tidak ikut ter-commit.

## 10. Daftar API

Alamat dasar: `http://localhost:8080/api/v1`. Selain `/health` dan `/auth/login`, setiap permintaan memerlukan header `Authorization: Bearer <token>`. Kesalahan dikembalikan sebagai `{"error": "pesan"}`.

**User Management**

| Metode | Endpoint | Akses | Fungsi |
|---|---|---|---|
| POST | `/auth/login` | Publik | Masuk, mengembalikan token dan data pengguna |
| POST | `/auth/logout` | Semua | Membatalkan token |
| POST | `/auth/verify` | Semua | Memeriksa password akun sendiri |
| GET, PUT | `/users/me` | Semua | Melihat atau mengubah profil |
| PUT | `/users/me/password` | Semua | Mengganti password |
| GET, POST | `/users` | SuperAdmin | Daftar atau tambah pengguna |
| PUT, DELETE | `/users/:id` | SuperAdmin | Ubah atau hapus pengguna |
| GET, POST | `/cohorts` | SuperAdmin | Daftar atau tambah angkatan (tambah: GLOBAL) |
| PUT, DELETE | `/cohorts/:id` | SuperAdmin | Ubah atau hapus angkatan (hapus: GLOBAL) |

**Class Management**

| Metode | Endpoint | Akses | Fungsi |
|---|---|---|---|
| GET | `/classes` | Semua | Kelas sesuai peran |
| GET | `/classes/available` | Mahasiswa | Kelas yang dapat diikuti |
| POST | `/classes` | SuperAdmin | Tambah kelas |
| GET | `/classes/:id` | Anggota, pengelola | Detail kelas |
| PUT | `/classes/:id` | Admin, SuperAdmin | Ubah kelas |
| DELETE | `/classes/:id` | SuperAdmin | Hapus kelas |
| POST | `/classes/:id/join` | Mahasiswa | Bergabung ke kelas |
| GET | `/classes/:id/members` | Anggota, pengelola | Daftar anggota |
| POST | `/classes/:id/members` | Admin, SuperAdmin | Tambah anggota lewat email |
| PATCH | `/classes/:id/members/:membershipId` | Admin, SuperAdmin | Ubah status keanggotaan |

**Task and Milestone Management**

| Metode | Endpoint | Akses | Fungsi |
|---|---|---|---|
| GET | `/tasks?classId=&status=&from=&to=&q=` | Semua | Daftar tugas dengan filter |
| POST | `/tasks` | Mahasiswa, Admin | Mahasiswa mengajukan, Admin langsung memublikasikan |
| GET | `/tasks/:id` | Sesuai akses | Detail tugas |
| PUT, DELETE | `/tasks/:id` | Admin, pengaju | Ubah atau hapus tugas |
| POST | `/tasks/:id/approve` | Admin | Menyetujui pengajuan |
| POST | `/tasks/:id/reject` | Admin | Menolak dengan alasan |
| PUT | `/tasks/:id/progress` | Mahasiswa | Progres manual, tanpa milestone |
| POST | `/tasks/:id/milestones` | Admin | Tambah milestone |
| PUT, DELETE | `/milestones/:id` | Admin | Ubah atau hapus milestone |
| PUT | `/milestones/:id/progress` | Mahasiswa | Progres milestone |

**Information and Monitoring**

| Metode | Endpoint | Akses | Fungsi |
|---|---|---|---|
| GET | `/notifications` | Semua | Daftar notifikasi |
| GET | `/notifications/unread-count` | Semua | Jumlah belum dibaca |
| PATCH | `/notifications/:id/read` | Semua | Tandai dibaca |
| POST | `/notifications/read-all` | Semua | Tandai semua dibaca |
| POST | `/classes/:id/notifications` | Admin | Kirim pengingat ke kelas |
| GET | `/dashboard/summary` | Semua | Ringkasan sesuai peran |

Progres disimpan per mahasiswa, karena satu tugas dipublikasikan ke seluruh kelas. Untuk Admin dan SuperAdmin, nilai `progress` adalah rata-rata seluruh anggota aktif.

## 11. Konfigurasi

Nilai berikut diatur di file `.env`. Jalankan `docker compose up -d` setelah mengubahnya.

| Nama | Bawaan | Fungsi |
|---|---|---|
| `API_PORT` | `8080` | Port backend di komputermu |
| `DB_PORT` | `5433` | Port database di komputermu |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | lihat `.env.example` | Kredensial database, dipakai saat database pertama dibuat |
| `JWT_SECRET` | lihat `.env.example` | Kunci penanda token login |
| `SEED_DEMO_DATA` | `true` | Mengisi data contoh saat database kosong |

Bila `API_PORT` diubah, beri tahu aplikasi desktop lewat variabel lingkungan `PLATO_API_URL`:

```bash
# macOS / Linux / Git Bash
PLATO_API_URL=http://localhost:9090/api/v1 ./mvnw javafx:run

# Windows PowerShell
$env:PLATO_API_URL="http://localhost:9090/api/v1"; .\mvnw.cmd javafx:run
```

## 12. Mengatasi masalah

| Gejala | Penyebab | Solusi |
|---|---|---|
| `Cannot connect to the Docker daemon` atau `failed to connect to the docker API` | Docker belum berjalan | Buka Docker Desktop dan tunggu sampai *running* |
| `port is already allocated` | Port 8080 atau 5433 terpakai | Ubah `API_PORT` atau `DB_PORT` di `.env`, lalu lihat [Konfigurasi](#11-konfigurasi) |
| Aplikasi menampilkan "Tidak dapat terhubung ke server" | Backend mati atau belum siap | Jalankan `docker compose ps`, lalu `docker compose logs backend` |
| Backend terus restart | Biasanya migrasi gagal | Baca `docker compose logs backend`, perbaiki SQL, atau reset dengan `docker compose down -v` |
| Tabel atau kolom baru tidak ada setelah `git pull` | Backend belum menjalankan migrasi baru | `docker compose up -d --build` |
| Gagal login dengan akun contoh | Database lama atau `SEED_DEMO_DATA=false` | `docker compose down -v` lalu `docker compose up -d` |
| Password database berubah tetapi backend gagal terhubung | Password hanya berlaku saat database pertama dibuat | `docker compose down -v` lalu `docker compose up -d` |
| `JAVA_HOME not found` atau `java: command not found` | JDK belum terpasang atau terminal lama | Pasang JDK 21 lalu buka terminal baru |
| `invalid target release: 21` | Versi JDK lebih rendah dari 21 | Atur `JAVA_HOME` ke JDK 21 |
| `JavaFX runtime components are missing` | IDE menjalankan `PlatoApp` | Jalankan `id.plato.core.Launcher` atau `./mvnw javafx:run` |
| `permission denied: ./mvnw` | Izin eksekusi hilang | `chmod +x mvnw` |
| `bad interpreter` saat menjalankan `./mvnw` | Akhir baris Windows | `git add --renormalize .` lalu `git checkout -- desktop/mvnw` |
| Sesi tiba-tiba berakhir | Token kedaluwarsa setelah 12 jam, atau akun yang sama logout di tempat lain | Masuk kembali |
| Perubahan kode backend tidak termuat di Windows | Pemantauan file tidak sampai ke container | `docker compose restart backend` |
