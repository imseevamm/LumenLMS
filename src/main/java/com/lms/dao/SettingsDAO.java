package com.lms.dao;

import com.lms.util.DBConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDAO {

    public Map<String, String> findAll() {
        Map<String, String> map = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT setting_key, setting_value FROM system_settings")) {
            while (rs.next()) map.put(rs.getString(1), rs.getString(2));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load settings", e);
        }
        return map;
    }

    public void set(String key, String value) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value) VALUES (?,?) " +
                "ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save setting", e);
        }
    }
}
