package com.reichman.campuslostandfound;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all reading and writing of claims in Firestore.
 *
 * Keeping database access in a repository (rather than inside activities) separates
 * the UI code from the data code, so each is easier to read, reuse, and maintain.
 */
public class ClaimRepository {

    private final FirebaseFirestore db;
    private static final String CLAIMS_COLLECTION = "claims";
    private static final String ITEMS_COLLECTION = "items";

    public ClaimRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // A callback for writes that either succeed or fail. Firestore is asynchronous,
    // so results are reported later through this interface, not returned directly.
    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // A callback for reads that return a list of claims.
    public interface ClaimsCallback {
        void onClaimsLoaded(List<Claim> claims);
        void onError(Exception e);
    }

    // ---------- SUBMIT A CLAIM ----------

    /** Creates a new claim on an item. The claim starts with status "pending". */
    public void submitClaim(Claim claim, final SimpleCallback callback) {
        db.collection(CLAIMS_COLLECTION)
                .add(claim)   // .add() creates a document with an auto-generated ID
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // ---------- LOAD CLAIMS FOR AN ITEM ----------

    /** Loads every claim made on a specific item (used by the finder to review them). */
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

    // ---------- APPROVE A CLAIM (ATOMIC BATCH WRITE) ----------

    /**
     * Approves one claim. This is the most important operation in the app, because it
     * touches THREE documents that must all stay consistent with each other:
     *
     *   1. The chosen claim           -> "approved"
     *   2. Every OTHER pending claim  -> "rejected"  (so no one else can still win it)
     *   3. The item itself            -> "claimed", and records who claimed it
     *
     * All three are committed together in a single WriteBatch, which is atomic: either
     * every change lands, or none do. Without this, a crash between separate writes could
     * leave the item in a broken state where two people both believe they won it.
     */
    public void approveClaim(final Claim approvedClaim, final SimpleCallback callback) {
        // First read all claims on this item, so we know which others to reject.
        db.collection(CLAIMS_COLLECTION)
                .whereEqualTo("itemId", approvedClaim.getItemId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    // Steps 1 & 2: approve the chosen claim, reject any other pending ones.
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentReference claimRef = doc.getReference();
                        if (doc.getId().equals(approvedClaim.getClaimId())) {
                            batch.update(claimRef, "status", "approved");
                        } else if ("pending".equals(doc.getString("status"))) {
                            batch.update(claimRef, "status", "rejected");
                        }
                    }

                    // Step 3: mark the item as claimed by the approved claimant.
                    DocumentReference itemRef =
                            db.collection(ITEMS_COLLECTION).document(approvedClaim.getItemId());
                    batch.update(itemRef, "status", "claimed");
                    batch.update(itemRef, "claimedBy", approvedClaim.getClaimantId());

                    // Commit all of the above together, atomically.
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // ---------- REJECT A SINGLE CLAIM ----------

    /** Rejects one claim outright, without approving anyone. */
    public void rejectClaim(String claimId, final SimpleCallback callback) {
        db.collection(CLAIMS_COLLECTION)
                .document(claimId)
                .update("status", "rejected")
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // ---------- LOAD CLAIMS MADE BY ONE USER ----------

    /** Loads every claim submitted by a given user (used by the "Items I claimed" list). */
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