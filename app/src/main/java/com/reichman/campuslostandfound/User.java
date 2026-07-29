package com.reichman.campuslostandfound;

// Represents one user record in the "users" collection in Firestore.
// Firestore automatically converts between this object and a database document.
public class User {

    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private long createdAt;

    // Firestore REQUIRES an empty constructor to rebuild objects when reading
    public User() { }

    public User(String uid, String displayName, String email, String photoUrl, long createdAt) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
    }

    // Getters and setters — Firestore uses these to read/write each field
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}