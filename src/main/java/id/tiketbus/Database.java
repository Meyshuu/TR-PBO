package id.tiketbus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class Database {
    private static final String URL = "jdbc:sqlite:bus_ticket.db";

    record Trip(int id, String busName, String origin, String destination, String departure,
                long price, int capacity, int availableSeats) {
        @Override
        public String toString() {
            return busName + " | " + origin + " - " + destination + " | " + departure
                    + " | Rp " + String.format("%,d", price)
                    + " | Kursi tersedia: " + availableSeats + "/" + capacity;
        }
    }

    record Member(int id, String name, String phone) { }

    record Booking(int id, String memberName, String memberPhone, String busName, String route,
                   String departure, int seat, long price, String status, String createdAt) { }

    Database() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver SQLite tidak ditemukan. Jalankan aplikasi melalui Maven.", e);
        }
        initialize();
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL UNIQUE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        bus_name TEXT NOT NULL,
                        origin TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        departure TEXT NOT NULL,
                        price INTEGER NOT NULL CHECK (price > 0),
                        capacity INTEGER NOT NULL CHECK (capacity > 0)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        member_id INTEGER NOT NULL REFERENCES members(id),
                        trip_id INTEGER NOT NULL REFERENCES trips(id),
                        seat INTEGER NOT NULL,
                        status TEXT NOT NULL CHECK (status IN ('AKTIF', 'DIBATALKAN')),
                        created_at TEXT NOT NULL
                    )
                    """);
            // Kursi unik hanya selama status pesanan aktif, sehingga kursi batal bisa dipakai lagi.
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_bookings_active_seat
                    ON bookings(trip_id, seat) WHERE status='AKTIF'
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bookings_member ON bookings(member_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bookings_trip ON bookings(trip_id)");
            try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM trips")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    seedTrips(connection);
                }
            }
        }
    }

    private void seedTrips(Connection connection) throws SQLException {
        String sql = "INSERT INTO trips(bus_name, origin, destination, departure, price, capacity) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            addTrip(ps, "Bus Eksekutif", "Jakarta", "Bandung", LocalDateTime.now().plusDays(1).withHour(8).withMinute(0), 150000, 40);
            addTrip(ps, "Bus Pariwisata", "Jakarta", "Yogyakarta", LocalDateTime.now().plusDays(2).withHour(19).withMinute(30), 280000, 36);
            addTrip(ps, "Bus Ekonomi", "Bandung", "Jakarta", LocalDateTime.now().plusDays(1).withHour(13).withMinute(0), 120000, 40);
        }
    }

    private static void addTrip(PreparedStatement ps, String bus, String origin, String destination,
                                LocalDateTime departure, long price, int capacity) throws SQLException {
        ps.setString(1, bus);
        ps.setString(2, origin);
        ps.setString(3, destination);
        ps.setString(4, departure.toString().replace('T', ' '));
        ps.setLong(5, price);
        ps.setInt(6, capacity);
        ps.executeUpdate();
    }

    List<Trip> getTrips() throws SQLException {
        List<Trip> trips = new ArrayList<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT t.*, t.capacity - COUNT(b.id) AS available_seats
                     FROM trips t LEFT JOIN bookings b ON b.trip_id=t.id AND b.status='AKTIF'
                     GROUP BY t.id ORDER BY t.departure
                     """)) {
            while (rs.next()) {
                trips.add(readTrip(rs));
            }
        }
        return trips;
    }

    // Membuat atau mengubah perjalanan; kapasitas tidak boleh memotong kursi aktif yang sudah terpakai.
    void saveTrip(Trip trip) throws SQLException {
        String sql = trip.id() == 0
                ? "INSERT INTO trips(bus_name, origin, destination, departure, price, capacity) VALUES (?, ?, ?, ?, ?, ?)"
                : "UPDATE trips SET bus_name=?, origin=?, destination=?, departure=?, price=?, capacity=? WHERE id=?";
        try (Connection connection = connect()) {
            if (trip.id() != 0) {
                try (PreparedStatement count = connection.prepareStatement(
                        "SELECT COUNT(*) FROM bookings WHERE trip_id=? AND status='AKTIF' AND seat>?")) {
                    count.setInt(1, trip.id());
                    count.setInt(2, trip.capacity());
                    try (ResultSet rs = count.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException("Kapasitas tidak boleh lebih kecil dari kursi yang sudah dipesan.");
                        }
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindTrip(ps, trip);
            if (trip.id() != 0) {
                ps.setInt(7, trip.id());
            }
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Perjalanan tidak ditemukan.");
            }
            }
        }
    }

    void deleteTrip(int tripId) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM trips WHERE id=?")) {
            ps.setInt(1, tripId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Perjalanan tidak ditemukan.");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY constraint failed")) {
                throw new SQLException("Perjalanan memiliki riwayat pemesanan dan tidak dapat dihapus.", e);
            }
            throw e;
        }
    }

    private static void bindTrip(PreparedStatement ps, Trip trip) throws SQLException {
        ps.setString(1, trip.busName());
        ps.setString(2, trip.origin());
        ps.setString(3, trip.destination());
        ps.setString(4, trip.departure());
        ps.setLong(5, trip.price());
        ps.setInt(6, trip.capacity());
    }

    private static Trip readTrip(ResultSet rs) throws SQLException {
        return new Trip(rs.getInt("id"), rs.getString("bus_name"), rs.getString("origin"),
                rs.getString("destination"), rs.getString("departure"), rs.getLong("price"),
                rs.getInt("capacity"), rs.getInt("available_seats"));
    }

    Member findOrCreateMember(String name, String phone) throws SQLException {
        try (Connection connection = connect()) {
            try (PreparedStatement find = connection.prepareStatement("SELECT id FROM members WHERE phone=?")) {
                find.setString(1, phone);
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement update = connection.prepareStatement("UPDATE members SET name=? WHERE id=?")) {
                            update.setString(1, name);
                            update.setInt(2, rs.getInt("id"));
                            update.executeUpdate();
                        }
                        return new Member(rs.getInt("id"), name, phone);
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO members(name, phone) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, name);
                insert.setString(2, phone);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new Member(keys.getInt(1), name, phone);
                    }
                }
            }
        }
        throw new SQLException("Gagal menyimpan data member.");
    }

    // Update dipisahkan dari proses daftar/masuk agar perubahan nomor telepon eksplisit.
    void updateMember(Member member) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement ps = connection.prepareStatement("UPDATE members SET name=?, phone=? WHERE id=?")) {
            ps.setString(1, member.name());
            ps.setString(2, member.phone());
            ps.setInt(3, member.id());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Member tidak ditemukan.");
            }
        }
    }

    // Booking dijalankan sebagai transaksi agar kursi dan catatan pesanan tersimpan bersama.
    int book(int memberId, int tripId) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                Trip trip = getTrip(connection, tripId);
                int seat = findAvailableSeat(connection, tripId, trip.capacity(), 0);
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO bookings(member_id, trip_id, seat, status, created_at)
                        VALUES (?, ?, ?, 'AKTIF', ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, memberId);
                    ps.setInt(2, tripId);
                    ps.setInt(3, seat);
                    ps.setString(4, LocalDateTime.now().toString().replace('T', ' '));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            connection.commit();
                            return keys.getInt(1);
                        }
                    }
                }
                throw new SQLException("Gagal membuat pemesanan.");
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Jadwal dan nomor kursi berubah dalam satu transaksi; kegagalan membatalkan seluruh perubahan.
    void reschedule(int bookingId, int memberId, int newTripId) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                Trip trip = getTrip(connection, newTripId);
                int seat = findAvailableSeat(connection, newTripId, trip.capacity(), bookingId);
                try (PreparedStatement ps = connection.prepareStatement("""
                        UPDATE bookings SET trip_id=?, seat=?
                        WHERE id=? AND member_id=? AND status='AKTIF'
                        """)) {
                    ps.setInt(1, newTripId);
                    ps.setInt(2, seat);
                    ps.setInt(3, bookingId);
                    ps.setInt(4, memberId);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Pemesanan aktif tidak ditemukan.");
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private Trip getTrip(Connection connection, int tripId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT t.*, t.capacity - COUNT(b.id) AS available_seats
                FROM trips t LEFT JOIN bookings b ON b.trip_id=t.id AND b.status='AKTIF'
                WHERE t.id=? GROUP BY t.id
                """)) {
            ps.setInt(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readTrip(rs);
                }
            }
        }
        throw new SQLException("Perjalanan tidak ditemukan.");
    }

    // Mencari nomor kursi kosong terkecil dan menolak pemesanan jika kapasitas habis.
    private int findAvailableSeat(Connection connection, int tripId, int capacity, int exceptBookingId) throws SQLException {
        boolean[] occupied = new boolean[capacity + 1];
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT seat FROM bookings
                WHERE trip_id=? AND status='AKTIF' AND id<>?
                """)) {
            ps.setInt(1, tripId);
            ps.setInt(2, exceptBookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int seat = rs.getInt(1);
                    if (seat >= 1 && seat <= capacity) {
                        occupied[seat] = true;
                    }
                }
            }
        }
        for (int seat = 1; seat <= capacity; seat++) {
            if (!occupied[seat]) {
                return seat;
            }
        }
        throw new SQLException("Perjalanan sudah penuh.");
    }

    void cancel(int bookingId, int memberId) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement ps = connection.prepareStatement("""
                     UPDATE bookings SET status='DIBATALKAN'
                     WHERE id=? AND member_id=? AND status='AKTIF'
                     """)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, memberId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Pemesanan aktif tidak ditemukan.");
            }
        }
    }

    // Menerima ID member opsional: null berarti rekap semua member untuk tampilan admin.
    List<Booking> getBookings(Integer memberId) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String sql = """
                SELECT b.id, m.name, m.phone, t.bus_name, t.origin, t.destination, t.departure,
                       b.seat, t.price, b.status, b.created_at
                FROM bookings b
                JOIN members m ON m.id=b.member_id
                JOIN trips t ON t.id=b.trip_id
                """ + (memberId == null ? "" : " WHERE b.member_id=?") + " ORDER BY b.created_at DESC";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            if (memberId != null) {
                ps.setInt(1, memberId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(new Booking(rs.getInt("id"), rs.getString("name"), rs.getString("phone"),
                            rs.getString("bus_name"), rs.getString("origin") + " - " + rs.getString("destination"),
                            rs.getString("departure"), rs.getInt("seat"), rs.getLong("price"),
                            rs.getString("status"), rs.getString("created_at")));
                }
            }
        }
        return bookings;
    }
}
