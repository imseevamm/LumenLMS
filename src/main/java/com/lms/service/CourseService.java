package com.lms.service;

import com.lms.dao.CourseDAO;
import com.lms.dao.ModuleDAO;
import com.lms.dao.EnrollmentDAO;
import com.lms.model.Course;
import com.lms.model.CourseModule;
import com.lms.model.Lesson;
import com.lms.model.Material;
import com.lms.model.Role;
import com.lms.model.User;
import com.lms.security.AuthorizationException;
import com.lms.util.SessionManager;
import com.lms.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

public class CourseService implements Countable {

    private final CourseDAO courseDAO = new CourseDAO();
    private final ModuleDAO moduleDAO = new ModuleDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public static class CourseServiceException extends RuntimeException {
        public CourseServiceException(String message) { super(message); }
    }

    public List<Course> getAllCourses() { return courseDAO.findAll(); }
    public List<Course> getPublishedCourses() { return courseDAO.findPublished(); }
    public List<Course> getCoursesByInstructor(long instructorId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR && caller.getId() != instructorId) {
            throw new AuthorizationException("Instructors can only view their own courses.");
        }
        return courseDAO.findByInstructor(instructorId);
    }
    public Optional<Course> getCourse(long id) { return courseDAO.findById(id); }

    public List<Course> search(String keyword, String category, String difficulty) {
        String k = ValidationUtil.isNotBlank(keyword) ? keyword.trim() : "";
        return courseDAO.search(k, category, difficulty);
    }

    public Course createCourse(Course course) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        requireOwnCourseForInstructor(caller, course.getInstructorId());
        validate(course);
        return courseDAO.insert(course);
    }

    public void updateCourse(Course course) {
        User caller = requireCourseAccess(course.getId());
        requireOwnCourseForInstructor(caller, course.getInstructorId());
        validate(course);
        courseDAO.update(course);
    }

    public void deleteCourse(long courseId) {
        requireCourseAccess(courseId);
        courseDAO.delete(courseId);
    }

    public List<CourseModule> getModules(long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        if (caller.getRole() == Role.INSTRUCTOR) requireOwnCourseForInstructor(caller, courseDAO.findById(courseId).orElseThrow(() -> new CourseServiceException("Course not found.")).getInstructorId());
        if (caller.getRole() == Role.STUDENT && !enrollmentDAO.isEnrolled(caller.getId(), courseId)) throw new AuthorizationException("You must be enrolled in this course.");
        return moduleDAO.findModulesWithLessons(courseId);
    }

    public CourseModule addModule(long courseId, String title, int position) {
        requireCourseAccess(courseId);
        if (!ValidationUtil.isNotBlank(title)) throw new CourseServiceException("Module title is required.");
        CourseModule m = new CourseModule();
        m.setCourseId(courseId);
        m.setTitle(title.trim());
        m.setPosition(position);
        return moduleDAO.insertModule(m);
    }

    public void deleteModule(long moduleId) {
        requireCourseAccess(moduleDAO.findCourseIdByModuleId(moduleId).orElseThrow(() -> new CourseServiceException("Module not found.")));
        moduleDAO.deleteModule(moduleId);
    }

    public Lesson addLesson(long moduleId, String title, String content, int position, int duration) {
        requireCourseAccess(moduleDAO.findCourseIdByModuleId(moduleId).orElseThrow(() -> new CourseServiceException("Module not found.")));
        if (!ValidationUtil.isNotBlank(title)) throw new CourseServiceException("Lesson title is required.");
        Lesson l = new Lesson();
        l.setModuleId(moduleId);
        l.setTitle(title.trim());
        l.setContent(content);
        l.setPosition(position);
        l.setDurationMinutes(duration);
        return moduleDAO.insertLesson(l);
    }

    public void updateLesson(Lesson lesson) {
        requireCourseAccess(moduleDAO.findCourseIdByLessonId(lesson.getId()).orElseThrow(() -> new CourseServiceException("Lesson not found.")));
        if (!ValidationUtil.isNotBlank(lesson.getTitle())) throw new CourseServiceException("Lesson title is required.");
        moduleDAO.updateLesson(lesson);
    }

    public void deleteLesson(long lessonId) {
        requireCourseAccess(moduleDAO.findCourseIdByLessonId(lessonId).orElseThrow(() -> new CourseServiceException("Lesson not found.")));
        moduleDAO.deleteLesson(lessonId);
    }

    public Material addMaterial(long lessonId, String title, Material.Type type, String urlOrPath) {
        requireCourseAccess(moduleDAO.findCourseIdByLessonId(lessonId).orElseThrow(() -> new CourseServiceException("Lesson not found.")));
        if (!ValidationUtil.isNotBlank(title)) throw new CourseServiceException("Material title is required.");
        Material m = new Material();
        m.setLessonId(lessonId);
        m.setTitle(title.trim());
        m.setType(type);
        m.setUrlOrPath(urlOrPath);
        return moduleDAO.insertMaterial(m);
    }

    public List<Material> getMaterials(long lessonId) {
        long courseId = moduleDAO.findCourseIdByLessonId(lessonId).orElseThrow(() -> new CourseServiceException("Lesson not found."));
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        if (caller.getRole() == Role.INSTRUCTOR) requireOwnCourseForInstructor(caller, courseDAO.findById(courseId).orElseThrow(() -> new CourseServiceException("Course not found.")).getInstructorId());
        if (caller.getRole() == Role.STUDENT && !enrollmentDAO.isEnrolled(caller.getId(), courseId)) throw new AuthorizationException("You must be enrolled in this course.");
        return moduleDAO.findMaterialsByLesson(lessonId);
    }

    public int countLessons(long courseId) { return moduleDAO.countLessonsForCourse(courseId); }

    @Override
    public int countAll() { return courseDAO.countAll(); }

    /** Admins may manage any course; instructors only the ones they own. */
    private User requireCourseAccess(long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.ADMIN) return caller;
        Course existing = courseDAO.findById(courseId)
                .orElseThrow(() -> new CourseServiceException("Course not found."));
        requireOwnCourseForInstructor(caller, existing.getInstructorId());
        return caller;
    }

    private void requireOwnCourseForInstructor(User caller, long courseInstructorId) {
        if (caller.getRole() == Role.INSTRUCTOR && caller.getId() != courseInstructorId) {
            throw new AuthorizationException("Instructors can only manage their own courses.");
        }
    }

    private void validate(Course course) {
        if (!ValidationUtil.isNotBlank(course.getTitle())) throw new CourseServiceException("Course title is required.");
        if (course.getDifficulty() == null) throw new CourseServiceException("Please select a difficulty level.");
        if (!ValidationUtil.isNotBlank(course.getCategory())) throw new CourseServiceException("Category is required.");
    }
}
