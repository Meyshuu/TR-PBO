package id.tiketbus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class App {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Database database;
    private final JFrame frame = new JFrame("Tiket Bus - Reservasi Perjalanan");

    private final JTextField memberName = new JTextField(16);
    private final JTextField memberPhone = new JTextField(12);
    private final JComboBox<Database.Trip> memberTrips = new JComboBox<>();
    private final DefaultTableModel memberBookingModel = model("ID", "Bus", "Rute", "Keberangkatan", "Kursi", "Harga (Rp)", "Status", "Dipesan");
    private final JTable memberBookings = new JTable(memberBookingModel);
    private Database.Member currentMember;

    private final JTextField busName = new JTextField(14);
    private final JTextField origin = new JTextField(10);
    private final JTextField destination = new JTextField(10);
    private final JTextField departure = new JTextField(16);
    private final JTextField price = new JTextField(10);
    private final JSpinner capacity = new JSpinner(new SpinnerNumberModel(40, 1, 1000, 1));
    private final DefaultTableModel tripModel = model("ID", "Bus", "Asal", "Tujuan", "Keberangkatan", "Harga (Rp)", "Kapasitas");
    private final JTable tripsTable = new JTable(tripModel);
    private final DefaultTableModel adminBookingModel = model("ID", "Member", "Telepon", "Bus", "Rute", "Keberangkatan", "Kursi", "Harga (Rp)", "Status", "Dipesan");

    // Menyiapkan dua tab dan memuat data awal sebelum jendela ditampilkan.
    private App(Database database) {
        this.database = database;
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Member - Pemesanan", createMemberPanel());
        tabs.addTab("Admin - Kelola Bus", createAdminPanel());
        frame.add(tabs);
        refreshMemberTrips();
        refreshTrips();
        refreshBookings();
        frame.setVisible(true);
    }

    // Menyusun form profil, pilihan perjalanan, dan tabel pesanan member.
    private JPanel createMemberPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel profile = new JPanel(new FlowLayout(FlowLayout.LEFT));
        profile.setBorder(BorderFactory.createTitledBorder("Profil member"));
        profile.add(new JLabel("Nama:"));
        profile.add(memberName);
        profile.add(new JLabel("No. telepon:"));
        profile.add(memberPhone);
        profile.add(button("Daftar / Masuk", this::registerMember));
        profile.add(button("Simpan Profil", this::updateProfile));

        JPanel bookingActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bookingActions.setBorder(BorderFactory.createTitledBorder("Pilih perjalanan"));
        bookingActions.add(memberTrips);
        bookingActions.add(button("Pesan Tiket", this::bookTicket));
        bookingActions.add(button("Ubah Jadwal", this::rescheduleTicket));
        bookingActions.add(button("Batalkan (tanpa refund)", this::cancelTicket));
        bookingActions.add(button("Muat Ulang", this::refreshMemberTrips));

        JPanel top = new JPanel(new GridLayout(2, 1, 4, 4));
        top.add(profile);
        top.add(bookingActions);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(memberBookings), BorderLayout.CENTER);
        panel.add(new JLabel("Pilih baris pemesanan untuk mengubah jadwal atau membatalkan. Pembatalan tidak mengembalikan dana."),
                BorderLayout.SOUTH);
        return panel;
    }

    // Menyusun form CRUD perjalanan dan tabel rekap yang digunakan admin.
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.setBorder(BorderFactory.createTitledBorder("Data rute dan perjalanan"));
        form.add(new JLabel("Nama bus:"));
        form.add(busName);
        form.add(new JLabel("Asal:"));
        form.add(origin);
        form.add(new JLabel("Tujuan:"));
        form.add(destination);
        form.add(new JLabel("Berangkat (yyyy-MM-dd HH:mm):"));
        form.add(departure);
        form.add(new JLabel("Harga:"));
        form.add(price);
        form.add(new JLabel("Kapasitas:"));
        form.add(capacity);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Tambah Perjalanan", this::createTrip));
        actions.add(button("Simpan Perubahan", this::updateTrip));
        actions.add(button("Hapus Perjalanan", this::deleteTrip));
        actions.add(button("Bersihkan Form", this::clearTripForm));
        actions.add(button("Muat Ulang", this::refreshTrips));

        JPanel header = new JPanel(new BorderLayout());
        header.add(form, BorderLayout.CENTER);
        header.add(actions, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane dataTabs = new JTabbedPane();
        dataTabs.addTab("Daftar Perjalanan", new JScrollPane(tripsTable));
        JTable bookingTable = new JTable(adminBookingModel);
        dataTabs.addTab("Rekap Pemesanan Member", new JScrollPane(bookingTable));
        panel.add(dataTabs, BorderLayout.CENTER);
        panel.add(new JLabel("Keterangan: kapasitas adalah jumlah kursi; perjalanan yang memiliki riwayat pemesanan tidak dapat dihapus."),
                BorderLayout.SOUTH);

        tripsTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                fillTripForm();
            }
        });
        return panel;
    }

    // Nomor telepon menjadi kunci untuk menemukan atau membuat profil member.
    private void registerMember() {
        run(() -> {
            String name = required(memberName.getText(), "Nama");
            String phone = required(memberPhone.getText(), "Nomor telepon");
            currentMember = database.findOrCreateMember(name, phone);
            memberName.setText(currentMember.name());
            memberPhone.setText(currentMember.phone());
            refreshMemberBookings();
            message("Profil member siap digunakan.");
        });
    }

    private void updateProfile() {
        run(() -> {
            requireMember();
            currentMember = new Database.Member(currentMember.id(), required(memberName.getText(), "Nama"),
                    required(memberPhone.getText(), "Nomor telepon"));
            database.updateMember(currentMember);
            refreshMemberBookings();
            message("Profil berhasil diperbarui.");
        });
    }

    // Membuat pesanan baru; data ketersediaan dan tabel diperbarui setelah berhasil.
    private void bookTicket() {
        run(() -> {
            requireMember();
            Database.Trip trip = selectedTrip();
            int id = database.book(currentMember.id(), trip.id());
            refreshMemberTrips();
            refreshMemberBookings();
            refreshBookings();
            message("Tiket berhasil dipesan. Nomor pemesanan: " + id);
        });
    }

    // Jadwal baru diambil dari pilihan perjalanan dan diterapkan ke pesanan aktif.
    private void rescheduleTicket() {
        run(() -> {
            requireMember();
            Database.Booking booking = selectedMemberBooking();
            Database.Trip trip = selectedTrip();
            database.reschedule(booking.id(), currentMember.id(), trip.id());
            refreshMemberTrips();
            refreshMemberBookings();
            refreshBookings();
            message("Jadwal pemesanan berhasil diubah.");
        });
    }

    // Konfirmasi ditampilkan sebelum status pesanan diubah menjadi dibatalkan.
    private void cancelTicket() {
        run(() -> {
            requireMember();
            Database.Booking booking = selectedMemberBooking();
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Batalkan pemesanan #" + booking.id() + "? Pembatalan tidak dapat dikembalikan dan tanpa refund.",
                    "Konfirmasi pembatalan", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                database.cancel(booking.id(), currentMember.id());
                refreshMemberTrips();
                refreshMemberBookings();
                refreshBookings();
                message("Pemesanan dibatalkan. Tidak ada refund.");
            }
        });
    }

    // ID nol menandakan data baru; Database menyimpannya sebagai perjalanan baru.
    private void createTrip() {
        run(() -> {
            database.saveTrip(readTripForm(0));
            refreshTrips();
            refreshMemberTrips();
            refreshBookings();
            clearTripForm();
            message("Perjalanan berhasil ditambahkan.");
        });
    }

    // Perjalanan yang sedang dipilih menentukan baris database yang diperbarui.
    private void updateTrip() {
        run(() -> {
            int id = selectedTripId();
            database.saveTrip(readTripForm(id));
            refreshTrips();
            refreshMemberTrips();
            refreshBookings();
            message("Perjalanan berhasil diperbarui.");
        });
    }

    private void deleteTrip() {
        run(() -> {
            int id = selectedTripId();
            int confirm = JOptionPane.showConfirmDialog(frame, "Hapus perjalanan terpilih?",
                    "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                database.deleteTrip(id);
                refreshTrips();
                refreshMemberTrips();
                clearTripForm();
                message("Perjalanan berhasil dihapus.");
            }
        });
    }

    // Validasi dan konversi nilai form dilakukan sebelum data dikirim ke database.
    private Database.Trip readTripForm(int id) {
        String bus = required(busName.getText(), "Nama bus");
        String from = required(origin.getText(), "Asal");
        String to = required(destination.getText(), "Tujuan");
        LocalDateTime time;
        try {
            time = LocalDateTime.parse(required(departure.getText(), "Waktu keberangkatan"), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format keberangkatan harus yyyy-MM-dd HH:mm.");
        }
        long amount;
        try {
            amount = Long.parseLong(required(price.getText(), "Harga"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Harga harus berupa bilangan bulat.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Harga harus lebih dari 0.");
        }
        return new Database.Trip(id, bus, from, to, time.toString().replace('T', ' '), amount,
                (Integer) capacity.getValue(), (Integer) capacity.getValue());
    }

    // Memuat ulang data database ke komponen GUI setelah ada perubahan.
    private void refreshMemberTrips() {
        run(() -> {
            List<Database.Trip> available = database.getTrips();
            memberTrips.removeAllItems();
            for (Database.Trip trip : available) {
                memberTrips.addItem(trip);
            }
        });
    }

    private void refreshTrips() {
        run(() -> {
            tripModel.setRowCount(0);
            for (Database.Trip trip : database.getTrips()) {
                tripModel.addRow(new Object[]{trip.id(), trip.busName(), trip.origin(), trip.destination(),
                        trip.departure(), trip.price(), trip.capacity()});
            }
        });
    }

    private void refreshMemberBookings() {
        run(() -> {
            memberBookingModel.setRowCount(0);
            if (currentMember != null) {
                for (Database.Booking booking : database.getBookings(currentMember.id())) {
                    memberBookingModel.addRow(bookingRow(booking, false));
                }
            }
        });
    }

    private void refreshBookings() {
        run(() -> {
            adminBookingModel.setRowCount(0);
            for (Database.Booking booking : database.getBookings(null)) {
                adminBookingModel.addRow(bookingRow(booking, true));
            }
        });
    }

    private Object[] bookingRow(Database.Booking booking, boolean includeMember) {
        if (includeMember) {
            return new Object[]{booking.id(), booking.memberName(), booking.memberPhone(), booking.busName(),
                    booking.route(), booking.departure(), booking.seat(), booking.price(), booking.status(), booking.createdAt()};
        }
        return new Object[]{booking.id(), booking.busName(), booking.route(), booking.departure(),
                booking.seat(), booking.price(), booking.status(), booking.createdAt()};
    }

    // Mengisi form dari baris terpilih agar admin dapat mengedit data perjalanan.
    private void fillTripForm() {
        int row = tripsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        busName.setText(tripModel.getValueAt(row, 1).toString());
        origin.setText(tripModel.getValueAt(row, 2).toString());
        destination.setText(tripModel.getValueAt(row, 3).toString());
        departure.setText(tripModel.getValueAt(row, 4).toString());
        price.setText(tripModel.getValueAt(row, 5).toString());
        capacity.setValue(tripModel.getValueAt(row, 6));
    }

    private void clearTripForm() {
        busName.setText("");
        origin.setText("");
        destination.setText("");
        departure.setText("");
        price.setText("");
        capacity.setValue(40);
        tripsTable.clearSelection();
    }

    // Menolak operasi yang membutuhkan pilihan jika tabel belum memiliki baris terpilih.
    private int selectedTripId() {
        int row = tripsTable.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException("Pilih perjalanan pada tabel terlebih dahulu.");
        }
        return ((Number) tripModel.getValueAt(row, 0)).intValue();
    }

    // Memastikan pesanan milik member ini masih aktif sebelum diubah atau dibatalkan.
    private Database.Booking selectedMemberBooking() throws SQLException {
        int row = memberBookings.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException("Pilih pemesanan pada tabel terlebih dahulu.");
        }
        int id = ((Number) memberBookingModel.getValueAt(row, 0)).intValue();
        for (Database.Booking booking : database.getBookings(currentMember.id())) {
            if (booking.id() == id) {
                if (!booking.status().equals("AKTIF")) {
                    throw new IllegalArgumentException("Pemesanan yang sudah dibatalkan tidak dapat diubah.");
                }
                return booking;
            }
        }
        throw new IllegalArgumentException("Pemesanan tidak ditemukan.");
    }

    private Database.Trip selectedTrip() {
        Database.Trip trip = (Database.Trip) memberTrips.getSelectedItem();
        if (trip == null) {
            throw new IllegalArgumentException("Belum ada perjalanan yang tersedia.");
        }
        return trip;
    }

    private void requireMember() {
        if (currentMember == null) {
            throw new IllegalArgumentException("Daftar / masuk member terlebih dahulu.");
        }
    }

    private static String required(String value, String label) {
        String result = value.trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        return result;
    }

    private JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    // Satu penanganan error untuk operasi GUI yang dapat gagal di database atau validasi.
    private void run(SqlAction action) {
        try {
            action.run();
        } catch (SQLException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Tidak dapat memproses",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(frame, text, "Tiket Bus", JOptionPane.INFORMATION_MESSAGE);
    }

    // Model tabel tidak dapat diedit langsung; perubahan harus melalui form dan tombol.
    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    public static void main(String[] args) {
        // Pembuatan dan perubahan komponen Swing dijalankan pada thread khusus GUI.
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new App(new Database());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Gagal menjalankan aplikasi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
