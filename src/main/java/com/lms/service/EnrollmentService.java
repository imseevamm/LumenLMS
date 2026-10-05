package com.lms.service;

import com.lms.dao.CourseDAO;
import com.lms.dao.EnrollmentDAO;
import com.lms.dao.ModuleDAO;
import com.lms.dao.ProgressDAO;
import com.lms.model.Enrollment;
import com.lms.model.Notification;
import com.lms.model.Role;
import com.lms.model.User;
import com.lms.security.AuthorizationException;
import com.lms.util.SessionManager;

import java.util.List;

public class EnrollmentService implements Countable {

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final ProgressDAO progressDAO = new ProgressDAO();
    private final ModuleDAO moduleDAO = new ModuleDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final NotificationService notificationService = new NotificationService();

    public static class EnrollmentException extends RuntimeException {
        public EnrollmentException(String message) { super(message); }
    }

    public Enrollment enroll(long studentId, long courseId, String courseTitle) {
        User caller = SessionManager.getInstance().requireRole(Role.STUDENT);
        if (caller.getId() != studentId) throw new AuthorizationException("You can only manage your own enrollments.");
        if (courseDAO.findById(courseId).isEmpty()) throw new EnrollmentException("Course not found.");
        if (enrollmentDAO.isEnrolled(studentId, courseId)) throw new EnrollmentException("You are already enrolled in this course.");
        Enrollment e = enrollmentDAO.enroll(studentId, courseId);
        notificationService.notify(studentId, "Enrollment Confirmed",
                "You have successfully enrolled in \"" + courseTitle + "\".", Notification.Type.SUCCESS);
        return e;
    }

    public List<Enrollment> getStudentEnrollments(long studentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) throw new AuthorizationException("You can only view your own enrollments.");
        List<Enrollment> enrollments = enrollmentDAO.findByStudent(studentId);
        for (Enrollment e : enrollments) {
            int totalLessons = moduleDAO.countLessonsForCourse(e.getCourseId());
            double pct = progressDAO.progressPercent(e.getId(), totalLessons);
            e.setProgressPercent(pct);
            if (pct >= 100 && e.getStatus() == Enrollment.Status.ACTIVE) {
                enrollmentDAO.updateStatus(e.getId(), Enrollment.Status.COMPLETED);
                e.setStatus(Enrollment.Status.COMPLETED);
            }
        }
        return enrollments;
    }

    public List<Enrollment> getCourseEnrollments(long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR) {
            long ownerId = courseDAO.findById(courseId).orElseThrow(() -> new EnrollmentException("Course not found.")).getInstructorId();
            if (ownerId != caller.getId()) throw new AuthorizationException("Instructors can only view students in their own courses.");
        }
        return enrollmentDAO.findByCourse(courseId);
    }

    public boolean isEnrolled(long studentId, long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) throw new AuthorizationException("You can only check your own enrollment.");
        return enrollmentDAO.isEnrolled(studentId, courseId);
    }

    public void markLessonComplete(long enrollmentId, long lessonId) {
        User caller = SessionManager.getInstance().requireRole(Role.STUDENT);
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId).orElseThrow(() -> new EnrollmentException("Enrollment not found."));
        if (enrollment.getStudentId() != caller.getId()) throw new AuthorizationException("You can only update your own progress.");
        long lessonCourse = moduleDAO.findCourseIdByLessonId(lessonId).orElseThrow(() -> new EnrollmentException("Lesson not found."));
        if (lessonCourse != enrollment.getCourseId()) throw new AuthorizationException("That lesson does not belong to your course.");
        progressDAO.markLessonComplete(enrollmentId, lessonId);

        // Keep the stored status in sync right away so admin completion statistics never lag behind
        // the student's real progress.
        if (enrollment.getStatus() == Enrollment.Status.ACTIVE) {
            int total = moduleDAO.countLessonsForCourse(enrollment.getCourseId());
            if (progressDAO.progressPercent(enrollmentId, total) >= 100) {
                enrollmentDAO.updateStatus(enrollmentId, Enrollment.Status.COMPLETED);
            }
        }
    }

    /** Ids of the lessons a student has completed for one enrollment (students: own only; instructors: own courses only). */
    public java.util.Set<Long> completedLessonIds(long enrollmentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT, Role.INSTRUCTOR);
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId).orElseThrow(() -> new EnrollmentException("Enrollment not found."));
        if (caller.getRole() == Role.STUDENT && enrollment.getStudentId() != caller.getId()) {
            throw new AuthorizationException("You can only view your own progress.");
        }
        if (caller.getRole() == Role.INSTRUCTOR) {
            long owner = courseDAO.findById(enrollment.getCourseId()).orElseThrow(() -> new EnrollmentException("Course not found.")).getInstructorId();
            if (owner != caller.getId()) throw new AuthorizationException("You can only view progress for your own courses.");
        }
        return progressDAO.completedLessonIds(enrollmentId);
    }

    public double progressFor(long enrollmentId, long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT, Role.INSTRUCTOR);
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId).orElseThrow(() -> new EnrollmentException("Enrollment not found."));
        if (caller.getRole() == Role.STUDENT && enrollment.getStudentId() != caller.getId()) throw new AuthorizationException("You can only view your own progress.");
        if (enrollment.getCourseId() != courseId) throw new EnrollmentException("Enrollment does not belong to this course.");
        if (caller.getRole() == Role.INSTRUCTOR) {
            long owner = courseDAO.findById(courseId).orElseThrow(() -> new EnrollmentException("Course not found.")).getInstructorId();
            if (owner != caller.getId()) throw new AuthorizationException("You can only view progress for your own courses.");
        }
        int total = moduleDAO.countLessonsForCourse(courseId);
        return progressDAO.progressPercent(enrollmentId, total);
    }

    @Override
    public int countAll() { return enrollmentDAO.countAll(); }
    public double completionRate() { return enrollmentDAO.completionRate(); }
}
