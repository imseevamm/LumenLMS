package com.lms.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizAttempt {
    private long id;
    private long quizId;
    private String quizTitle;
    private long studentId;
    private double score;
    private double totalMarks;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private List<QuizAnswer> answers = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getQuizId() { return quizId; }
    public void setQuizId(long quizId) { this.quizId = quizId; }
    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }
    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(double totalMarks) { this.totalMarks = totalMarks; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public List<QuizAnswer> getAnswers() { return answers; }
    public void setAnswers(List<QuizAnswer> answers) { this.answers = answers; }

    public double percentage() {
        return totalMarks == 0 ? 0 : (score / totalMarks) * 100.0;
    }
}
