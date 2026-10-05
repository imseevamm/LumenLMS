package com.lms.model;

import java.time.LocalDateTime;

public class Submission {
    public enum Status { SUBMITTED, GRADED, LATE }

    private long id;
    private long assignmentId;
    private String assignmentTitle; // joined
    private long studentId;
    private String studentName; // joined
    private String submissionText;
    private String attachmentPath;
    private String attachmentName;
    private long attachmentSize;
    private LocalDateTime submittedAt;
    private Integer grade;
    private String feedback;
    private Status status;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(long assignmentId) { this.assignmentId = assignmentId; }
    public String getAssignmentTitle() { return assignmentTitle; }
    public void setAssignmentTitle(String assignmentTitle) { this.assignmentTitle = assignmentTitle; }
    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public long getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(long attachmentSize) { this.attachmentSize = attachmentSize; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
