package com.lms.model;

import java.time.LocalDateTime;

/** Core user entity shared by all three roles (encapsulation + single responsibility). */
public class User {
    private long id;
    private String fullName;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active;
    private String avatarPath;
    private String bio;
    private String rollNumber;
    private LocalDateTime createdAt;

    public User() {}

    public User(long id, String fullName, String email, String passwordHash, Role role,
                boolean active, String avatarPath, String bio, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.avatarPath = avatarPath;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String initials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String i = parts[0].substring(0, 1);
        if (parts.length > 1) i += parts[parts.length - 1].substring(0, 1);
        return i.toUpperCase();
    }
}
