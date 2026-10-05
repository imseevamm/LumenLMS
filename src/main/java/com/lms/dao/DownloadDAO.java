package com.lms.dao;

import com.lms.model.DownloadRecord;
import com.lms.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DownloadDAO {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS download_history (" +
            "download_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "user_id BIGINT NOT NULL, " +
            "title VARCHAR(200) NOT NULL, " +
            "file_name VARCHAR(255) NOT NULL, " +
            "category VARCHAR(80) NOT NULL DEFAULT 'File', " +
            "downloaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "INDEX idx_download_user_time (user_id, downloaded_at), " +
            "CONSTRAINT fk_download_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
            ") ENGINE=InnoDB";

    private void ensureTable(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) { st.executeUpdate(CREATE_TABLE); }
    }

    public void insert(DownloadRecord record) {
        try (Connection con = DBConnection.getConnection()) {
            ensureTable(con);
            String sql = "INSERT INTO download_history (user_id,title,file_name,category) VALUES (?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, record.getUserId());
                ps.setString(2, record.getTitle());
                ps.setString(3, record.getFileName());
                ps.setString(4, record.getCategory());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) record.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save download history", e);
        }
    }

    public List<DownloadRecord> findByUser(long userId) {
        List<DownloadRecord> records = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            ensureTable(con);
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM download_history WHERE user_id=? ORDER BY downloaded_at DESC")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DownloadRecord r = new DownloadRecord();
                        r.setId(rs.getLong("download_id"));
                        r.setUserId(rs.getLong("user_id"));
                        r.setTitle(rs.getString("title"));
                        r.setFileName(rs.getString("file_name"));
                        r.setCategory(rs.getString("category"));
                        Timestamp ts = rs.getTimestamp("downloaded_at");
                        r.setDownloadedAt(ts == null ? null : ts.toLocalDateTime());
                        records.add(r);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load download history", e);
        }
        return records;
    }

    public void clearByUser(long userId) {
        try (Connection con = DBConnection.getConnection()) {
            ensureTable(con);
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM download_history WHERE user_id=?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear download history", e);
        }
    }
}
