package com.lms.model;

import java.time.LocalDateTime;

public class Enrollment {
    public enum Status { ACTIVE, COMPLETED, DROPPED }

    private long id;
    private long studentId;
    private long courseId;
    private String courseTitle;      // joined
    private String instructorName;   // joined
    private String studentName;      // joined
    private LocalDateTime enrolledAt;
    private Status status;
    private double progressPercent;  // computed

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }
    public long getCourseId() { return courseId; }
    public void setCourseId(long courseId) { this.courseId = courseId; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }
}
