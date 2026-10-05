package com.lms.dao;

import com.lms.model.CourseModule;
import com.lms.model.Lesson;
import com.lms.model.Material;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Handles course_modules, lessons and materials - grouped together since they're always used as a tree. */
public class ModuleDAO {

    public java.util.Optional<Long> findCourseIdByModuleId(long moduleId) {
        return findCourseId("SELECT course_id FROM course_modules WHERE module_id=?", moduleId);
    }

    public java.util.Optional<Long> findCourseIdByLessonId(long lessonId) {
        String sql = "SELECT m.course_id FROM course_modules m JOIN lessons l ON l.module_id=m.module_id WHERE l.lesson_id=?";
        return findCourseId(sql, lessonId);
    }

    private java.util.Optional<Long> findCourseId(String sql, long id) {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return java.util.Optional.of(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve course ownership", e);
        }
        return java.util.Optional.empty();
    }

    public List<CourseModule> findModulesWithLessons(long courseId) {
        List<CourseModule> modules = new ArrayList<>();
        String sql = "SELECT * FROM course_modules WHERE course_id=? ORDER BY position";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CourseModule m = new CourseModule();
                    m.setId(rs.getLong("module_id"));
                    m.setCourseId(rs.getLong("course_id"));
                    m.setTitle(rs.getString("title"));
                    m.setPosition(rs.getInt("position"));
                    m.setLessons(findLessonsByModule(m.getId()));
                    modules.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load modules", e);
        }
        return modules;
    }

    public List<Lesson> findLessonsByModule(long moduleId) {
        List<Lesson> lessons = new ArrayList<>();
        String sql = "SELECT * FROM lessons WHERE module_id=? ORDER BY position";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, moduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lessons.add(mapLesson(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load lessons", e);
        }
        return lessons;
    }

    public CourseModule insertModule(CourseModule m) {
        String sql = "INSERT INTO course_modules (course_id, title, position) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, m.getCourseId());
            ps.setString(2, m.getTitle());
            ps.setInt(3, m.getPosition());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getLong(1));
            }
            return m;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert module", e);
        }
    }

    public void updateModule(CourseModule m) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE course_modules SET title=?, position=? WHERE module_id=?")) {
            ps.setString(1, m.getTitle());
            ps.setInt(2, m.getPosition());
            ps.setLong(3, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update module", e);
        }
    }

    public void deleteModule(long moduleId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM course_modules WHERE module_id=?")) {
            ps.setLong(1, moduleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete module", e);
        }
    }

    public Lesson insertLesson(Lesson l) {
        String sql = "INSERT INTO lessons (module_id, title, content, position, duration_minutes) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, l.getModuleId());
            ps.setString(2, l.getTitle());
            ps.setString(3, l.getContent());
            ps.setInt(4, l.getPosition());
            ps.setInt(5, l.getDurationMinutes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) l.setId(keys.getLong(1));
            }
            return l;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert lesson", e);
        }
    }

    public void updateLesson(Lesson l) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE lessons SET title=?, content=?, position=?, duration_minutes=? WHERE lesson_id=?")) {
            ps.setString(1, l.getTitle());
            ps.setString(2, l.getContent());
            ps.setInt(3, l.getPosition());
            ps.setInt(4, l.getDurationMinutes());
            ps.setLong(5, l.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update lesson", e);
        }
    }

    public void deleteLesson(long lessonId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM lessons WHERE lesson_id=?")) {
            ps.setLong(1, lessonId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete lesson", e);
        }
    }

    public List<Material> findMaterialsByLesson(long lessonId) {
        List<Material> materials = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM materials WHERE lesson_id=?")) {
            ps.setLong(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Material mat = new Material();
                    mat.setId(rs.getLong("material_id"));
                    mat.setLessonId(rs.getLong("lesson_id"));
                    mat.setTitle(rs.getString("title"));
                    mat.setType(Material.Type.valueOf(rs.getString("material_type")));
                    mat.setUrlOrPath(rs.getString("url_or_path"));
                    materials.add(mat);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load materials", e);
        }
        return materials;
    }

    public Material insertMaterial(Material m) {
        String sql = "INSERT INTO materials (lesson_id, title, material_type, url_or_path) VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, m.getLessonId());
            ps.setString(2, m.getTitle());
            ps.setString(3, m.getType().name());
            ps.setString(4, m.getUrlOrPath());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getLong(1));
            }
            return m;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert material", e);
        }
    }

    public int countLessonsForCourse(long courseId) {
        String sql = "SELECT COUNT(*) FROM lessons l JOIN course_modules m ON l.module_id = m.module_id WHERE m.course_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count lessons", e);
        }
        return 0;
    }

    private Lesson mapLesson(ResultSet rs) throws SQLException {
        Lesson l = new Lesson();
        l.setId(rs.getLong("lesson_id"));
        l.setModuleId(rs.getLong("module_id"));
        l.setTitle(rs.getString("title"));
        l.setContent(rs.getString("content"));
        l.setPosition(rs.getInt("position"));
        l.setDurationMinutes(rs.getInt("duration_minutes"));
        return l;
    }
}
