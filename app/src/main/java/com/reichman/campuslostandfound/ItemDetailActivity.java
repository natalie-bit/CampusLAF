package com.reichman.campuslostandfound;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ItemDetailActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ClaimRepository claimRepository;

    // Views
    private ImageView detailImage;
    private TextView detailTitle, detailStatus, detailDescription, detailCategory, detailLocation;
    private Button claimButton;
    private LinearLayout finderSection, claimsContainer;
    private TextView noClaimsMessage;

    // The item we're showing
    private Item currentItem;
    private String itemId;
    private com.google.firebase.analytics.FirebaseAnalytics mAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        mAuth = FirebaseAuth.getInstance();
        mAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        claimRepository = new ClaimRepository();

        // Find views
        detailImage = findViewById(R.id.detailImage);
        detailTitle = findViewById(R.id.detailTitle);
        detailStatus = findViewById(R.id.detailStatus);
        detailDescription = findViewById(R.id.detailDescription);
        detailCategory = findViewById(R.id.detailCategory);
        detailLocation = findViewById(R.id.detailLocation);
        claimButton = findViewById(R.id.claimButton);
        finderSection = findViewById(R.id.finderSection);
        claimsContainer = findViewById(R.id.claimsContainer);
        noClaimsMessage = findViewById(R.id.noClaimsMessage);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // The feed passed us the item's ID
        itemId = getIntent().getStringExtra("itemId");
        if (itemId == null) {
            Toast.makeText(this, "No item specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        claimButton.setOnClickListener(v -> showClaimDialog());

        loadItem();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Load the item fresh from Firestore (so status is always current)
    private void loadItem() {
        db.collection("items").document(itemId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentItem = doc.toObject(Item.class);
                    currentItem.setItemId(doc.getId());
                    showItem();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show());
    }

    // Fill the screen with the item's details, and decide which mode to show
    private void showItem() {
        detailImage.setImageResource(
                ItemImages.getImageResource(this, currentItem.getPhotoUrl()));
        detailTitle.setText(currentItem.getTitle());
        detailStatus.setText(currentItem.getStatus());
        detailDescription.setText(currentItem.getDescription());
        detailCategory.setText(currentItem.getCategory());
        detailLocation.setText(String.format("%.4f, %.4f",
                currentItem.getLat(), currentItem.getLng()));

        String myUid = mAuth.getCurrentUser().getUid();
        boolean iAmTheFinder = myUid.equals(currentItem.getFinderId());
        boolean stillAvailable = "found".equals(currentItem.getStatus());

        if (iAmTheFinder) {
            // Finder view: show the claims list, hide the claim button
            finderSection.setVisibility(View.VISIBLE);
            claimButton.setVisibility(View.GONE);
            loadClaims();
        } else {
            // Claimant view: show the claim button (only if still available)
            finderSection.setVisibility(View.GONE);
            if (stillAvailable) {
                claimButton.setVisibility(View.VISIBLE);
            } else {
                claimButton.setVisibility(View.GONE);
            }
        }
    }

    // ---------- CLAIMANT: submit a claim ----------

    private void showClaimDialog() {
        // A simple dialog with a text box for the proof description
        final EditText input = new EditText(this);
        input.setHint("Describe something only the owner would know");
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Claim this item")
                .setView(input)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String proof = input.getText().toString().trim();
                    if (TextUtils.isEmpty(proof)) {
                        Toast.makeText(this, "Please describe the item", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitClaim(proof);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitClaim(String proof) {
        String myUid = mAuth.getCurrentUser().getUid();
        Claim claim = new Claim(
                null,
                currentItem.getItemId(),
                myUid,
                currentItem.getFinderId(),
                proof,
                "pending",
                System.currentTimeMillis()
        );

        claimRepository.submitClaim(claim, new ClaimRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ItemDetailActivity.this,
                        "Claim submitted! The finder will review it.", Toast.LENGTH_LONG).show();
                claimButton.setVisibility(View.GONE);
                mAnalytics.logEvent("claim_submitted", new android.os.Bundle());
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ItemDetailActivity.this,
                        "Failed to submit claim: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------- FINDER: view and decide on claims ----------

    private void loadClaims() {
        claimRepository.loadClaimsForItem(itemId, new ClaimRepository.ClaimsCallback() {
            @Override
            public void onClaimsLoaded(List<Claim> claims) {
                claimsContainer.removeAllViews();

                if (claims.isEmpty()) {
                    noClaimsMessage.setVisibility(View.VISIBLE);
                    return;
                }
                noClaimsMessage.setVisibility(View.GONE);

                // Build one block per claim
                for (Claim claim : claims) {
                    addClaimBlock(claim);
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ItemDetailActivity.this,
                        "Failed to load claims", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Creates a small block showing one claim, with Approve/Reject if it's pending
    private void addClaimBlock(final Claim claim) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        block.setLayoutParams(params);
        block.setBackgroundColor(0xFFFFFFFF);

        // Proof text
        TextView proof = new TextView(this);
        proof.setText("\"" + claim.getProofText() + "\"");
        proof.setTextColor(0xFF1A202C);
        proof.setTextSize(16);
        block.addView(proof);

        // Status line
        TextView status = new TextView(this);
        status.setText("Status: " + claim.getStatus());
        status.setTextColor(0xFF718096);
        status.setPadding(0, 8, 0, 8);
        block.addView(status);

        // Only show Approve/Reject if this claim is still pending
        if ("pending".equals(claim.getStatus())) {
            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);

            Button approve = new Button(this);
            approve.setText("Approve");
            approve.setOnClickListener(v -> approveClaim(claim));
            buttonRow.addView(approve);

            Button reject = new Button(this);
            reject.setText("Reject");
            reject.setOnClickListener(v -> rejectClaim(claim));
            buttonRow.addView(reject);

            block.addView(buttonRow);
        }

        claimsContainer.addView(block);
    }

    private void approveClaim(Claim claim) {
        claimRepository.approveClaim(claim, new ClaimRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ItemDetailActivity.this,
                        "Claim approved! Item marked as claimed.", Toast.LENGTH_SHORT).show();
                loadItem(); // reload to refresh everything
                mAnalytics.logEvent("claim_approved", new android.os.Bundle());
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ItemDetailActivity.this,
                        "Failed to approve: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void rejectClaim(Claim claim) {
        claimRepository.rejectClaim(claim.getClaimId(), new ClaimRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ItemDetailActivity.this, "Claim rejected", Toast.LENGTH_SHORT).show();
                loadClaims(); // refresh the claims list
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ItemDetailActivity.this,
                        "Failed to reject", Toast.LENGTH_SHORT).show();
            }
        });
    }
}