package com.lms.model;

public class Material {
    public enum Type { LINK, TEXT, FILE }
    private long id;
    private long lessonId;
    private String title;
    private Type type;
    private String urlOrPath;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getLessonId() { return lessonId; }
    public void setLessonId(long lessonId) { this.lessonId = lessonId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getUrlOrPath() { return urlOrPath; }
    public void setUrlOrPath(String urlOrPath) { this.urlOrPath = urlOrPath; }
}
