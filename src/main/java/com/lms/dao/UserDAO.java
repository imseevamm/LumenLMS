package com.lms.dao;

import com.lms.model.Role;
import com.lms.model.User;
import com.lms.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for the users table. No business logic lives here - only CRUD + mapping. */
public class UserDAO {

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = map(rs);
                    ensureStudentRollNumber(con, user);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user by email", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = map(rs);
                    ensureStudentRollNumber(con, user);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user by id", e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User user = map(rs);
                ensureStudentRollNumber(con, user);
                users.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load users", e);
        }
        return users;
    }

    public List<User> findByRole(Role role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = map(rs);
                    ensureStudentRollNumber(con, user);
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load users by role", e);
        }
        return users;
    }

    public List<User> search(String keyword, Role roleFilter) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE (full_name LIKE ? OR email LIKE ?)");
        if (roleFilter != null) sql.append(" AND role = ?");
        sql.append(" ORDER BY created_at DESC");
        List<User> users = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            if (roleFilter != null) ps.setString(3, roleFilter.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = map(rs);
                    ensureStudentRollNumber(con, user);
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search users", e);
        }
        return users;
    }

    public User insert(User user) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, is_active, bio) VALUES (?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.setString(6, user.getBio());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getLong(1));
            }
            // Student roll numbers are deterministic from the DB-generated primary key,
            // so they are unique even when multiple accounts are created close together.
            if (user.getRole() == Role.STUDENT) {
                String roll = String.format("ST25%06d", user.getId());
                // Keep login/signup usable even if an older database has not yet run
                // migrate_student_roll_numbers.sql. Once the column exists, persist it.
                if (hasRollNumberColumn(con)) {
                    try (PreparedStatement rps = con.prepareStatement("UPDATE users SET roll_number=? WHERE user_id=?")) {
                        rps.setString(1, roll);
                        rps.setLong(2, user.getId());
                        rps.executeUpdate();
                    }
                    user.setRollNumber(roll);
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    public void update(User user) {
        String sql = "UPDATE users SET full_name=?, email=?, role=?, is_active=?, bio=? WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getRole().name());
            ps.setBoolean(4, user.isActive());
            ps.setString(5, user.getBio());
            ps.setLong(6, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public void updatePassword(long userId, String newHash) {
        String sql = "UPDATE users SET password_hash=? WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password", e);
        }
    }

    public void setActive(long userId, boolean active) {
        String sql = "UPDATE users SET is_active=? WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to toggle user active state", e);
        }
    }

    public void delete(long userId) {
        String sql = "DELETE FROM users WHERE user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public int countByRole(Role role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count users by role", e);
        }
        return 0;
    }

    /**
     * Ensures every student has a roll number when the roll_number column exists.
     * This also repairs students created before the roll-number migration was run.
     */
    private void ensureStudentRollNumber(Connection con, User user) throws SQLException {
        if (user.getRole() != Role.STUDENT || user.getRollNumber() != null && !user.getRollNumber().isBlank()) {
            return;
        }
        if (!hasRollNumberColumn(con)) return;

        String roll = String.format("ST25%06d", user.getId());
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET roll_number=? WHERE user_id=? AND (roll_number IS NULL OR roll_number='')")) {
            ps.setString(1, roll);
            ps.setLong(2, user.getId());
            ps.executeUpdate();
        }
        user.setRollNumber(roll);
    }

    private boolean hasRollNumberColumn(Connection con) {
        try (ResultSet columns = con.getMetaData().getColumns(null, null, "users", "roll_number")) {
            return columns.next();
        } catch (SQLException ignored) {
            return false;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("is_active"));
        u.setAvatarPath(rs.getString("avatar_path"));
        u.setBio(rs.getString("bio"));
        // Older installations may not have the roll_number column yet.
        // Do not let that schema mismatch prevent login.
        try {
            u.setRollNumber(rs.getString("roll_number"));
        } catch (SQLException ignored) {
            u.setRollNumber(null);
        }
        Timestamp ts = rs.getTimestamp("created_at");
        u.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return u;
    }
}
