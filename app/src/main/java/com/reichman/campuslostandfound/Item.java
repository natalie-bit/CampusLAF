package com.reichman.campuslostandfound;

// Represents one found item in the "items" collection in Firestore.
public class Item {

    private String itemId;
    private String title;
    private String description;
    private String category;
    private String photoUrl;
    private double lat;
    private double lng;
    private String locationLabel;
    private String finderId;
    private String status;      // "found", "claimed", or "returned"
    private String claimedBy;   // uid of whoever claimed it, or null
    private long createdAt;

    // Firestore REQUIRES an empty constructor
    public Item() { }

    public Item(String itemId, String title, String description, String category,
                String photoUrl, double lat, double lng, String locationLabel,
                String finderId, String status, String claimedBy, long createdAt) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.photoUrl = photoUrl;
        this.lat = lat;
        this.lng = lng;
        this.locationLabel = locationLabel;
        this.finderId = finderId;
        this.status = status;
        this.claimedBy = claimedBy;
        this.createdAt = createdAt;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public String getFinderId() { return finderId; }
    public void setFinderId(String finderId) { this.finderId = finderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}