package com.lms.model;

public class Lesson {
    private long id;
    private long moduleId;
    private String title;
    private String content;
    private int position;
    private int durationMinutes;
    private boolean completed; // transient - set per-student when loading learning page

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getModuleId() { return moduleId; }
    public void setModuleId(long moduleId) { this.moduleId = moduleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
