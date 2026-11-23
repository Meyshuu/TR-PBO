# Wiki Aplikasi Reservasi Tiket Bus

## 1. Gambaran umum

Aplikasi ini adalah program desktop untuk mengelola jadwal perjalanan bus dan pemesanan tiket. Antarmukanya dibuat dengan **Java Swing**, sedangkan data disimpan secara lokal menggunakan **SQLite** melalui JDBC.

Ada dua bagian pada jendela utama:

- **Member - Pemesanan:** membuat atau membuka profil berdasarkan nomor telepon, melihat perjalanan dan ketersediaan kursi, memesan tiket, melihat daftar pesanan, mengubah jadwal, dan membatalkan pesanan.
- **Admin - Kelola Bus:** mengelola data perjalanan dan melihat rekap pesanan member.

Ini adalah aplikasi demonstrasi lokal. Tab admin belum meminta login admin, dan proses pembayaran/refund tidak terhubung ke layanan pembayaran.

## 2. Teknologi dan cara menjalankan

- Java 17 atau lebih baru
- Maven
- Swing untuk GUI
- SQLite dan driver `sqlite-jdbc`

Jalankan dari folder proyek:

```powershell
mvn compile exec:java
```

File database bernama `bus_ticket.db` dibuat di **working directory** saat aplikasi dijalankan. Database baru otomatis diisi tiga perjalanan contoh. File database yang sudah ada tidak direset saat aplikasi dibuka.

## 3. Struktur proyek

```text
.
├── pom.xml
├── README.md
├── docs/
│   ├── wiki.md
│   └── materi-presentasi.md
└── src/main/java/id/tiketbus/
    ├── App.java
    └── Database.java
```

### [`App.java`](../src/main/java/id/tiketbus/App.java)

Mengatur jendela, komponen Swing, event tombol, validasi input dari form, dan pembaruan tampilan setelah data berubah. Kelas ini memanggil `Database` untuk membaca atau menyimpan data, bukan menulis SQL sendiri.

Metode alur utama:

| Metode | Tanggung jawab |
|---|---|
| `main` | Menjadwalkan pembuatan GUI pada Swing Event Dispatch Thread, memilih tampilan sistem, lalu membuat database dan aplikasi. |
| `createMemberPanel` | Membuat tab profil dan pemesanan member. |
| `createAdminPanel` | Membuat form perjalanan dan tabel rekap admin. |
| `registerMember` / `updateProfile` | Membuat atau memperbarui profil member. |
| `bookTicket` / `rescheduleTicket` / `cancelTicket` | Menangani aksi pemesanan member dan menyegarkan data tampilan. |
| `createTrip` / `updateTrip` / `deleteTrip` | Menangani CRUD perjalanan dari tab admin. |
| `readTripForm` | Memvalidasi form admin dan mengubah waktu masukan menjadi data perjalanan. |
| `refreshMemberTrips`, `refreshTrips`, `refreshMemberBookings`, `refreshBookings` | Memuat ulang data dari database ke komponen GUI. |
| `run` | Menampilkan pesan kesalahan SQL atau validasi sebagai dialog, bukan membiarkan error hilang. |

Tabel memakai `DefaultTableModel` yang dibuat tidak dapat diedit langsung. Perubahan data dilakukan melalui tombol dan form.

### [`Database.java`](../src/main/java/id/tiketbus/Database.java)

Mengelola koneksi SQLite, pembuatan skema, data contoh, query, dan aturan pemesanan. Kelas ini memakai `PreparedStatement` untuk memasukkan parameter query serta `try-with-resources` untuk menutup koneksi dan statement.

Record yang menjadi bentuk data:

- `Trip`: data bus, rute, jadwal, harga, kapasitas, dan kursi tersedia.
- `Member`: ID, nama, dan nomor telepon.
- `Booking`: data pesanan dan informasi perjalanan/member yang ditampilkan pada tabel.

### [`pom.xml`](../pom.xml)

Mendefinisikan koordinat Maven, versi Java 17, dependensi SQLite JDBC, dan kelas utama `id.tiketbus.App` untuk plugin `exec`.

## 4. Skema database

Database membuat tiga tabel:

### `members`

| Kolom | Keterangan |
|---|---|
| `id` | Primary key otomatis. |
| `name` | Nama member, wajib diisi. |
| `phone` | Nomor telepon, wajib diisi dan unik. Digunakan untuk menemukan profil yang sudah ada. |

### `trips`

| Kolom | Keterangan |
|---|---|
| `id` | Primary key otomatis. |
| `bus_name` | Nama atau jenis bus. |
| `origin`, `destination` | Kota asal dan tujuan. |
| `departure` | Waktu keberangkatan dalam teks `yyyy-MM-dd HH:mm`. |
| `price` | Harga dalam rupiah sebagai bilangan bulat positif. |
| `capacity` | Jumlah kursi, harus lebih dari nol. |

### `bookings`

| Kolom | Keterangan |
|---|---|
| `id` | Primary key otomatis. |
| `member_id` | Relasi ke member. |
| `trip_id` | Relasi ke perjalanan. |
| `seat` | Nomor kursi yang ditetapkan aplikasi. |
| `status` | `AKTIF` atau `DIBATALKAN`. |
| `created_at` | Waktu pembuatan pesanan. |

Foreign key menjaga referensi ke member dan perjalanan tetap valid. Index unik parsial `idx_bookings_active_seat` mencegah satu kursi aktif terpesan dua kali pada perjalanan yang sama. Kursi dari pesanan yang dibatalkan dapat digunakan kembali.

## 5. Alur kerja member

1. Member mengisi nama dan nomor telepon lalu memilih **Daftar / Masuk**.
2. `findOrCreateMember` mencari nomor telepon. Jika sudah ada, profil yang sama digunakan dan namanya diperbarui; jika belum, profil baru dibuat.
3. Daftar perjalanan menampilkan bus, rute, waktu, harga, dan kursi yang masih tersedia.
4. Member memilih perjalanan dan menekan **Pesan Tiket**.
5. Database memilih nomor kursi kosong terkecil, kemudian memasukkan pesanan berstatus `AKTIF`.
6. Member memilih baris pesanan dan perjalanan lain untuk mengubah jadwal, atau menekan **Batalkan (tanpa refund)** untuk membatalkan.
7. Pesanan yang dibatalkan tetap disimpan untuk riwayat, namun tidak lagi mengurangi ketersediaan kursi.

Pemesanan dan perubahan jadwal dijalankan di dalam transaksi database. Jika operasi gagal, transaksi di-rollback agar perubahan parsial tidak tertinggal.

## 6. Alur kerja admin

- **Tambah:** isi nama bus, asal, tujuan, waktu, harga, dan kapasitas lalu pilih **Tambah Perjalanan**.
- **Lihat:** tabel menampilkan semua perjalanan; tab rekap menampilkan pesanan beserta data member.
- **Ubah:** pilih baris perjalanan, edit form yang terisi otomatis, lalu pilih **Simpan Perubahan**.
- **Hapus:** pilih perjalanan dan konfirmasi penghapusan. Perjalanan yang memiliki riwayat pesanan tidak bisa dihapus karena foreign key.

Waktu harus memakai format `yyyy-MM-dd HH:mm`, harga harus bilangan bulat positif, dan kapasitas harus bilangan bulat positif. Kapasitas tidak dapat diturunkan hingga lebih kecil dari nomor kursi aktif yang sudah dipesan.

## 7. Validasi dan aturan penting

- Profil harus dipilih sebelum member memesan, mengubah, atau membatalkan pesanan.
- Nama, nomor telepon, rute, dan waktu tidak boleh kosong.
- Pemesanan pada perjalanan penuh ditolak.
- Nomor kursi aktif unik untuk kombinasi perjalanan dan kursi.
- Pesanan yang tidak aktif tidak dapat dijadwalkan ulang atau dibatalkan kembali.
- Harga yang ditampilkan di riwayat dibaca dari data perjalanan saat ini; harga transaksi historis tidak disimpan sebagai salinan terpisah pada `bookings`.
- Identitas member hanya ditentukan melalui nomor telepon; aplikasi belum menyediakan kata sandi atau autentikasi.
- Pesanan batal tidak menghapus riwayat transaksi dan tidak menghasilkan refund.

## 8. Peta kode

| Bagian | Lokasi |
|---|---|
| Inisialisasi aplikasi dan pembuatan tab | [`App.java`](../src/main/java/id/tiketbus/App.java): `main`, konstruktor `App`, `createMemberPanel`, `createAdminPanel` |
| Aksi pemesanan member | [`App.java`](../src/main/java/id/tiketbus/App.java): `registerMember`, `bookTicket`, `rescheduleTicket`, `cancelTicket` |
| Aksi CRUD perjalanan | [`App.java`](../src/main/java/id/tiketbus/App.java): `createTrip`, `updateTrip`, `deleteTrip`, `readTripForm` |
| Refresh tabel GUI | Metode `refresh...` pada [`App.java`](../src/main/java/id/tiketbus/App.java) |
| Pembuatan skema dan data awal | [`Database.java`](../src/main/java/id/tiketbus/Database.java): `initialize`, `seedTrips` |
| Data perjalanan dan member | [`Database.java`](../src/main/java/id/tiketbus/Database.java): `getTrips`, `saveTrip`, `findOrCreateMember`, `updateMember` |
| Pemesanan, penjadwalan ulang, pembatalan | [`Database.java`](../src/main/java/id/tiketbus/Database.java): `book`, `reschedule`, `cancel` |
| Rekap transaksi | [`Database.java`](../src/main/java/id/tiketbus/Database.java): `getBookings` |

## 9. Pengembangan lanjutan

Jika aplikasi akan digunakan di luar demonstrasi, pertimbangkan menambahkan autentikasi dan otorisasi admin, validasi nomor telepon, penyimpanan harga pada saat transaksi, alur pembayaran/refund yang nyata, serta pengujian otomatis yang dijalankan lewat Maven.
