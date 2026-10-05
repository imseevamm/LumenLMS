package com.lms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Centralized JDBC connection factory.
 * Reads configuration from db.properties (defaults), db.local.properties (local secrets) and LMS_DB_* environment variables.
 * A single place for connection details avoids duplicating connection code across every DAO.
 */
public final class DBConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        String url = "jdbc:mysql://localhost:3306/lms_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "root";
        String pass = "root";
        java.util.logging.Logger log = java.util.logging.Logger.getLogger(DBConnection.class.getName());

        // Precedence (lowest -> highest):
        //   1. bundled db.properties on the classpath (safe defaults only, no real secrets)
        //   2. external db.local.properties (working dir, or the file named by -Dlms.db.config) - git-ignored
        //   3. environment variables LMS_DB_URL / LMS_DB_USER / LMS_DB_PASSWORD
        java.util.Properties props = new java.util.Properties();
        try (var in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            log.log(java.util.logging.Level.WARNING, "Could not read bundled db.properties; using defaults.", e);
        }
        java.nio.file.Path external = java.nio.file.Path.of(System.getProperty("lms.db.config", "db.local.properties"));
        if (java.nio.file.Files.isRegularFile(external)) {
            try (var in = java.nio.file.Files.newInputStream(external)) {
                props.load(in);
            } catch (Exception e) {
                log.log(java.util.logging.Level.WARNING, "Could not read " + external + "; ignoring it.", e);
            }
        }
        url = props.getProperty("db.url", url);
        user = props.getProperty("db.user", user);
        pass = props.getProperty("db.password", pass);

        String envUrl = System.getenv("LMS_DB_URL");
        String envUser = System.getenv("LMS_DB_USER");
        String envPass = System.getenv("LMS_DB_PASSWORD");
        if (envUrl != null && !envUrl.isBlank()) url = envUrl;
        if (envUser != null && !envUser.isBlank()) user = envUser;
        if (envPass != null) pass = envPass;

        URL = url;
        USER = user;
        PASSWORD = pass;
    }

    private DBConnection() {}

    /** Opens a fresh JDBC connection. Callers are responsible for closing it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on classpath.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Ensures the student roll-number schema exists and repairs missing student roll numbers. */
    public static void ensureStudentRollNumbers() throws SQLException {
        try (Connection con = getConnection()) {
            boolean hasColumn;
            try (ResultSet rs = con.getMetaData().getColumns(null, null, "users", "roll_number")) { hasColumn = rs.next(); }
            if (!hasColumn) {
                try (var st = con.createStatement()) { st.executeUpdate("ALTER TABLE users ADD COLUMN roll_number VARCHAR(20) NULL"); }
            }
            try (var ps = con.prepareStatement("UPDATE users SET roll_number=CONCAT('ST25', LPAD(user_id, 6, '0')) WHERE role='STUDENT' AND (roll_number IS NULL OR TRIM(roll_number)='')")) { ps.executeUpdate(); }
            boolean hasUniqueIndex=false;
            try (ResultSet rs=con.getMetaData().getIndexInfo(null,null,"users",false,false)) {
                while(rs.next()){ String idx=rs.getString("INDEX_NAME"), col=rs.getString("COLUMN_NAME"); if("uq_users_roll_number".equalsIgnoreCase(idx) && "roll_number".equalsIgnoreCase(col)){hasUniqueIndex=true;break;} }
            }
            if(!hasUniqueIndex){ try(var st=con.createStatement()){st.executeUpdate("ALTER TABLE users ADD UNIQUE KEY uq_users_roll_number (roll_number)");} }
        }
    }

    /** Bootstraps the built-in administrator account for first-time/local setup.
     *  This is create-only: it NEVER overwrites the password, role or active flag of an account that
     *  already exists, so an administrator who changes their password keeps it across restarts.
     *  - If ogadmin@gmail.com exists, nothing is changed.
     *  - If only the legacy admin@lms.com administrator exists, it is renamed once to the current email.
     *  - If no administrator exists at all, the default one is created.
     */
    public static void ensureDefaultAdminAccount() throws SQLException {
        final String email = "ogadmin@gmail.com";
        final String legacyEmail = "admin@lms.com";
        final String hash = "$2a$10$oTsCYNp8nVmzhRgSOCmn1uRc3LluwEbPChUBlcADEvv8TSxvd/AKC";

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                if (exists(con, "SELECT 1 FROM users WHERE email = ? LIMIT 1", email)) {
                    con.commit();
                    return;
                }
                if (exists(con, "SELECT 1 FROM users WHERE email = ? AND role='ADMIN' LIMIT 1", legacyEmail)) {
                    try (var ps = con.prepareStatement(
                            "UPDATE users SET email=?, password_hash=?, is_active=TRUE WHERE email=? AND role='ADMIN'")) {
                        ps.setString(1, email);
                        ps.setString(2, hash);
                        ps.setString(3, legacyEmail);
                        ps.executeUpdate();
                    }
                } else {
                    boolean anyAdmin;
                    try (var st = con.createStatement(); var rs = st.executeQuery("SELECT 1 FROM users WHERE role='ADMIN' LIMIT 1")) {
                        anyAdmin = rs.next();
                    }
                    if (!anyAdmin) {
                        try (var ps = con.prepareStatement(
                                "INSERT INTO users (full_name, email, password_hash, role, is_active, bio) VALUES (?, ?, ?, 'ADMIN', TRUE, ?)")) {
                            ps.setString(1, "System Administrator");
                            ps.setString(2, email);
                            ps.setString(3, hash);
                            ps.setString(4, "Platform administrator");
                            ps.executeUpdate();
                        }
                    }
                }
                con.commit();
            } catch (SQLException ex) {
                try { con.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } finally {
                try { con.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    private static boolean exists(Connection con, String sql, String param) throws SQLException {
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Quick connectivity check used at application startup. */
    public static boolean testConnection() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
