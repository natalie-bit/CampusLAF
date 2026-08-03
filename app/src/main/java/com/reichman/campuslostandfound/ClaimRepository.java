package com.reichman.campuslostandfound;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

// Handles all reading and writing of claims in Firestore.
public class ClaimRepository {

    private final FirebaseFirestore db;
    private static final String CLAIMS_COLLECTION = "claims";
    private static final String ITEMS_COLLECTION = "items";

    public ClaimRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ---------- SUBMIT A CLAIM ----------

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // Creates a new claim on an item (status starts as "pending").
    public void submitClaim(Claim claim, final SimpleCallback callback) {
        db.collection(CLAIMS_COLLECTION)
                .add(claim)
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // ---------- LOAD CLAIMS FOR AN ITEM ----------

    public interface ClaimsCallback {
        void onClaimsLoaded(List<Claim> claims);
        void onError(Exception e);
    }

    // Loads all claims made on a specific item.
    public void loadClaimsForItem(String itemId, final ClaimsCallback callback) {
        db.collection(CLAIMS_COLLECTION)
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Claim> claims = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Claim claim = doc.toObject(Claim.class);
                        claim.setClaimId(doc.getId());
                        claims.add(claim);
                    }
                    callback.onClaimsLoaded(claims);
                })
                .addOnFailureListener(callback::onError);
    }

    // ---------- APPROVE A CLAIM (THE BATCH WRITE) ----------

    // Approving one claim must do THREE things atomically:
    //   1. Set this claim to "approved"
    //   2. Set every OTHER pending claim on the same item to "rejected"
    //   3. Set the item's status to "claimed" and record who claimed it
    // A WriteBatch guarantees all of these happen together, or none do.
    public void approveClaim(final Claim approvedClaim, final SimpleCallback callback) {
        // First, get all claims on this item so we know which others to reject
        db.collection(CLAIMS_COLLECTION)
                .whereEqualTo("itemId", approvedClaim.getItemId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    // 1 & 2: approve the chosen claim, reject all other pending ones
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentReference claimRef = doc.getReference();
                        if (doc.getId().equals(approvedClaim.getClaimId())) {
                            batch.update(claimRef, "status", "approved");
                        } else {
                            // Only touch ones still pending
                            String s = doc.getString("status");
                            if ("pending".equals(s)) {
                                batch.update(claimRef, "status", "rejected");
                            }
                        }
                    }

                    // 3: mark the item as claimed
                    DocumentReference itemRef =
                            db.collection(ITEMS_COLLECTION).document(approvedClaim.getItemId());
                    batch.update(itemRef, "status", "claimed");
                    batch.update(itemRef, "claimedBy", approvedClaim.getClaimantId());

                    // Commit all changes together
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // ---------- REJECT A SINGLE CLAIM ----------

    public void rejectClaim(String claimId, final SimpleCallback callback) {
        db.collection(CLAIMS_COLLECTION)
                .document(claimId)
                .update("status", "rejected")
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // Loads all claims made BY a specific user.
    public void loadMyClaims(String claimantId, final ClaimsCallback callback) {
        db.collection(CLAIMS_COLLECTION)
                .whereEqualTo("claimantId", claimantId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Claim> claims = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Claim claim = doc.toObject(Claim.class);
                        claim.setClaimId(doc.getId());
                        claims.add(claim);
                    }
                    callback.onClaimsLoaded(claims);
                })
                .addOnFailureListener(callback::onError);
    }
}