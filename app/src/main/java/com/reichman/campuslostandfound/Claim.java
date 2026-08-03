package com.reichman.campuslostandfound;

// Represents one claim on an item, in the "claims" collection in Firestore.
public class Claim {

    private String claimId;
    private String itemId;
    private String claimantId;   // who is claiming the item
    private String finderId;     // who found it (duplicated here so the finder can query easily)
    private String proofText;    // "it has a blue keychain"
    private String status;       // "pending", "approved", or "rejected"
    private long createdAt;

    // Firestore REQUIRES an empty constructor
    public Claim() { }

    public Claim(String claimId, String itemId, String claimantId, String finderId,
                 String proofText, String status, long createdAt) {
        this.claimId = claimId;
        this.itemId = itemId;
        this.claimantId = claimantId;
        this.finderId = finderId;
        this.proofText = proofText;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getClaimantId() { return claimantId; }
    public void setClaimantId(String claimantId) { this.claimantId = claimantId; }

    public String getFinderId() { return finderId; }
    public void setFinderId(String finderId) { this.finderId = finderId; }

    public String getProofText() { return proofText; }
    public void setProofText(String proofText) { this.proofText = proofText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}