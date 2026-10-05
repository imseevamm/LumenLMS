package com.lms.dao;

import com.lms.model.Course;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseDAO {

    private static final String BASE_SELECT =
            "SELECT c.*, u.full_name AS instructor_name, " +
            "(SELECT COUNT(*) FROM enrollments e WHERE e.course_id = c.course_id) AS enrollment_count " +
            "FROM courses c JOIN users u ON c.instructor_id = u.user_id ";

    public List<Course> findAll() {
        List<Course> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(BASE_SELECT + " ORDER BY c.created_at DESC")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load courses", e);
        }
        return list;
    }

    public List<Course> findPublished() {
        List<Course> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE c.is_published = TRUE ORDER BY c.created_at DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load published courses", e);
        }
        return list;
    }

    public List<Course> findByInstructor(long instructorId) {
        List<Course> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE c.instructor_id = ? ORDER BY c.created_at DESC")) {
            ps.setLong(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load instructor courses", e);
        }
        return list;
    }

    public List<Course> search(String keyword, String category, String difficulty) {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE (c.title LIKE ? OR c.description LIKE ?)");
        if (category != null && !category.equals("All")) sql.append(" AND c.category = ?");
        if (difficulty != null && !difficulty.equals("All")) sql.append(" AND c.difficulty = ?");
        sql.append(" ORDER BY c.created_at DESC");
        List<Course> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            String like = "%" + keyword + "%";
            ps.setString(idx++, like);
            ps.setString(idx++, like);
            if (category != null && !category.equals("All")) ps.setString(idx++, category);
            if (difficulty != null && !difficulty.equals("All")) ps.setString(idx++, difficulty);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search courses", e);
        }
        return list;
    }

    public Optional<Course> findById(long id) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE c.course_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load course", e);
        }
        return Optional.empty();
    }

    public Course insert(Course c) {
        String sql = "INSERT INTO courses (instructor_id, title, description, syllabus, category, difficulty, duration_hours, thumbnail_color, is_published) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getInstructorId());
            ps.setString(2, c.getTitle());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getSyllabus());
            ps.setString(5, c.getCategory());
            ps.setString(6, c.getDifficulty().name());
            ps.setInt(7, c.getDurationHours());
            ps.setString(8, c.getThumbnailColor());
            ps.setBoolean(9, c.isPublished());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getLong(1));
            }
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert course", e);
        }
    }

    public void update(Course c) {
        String sql = "UPDATE courses SET title=?, description=?, syllabus=?, category=?, difficulty=?, duration_hours=?, thumbnail_color=?, is_published=?, instructor_id=? WHERE course_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getTitle());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getSyllabus());
            ps.setString(4, c.getCategory());
            ps.setString(5, c.getDifficulty().name());
            ps.setInt(6, c.getDurationHours());
            ps.setString(7, c.getThumbnailColor());
            ps.setBoolean(8, c.isPublished());
            ps.setLong(9, c.getInstructorId());
            ps.setLong(10, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update course", e);
        }
    }

    public void delete(long courseId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM courses WHERE course_id=?")) {
            ps.setLong(1, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete course", e);
        }
    }

    public int countAll() {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM courses")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count courses", e);
        }
        return 0;
    }

    private Course map(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId(rs.getLong("course_id"));
        c.setInstructorId(rs.getLong("instructor_id"));
        c.setInstructorName(rs.getString("instructor_name"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setSyllabus(rs.getString("syllabus"));
        c.setCategory(rs.getString("category"));
        c.setDifficulty(Course.Difficulty.valueOf(rs.getString("difficulty")));
        c.setDurationHours(rs.getInt("duration_hours"));
        c.setThumbnailColor(rs.getString("thumbnail_color"));
        c.setPublished(rs.getBoolean("is_published"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        c.setEnrollmentCount(rs.getInt("enrollment_count"));
        return c;
    }
}
