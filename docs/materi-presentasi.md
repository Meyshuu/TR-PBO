# Empat Materi Presentasi Aplikasi Tiket Bus

Materi ini dibagi untuk empat presenter. Setiap bagian menunjukkan **file dan nomor baris**, menjelaskan tugas fungsi, memberi ringkasan konsep kodenya, dan menyertakan contoh narasi. Nomor baris mengacu pada kode saat ini; buka file yang ditautkan lalu gunakan **Ctrl+G** untuk menuju ke baris tertentu.

## Orang 1 — Gambaran aplikasi dan GUI

**Fokus:** struktur aplikasi Swing dan cara event pengguna ditangani.

### Peta kode

| File dan baris | Fungsi/kode | Penjelasan |
|---|---|---|
| [`App.java`](../src/main/java/id/tiketbus/App.java#L28), baris 28–48 | Deklarasi kelas dan komponen | Menyimpan referensi database, frame, form member/admin, serta model tabel. `JTextField`, `JComboBox`, dan `JTable` adalah komponen input dan tampilan Swing. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L50), baris 50–65 | Konstruktor `App` | Mengatur jendela, membuat tab member/admin, memuat data awal, lalu menampilkan frame. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L67), baris 67–96 | `createMemberPanel()` | Menyusun form profil, pilihan perjalanan, tombol aksi, dan tabel pesanan member. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L99), baris 99–145 | `createAdminPanel()` | Menyusun form CRUD perjalanan, tabel perjalanan, tab rekap pesanan, dan listener untuk mengisi form dari baris terpilih. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L402), baris 402–416 | `button()`, `run()` | `button()` menghubungkan klik dengan aksi menggunakan method reference seperti `this::bookTicket`. `run()` menangkap error SQL/validasi dan menampilkannya sebagai dialog. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L437), baris 437–449 | `main()` | Menjalankan inisialisasi GUI melalui `SwingUtilities.invokeLater`, memilih tampilan sistem, lalu membuat database dan aplikasi. |

### Cara menjelaskan

`App` bertugas mengurus tampilan dan interaksi, sedangkan operasi penyimpanan diserahkan ke `Database`. Tombol tidak menulis SQL; tombol memanggil fungsi seperti `bookTicket()` atau `createTrip()`. Model tabel dibuat tidak dapat diedit langsung pada [`App.java`](../src/main/java/id/tiketbus/App.java#L422), sehingga perubahan harus lewat form dan tombol.

### Contoh narasi

> “Antarmuka dibuat dengan Java Swing. Konstruktor `App` membuat dua tab. `createMemberPanel` menyusun fitur member, sedangkan `createAdminPanel` menyusun fitur pengelolaan perjalanan. Tombol dikaitkan ke fungsi dengan method reference, contohnya `this::bookTicket`. Dengan begitu, kode UI menangani event dan kelas `Database` menangani penyimpanan.”

**Demo yang disarankan:** buka kedua tab, tunjukkan komponen tabel/form, lalu jelaskan baris tombol yang memanggil aksi.

## Orang 2 — Database, tabel, dan JDBC

**Fokus:** bagaimana data disimpan dan dibaca.

### Peta kode

| File dan baris | Fungsi/kode | Penjelasan |
|---|---|---|
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L13), baris 13–38 | `Database`, record, konstruktor | `Trip`, `Member`, dan `Booking` adalah bentuk data. Konstruktor memuat driver SQLite dan memanggil inisialisasi skema. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L40), baris 40–46 | `connect()` | Membuka koneksi SQLite dan mengaktifkan pemeriksaan foreign key untuk koneksi tersebut. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L48), baris 48–90 | `initialize()` | Membuat tabel `members`, `trips`, `bookings`, index, dan data contoh bila belum ada perjalanan. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L92), baris 92–110 | `seedTrips()`, `addTrip()` | Menambahkan tiga perjalanan awal menggunakan parameter `PreparedStatement`. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L113), baris 113–127 | `getTrips()` | Membaca perjalanan sekaligus menghitung kursi tersedia dari kapasitas dikurangi pesanan aktif. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L346), baris 346–371 | `getBookings()` | Menggabungkan data pesanan, member, dan perjalanan. ID member opsional: `null` berarti semua transaksi untuk rekap admin. |

### Skema singkat

- `members`: profil member; nomor telepon unik.
- `trips`: bus, rute, jadwal, harga, dan kapasitas.
- `bookings`: relasi member dan perjalanan, nomor kursi, status, dan waktu pesan.
- Index unik parsial di [`Database.java`](../src/main/java/id/tiketbus/Database.java#L78) mencegah kursi aktif terpesan ganda, tetapi mengizinkan kursi pesanan batal dipakai lagi.

### Konsep kode yang dijelaskan

- **JDBC:** Java mengakses SQLite melalui `Connection`, `PreparedStatement`, dan `ResultSet`.
- **Prepared statement:** tanda `?` di SQL diisi lewat `setString`, `setInt`, atau `setLong`.
- **Try-with-resources:** koneksi, statement, dan hasil query ditutup otomatis setelah blok selesai.
- **Record:** menyediakan tipe data ringkas untuk membawa hasil query antarbagian aplikasi.

### Contoh narasi

> “Database aplikasi adalah SQLite lokal bernama `bus_ticket.db`. Saat aplikasi mulai, `initialize()` memastikan tiga tabel sudah tersedia. Tabel `bookings` menghubungkan ID member dengan ID perjalanan. Query perjalanan juga menghitung kursi tersedia. `PreparedStatement` digunakan untuk mengisi parameter query, dan `try-with-resources` menutup koneksi serta hasil query dengan aman.”

**Demo yang disarankan:** buka `Database.java` pada `initialize()`, tunjukkan relasi tiga tabel dan index unik kursi aktif.

## Orang 3 — Fitur member dan alur pemesanan

**Fokus:** pendaftaran/masuk sederhana, pesan, ubah jadwal, dan pembatalan.

### Peta kode

| File dan baris | Fungsi/kode | Penjelasan |
|---|---|---|
| [`App.java`](../src/main/java/id/tiketbus/App.java#L147), baris 147–169 | `registerMember()`, `updateProfile()` | Memeriksa input, mencari/membuat member, dan menyimpan perubahan profil. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L189), baris 189–218 | `findOrCreateMember()` | Mencari member lewat nomor telepon; memperbarui nama jika ditemukan atau membuat profil baru jika belum ada. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L171), baris 171–196 | `bookTicket()`, `rescheduleTicket()` | Memeriksa member dan pilihan pengguna, memanggil database, lalu memuat ulang daftar perjalanan dan pesanan. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L232), baris 232–262 | `book()` | Memulai transaksi, memilih kursi yang tersedia, memasukkan pesanan aktif, lalu commit. Jika SQL gagal, rollback. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L264), baris 264–289 | `reschedule()` | Mencari kursi pada perjalanan baru dan mengubah perjalanan/kursi pesanan aktif dalam satu transaksi. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L290), baris 290–331 | `getTrip()`, `findAvailableSeat()` | Membaca kapasitas dan mencari nomor kursi kosong terkecil. Jika semua kursi penuh, fungsi melempar error. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L198), baris 198–214; [`Database.java`](../src/main/java/id/tiketbus/Database.java#L332), baris 332–345 | `cancelTicket()`, `cancel()` | Meminta konfirmasi, kemudian mengubah status pesanan aktif menjadi `DIBATALKAN`; pesanan tidak dihapus dari riwayat. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L362), baris 362–387 | `selectedMemberBooking()`, `selectedTrip()` | Memastikan baris pesanan/perjalanan dipilih dan pesanan masih aktif sebelum aksi dilanjutkan. |

### Alur pemesanan

1. Member mengisi nama dan telepon. Nomor telepon digunakan untuk menemukan profil.
2. Member memilih perjalanan pada combo box.
3. `bookTicket()` meminta `Database.book()` membuat pesanan.
4. `findAvailableSeat()` menetapkan kursi kosong paling kecil.
5. Setelah berhasil, tampilan perjalanan dan tabel riwayat dimuat ulang.
6. Pembatalan mengganti status, sehingga riwayat masih terlihat dan kursinya bisa dipesan kembali. Tidak ada refund.

### Contoh narasi

> “Saat tombol pesan ditekan, `bookTicket()` memeriksa bahwa member dan perjalanan sudah dipilih. Kemudian `Database.book()` membuka transaksi, mencari kursi kosong, dan menyimpan pesanan. Jika perjalanan penuh, pemesanan gagal. Pembatalan tidak menghapus data, tetapi mengubah status menjadi `DIBATALKAN`, sehingga transaksi tetap tercatat.”

**Demo yang disarankan:** daftar sebagai member, pesan tiket, lihat kursi tersedia berkurang, kemudian batalkan dan tunjukkan status serta ketersediaan kursi.

## Orang 4 — Fitur admin, validasi, dan batasan

**Fokus:** CRUD data perjalanan dan cara input divalidasi.

### Peta kode

| File dan baris | Fungsi/kode | Penjelasan |
|---|---|---|
| [`App.java`](../src/main/java/id/tiketbus/App.java#L216), baris 216–254 | `createTrip()`, `updateTrip()`, `deleteTrip()` | Memanggil operasi penyimpanan/perubahan/penghapusan dari form. Delete meminta konfirmasi terlebih dahulu. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L255), baris 255–278 | `readTripForm()` | Memeriksa kolom wajib, parsing waktu, memeriksa harga positif, lalu membentuk record `Trip`. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L129), baris 129–158 | `saveTrip()` | ID `0` berarti INSERT; ID lain berarti UPDATE. Kapasitas tidak boleh lebih kecil dari nomor kursi aktif yang sudah digunakan. |
| [`Database.java`](../src/main/java/id/tiketbus/Database.java#L159), baris 159–173 | `deleteTrip()` | Menghapus perjalanan. Foreign key menolak penghapusan jika masih ada riwayat pesanan, lalu error ditampilkan dengan pesan yang mudah dipahami. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L280), baris 280–328 | Fungsi `refresh...()` dan `bookingRow()` | Mengisi ulang tabel perjalanan dan pesanan setelah perubahan data. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L329), baris 329–361 | `fillTripForm()`, `clearTripForm()`, `selectedTripId()` | Mengisi form dari tabel, membersihkan form, dan memastikan admin memilih perjalanan sebelum mengubah/menghapus. |
| [`App.java`](../src/main/java/id/tiketbus/App.java#L394), baris 394–416 | `required()`, `button()`, `run()` | Memvalidasi kolom kosong, menghubungkan tombol dengan aksi, dan menampilkan error validasi/database. |

### Aturan validasi yang dapat didemokan

- Waktu keberangkatan harus berformat `yyyy-MM-dd HH:mm`.
- Harga harus berupa bilangan bulat lebih dari nol.
- Kapasitas harus positif; database memeriksa aturan saat perjalanan disimpan.
- Perjalanan yang sudah mempunyai pesanan tidak bisa dihapus.
- Admin memilih baris pada tabel sebelum menekan simpan perubahan atau hapus.
- Aplikasi ini belum mempunyai autentikasi admin dan bukan sistem pembayaran/refund sungguhan.

### Contoh narasi

> “Admin mengelola perjalanan lewat form CRUD. `readTripForm()` memeriksa dan mengonversi input sebelum data dikirim. `saveTrip()` memilih INSERT atau UPDATE berdasarkan ID perjalanan. `deleteTrip()` tidak mengizinkan penghapusan data yang masih direferensikan oleh riwayat pemesanan. Pesan kegagalan ditampilkan lewat dialog agar pengguna tahu penyebabnya.”

**Demo yang disarankan:** tambah perjalanan dengan waktu valid, coba masukkan harga bukan angka untuk menunjukkan validasi, lalu pilih perjalanan dan ubah nilainya.

## Penutup bersama

> “Aplikasi membagi tanggung jawab antara GUI Swing pada `App.java` dan akses SQLite pada `Database.java`. Fitur utamanya meliputi CRUD perjalanan, pemesanan dengan kursi otomatis, riwayat, penjadwalan ulang, dan pembatalan. Pengembangan berikutnya dapat menambahkan login admin, pembayaran, dan penyimpanan harga historis transaksi.”
