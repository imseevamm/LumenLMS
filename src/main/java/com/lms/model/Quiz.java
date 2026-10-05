package com.lms.model;

import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private long id;
    private long courseId;
    private String title;
    private String description;
    private int durationMinutes;
    private boolean published;
    private List<QuizQuestion> questions = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getCourseId() { return courseId; }
    public void setCourseId(long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }

    public int totalMarks() {
        return questions.stream().mapToInt(QuizQuestion::getMarks).sum();
    }
}
