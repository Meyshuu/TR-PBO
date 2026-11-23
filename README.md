# Aplikasi Reservasi Tiket Bus

Aplikasi desktop Java Swing untuk pengelolaan perjalanan bus dan pemesanan tiket. Data disimpan secara lokal pada database SQLite (`bus_ticket.db`) di folder tempat aplikasi dijalankan.

## Fitur

- **Member:** daftar/masuk dengan nomor telepon, ubah profil, lihat perjalanan, pesan tiket, melihat riwayat, mengubah jadwal, dan membatalkan pemesanan (tanpa refund).
- **Admin:** tambah, lihat, ubah, dan hapus perjalanan; mengatur waktu keberangkatan, harga, serta kapasitas; melihat rekap transaksi member.
- Kursi diberikan otomatis dan tidak dapat dipesan ganda untuk perjalanan yang sama.
- Aplikasi mengisi beberapa perjalanan contoh pada database baru.

## Menjalankan

Gunakan Java 17 atau lebih baru dan Maven:

```powershell
mvn compile exec:java
```

Tab admin ditujukan untuk demonstrasi/tugas lokal dan belum dilindungi login admin.

## Dokumentasi

- [Wiki aplikasi](docs/wiki.md)
- [Empat materi presentasi](docs/materi-presentasi.md)
