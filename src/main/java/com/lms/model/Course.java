package com.lms.model;

import java.time.LocalDateTime;

public class Course {
    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

    private long id;
    private long instructorId;
    private String instructorName; // populated via joins for display
    private String title;
    private String description;
    private String syllabus;
    private String category;
    private Difficulty difficulty;
    private int durationHours;
    private String thumbnailColor;
    private boolean published;
    private LocalDateTime createdAt;
    private int enrollmentCount; // populated via join/aggregate

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getInstructorId() { return instructorId; }
    public void setInstructorId(long instructorId) { this.instructorId = instructorId; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }
    public String getThumbnailColor() { return thumbnailColor; }
    public void setThumbnailColor(String thumbnailColor) { this.thumbnailColor = thumbnailColor; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(int enrollmentCount) { this.enrollmentCount = enrollmentCount; }
}
