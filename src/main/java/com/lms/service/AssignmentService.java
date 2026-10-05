package com.lms.service;

import com.lms.dao.AssignmentDAO;
import com.lms.dao.CourseDAO;
import com.lms.dao.EnrollmentDAO;
import com.lms.dao.SubmissionDAO;
import com.lms.model.Assignment;
import com.lms.model.Notification;
import com.lms.model.Role;
import com.lms.model.Submission;
import com.lms.model.User;
import com.lms.security.AuthorizationException;
import com.lms.util.SessionManager;
import com.lms.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AssignmentService implements Countable {

    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final NotificationService notificationService = new NotificationService();

    public static class AssignmentException extends RuntimeException {
        public AssignmentException(String message) { super(message); }
    }

    public List<Assignment> getForCourse(long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, courseId);
        return assignmentDAO.findByCourse(courseId);
    }

    public List<Assignment> getForStudent(long studentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) {
            throw new AuthorizationException("You can only view your own assignments.");
        }
        return assignmentDAO.findByStudentEnrollments(studentId);
    }

    public Optional<Assignment> getById(long id) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        Optional<Assignment> result = assignmentDAO.findById(id);
        if (result.isEmpty()) return result;
        Assignment a = result.get();
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, a.getCourseId());
        if (caller.getRole() == Role.STUDENT && !enrollmentDAO.isEnrolled(caller.getId(), a.getCourseId())) {
            throw new AuthorizationException("You must be enrolled in this course to access this assignment.");
        }
        return result;
    }

    public Assignment create(long courseId, String title, String description, String instructions,
                              LocalDateTime deadline, int maxMarks) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, courseId);
        validate(title, deadline, maxMarks, true);
        Assignment a = new Assignment();
        a.setCourseId(courseId);
        a.setTitle(title.trim());
        a.setDescription(description == null ? null : description.trim());
        a.setInstructions(instructions == null ? null : instructions.trim());
        a.setDeadline(deadline);
        a.setMaxMarks(maxMarks);
        return assignmentDAO.insert(a);
    }

    public void update(Assignment a) {
        if (a == null) throw new AssignmentException("Assignment is required.");
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Assignment existing = assignmentDAO.findById(a.getId()).orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, existing.getCourseId());
        // A deadline must be in the future only when it is being changed; otherwise an assignment whose
        // deadline has already passed could never have its title, instructions or marks corrected.
        boolean deadlineChanged = a.getDeadline() == null || !a.getDeadline().equals(existing.getDeadline());
        validate(a.getTitle(), a.getDeadline(), a.getMaxMarks(), deadlineChanged);
        a.setCourseId(existing.getCourseId());
        a.setTitle(a.getTitle().trim());
        a.setDescription(a.getDescription() == null ? null : a.getDescription().trim());
        a.setInstructions(a.getInstructions() == null ? null : a.getInstructions().trim());
        assignmentDAO.update(a);
    }

    public void delete(long id) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Assignment a = assignmentDAO.findById(id).orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, a.getCourseId());
        assignmentDAO.delete(id);
    }

    public Optional<Submission> findSubmission(long assignmentId, long studentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        Assignment a = assignmentDAO.findById(assignmentId).orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (caller.getRole() == Role.STUDENT) {
            if (caller.getId() != studentId) throw new AuthorizationException("You can only view your own submission.");
            if (!enrollmentDAO.isEnrolled(studentId, a.getCourseId())) throw new AuthorizationException("You are not enrolled in this course.");
        } else if (caller.getRole() == Role.INSTRUCTOR) {
            requireInstructorCourse(caller, a.getCourseId());
        }
        return submissionDAO.findByAssignmentAndStudent(assignmentId, studentId);
    }

    public Submission submit(long assignmentId, long studentId, String text, boolean overdue) {
        return submit(assignmentId, studentId, text, overdue, null, null, 0);
    }

    public Submission submit(long assignmentId, long studentId, String text, boolean overdue,
                             String attachmentPath, String attachmentName, long attachmentSize) {
        User caller = SessionManager.getInstance().requireRole(Role.STUDENT);
        if (caller.getId() != studentId) throw new AuthorizationException("You can only submit your own work.");
        Assignment assignment = assignmentDAO.findById(assignmentId).orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (!enrollmentDAO.isEnrolled(studentId, assignment.getCourseId())) {
            throw new AuthorizationException("You must be enrolled in this course to submit the assignment.");
        }
        if (!ValidationUtil.isNotBlank(text) && attachmentPath == null) {
            throw new AssignmentException("Add some written work or attach a PDF before submitting.");
        }
        if (submissionDAO.findByAssignmentAndStudent(assignmentId, studentId).isPresent()) {
            throw new AssignmentException("You have already submitted this assignment.");
        }
        Submission s = new Submission();
        s.setAssignmentId(assignmentId);
        s.setStudentId(studentId);
        s.setSubmissionText(ValidationUtil.isNotBlank(text) ? text.trim() : null);
        s.setAttachmentPath(attachmentPath);
        s.setAttachmentName(attachmentName);
        s.setAttachmentSize(Math.max(0, attachmentSize));
        s.setStatus(assignment.getDeadline().isBefore(LocalDateTime.now()) ? Submission.Status.LATE : Submission.Status.SUBMITTED);
        return submissionDAO.insert(s);
    }

    public List<Submission> getSubmissionsForAssignment(long assignmentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Assignment a = assignmentDAO.findById(assignmentId).orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, a.getCourseId());
        return submissionDAO.findByAssignment(assignmentId);
    }

    public List<Submission> getSubmissionsForStudent(long studentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) throw new AuthorizationException("You can only view your own submissions.");
        return submissionDAO.findByStudent(studentId);
    }

    public void updateSubmissionAttachmentPath(long submissionId, String attachmentPath) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Submission submission = submissionDAO.findById(submissionId)
                .orElseThrow(() -> new AssignmentException("Submission not found."));
        Assignment assignment = assignmentDAO.findById(submission.getAssignmentId())
                .orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, assignment.getCourseId());
        submissionDAO.updateAttachmentPath(submissionId, attachmentPath);
    }

    public void grade(long submissionId, int grade, String feedback, long studentId, String assignmentTitle) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Submission submission = submissionDAO.findById(submissionId).orElseThrow(() -> new AssignmentException("Submission not found."));
        Assignment assignment = assignmentDAO.findById(submission.getAssignmentId()).orElseThrow(() -> new AssignmentException("Assignment not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, assignment.getCourseId());
        if (grade < 0 || grade > assignment.getMaxMarks()) throw new AssignmentException("Grade must be between 0 and " + assignment.getMaxMarks() + ".");
        submissionDAO.grade(submissionId, grade, feedback == null ? null : feedback.trim());
        notificationService.notify(submission.getStudentId(), "Assignment Graded",
                "\"" + assignment.getTitle() + "\" has been graded: " + grade + " marks.", Notification.Type.INFO);
    }

    @Override
    public int countAll() { return assignmentDAO.countAll(); }

    private void requireInstructorCourse(User instructor, long courseId) {
        long ownerId = courseDAO.findById(courseId).orElseThrow(() -> new AssignmentException("Course not found.")).getInstructorId();
        if (ownerId != instructor.getId()) throw new AuthorizationException("Instructors can only manage assignments in their own courses.");
    }

    private void validate(String title, LocalDateTime deadline, int maxMarks, boolean requireFutureDeadline) {
        if (!ValidationUtil.isNotBlank(title)) throw new AssignmentException("Assignment title is required.");
        if (deadline == null) throw new AssignmentException("A deadline is required.");
        if (requireFutureDeadline && deadline.isBefore(LocalDateTime.now())) throw new AssignmentException("Deadline must be a future date and time.");
        if (maxMarks < 1) throw new AssignmentException("Maximum marks must be at least 1.");
    }
}
