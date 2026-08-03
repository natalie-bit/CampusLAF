package com.reichman.campuslostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ItemRepository itemRepository;
    private ClaimRepository claimRepository;

    // Two lists + two adapters (one per RecyclerView)
    private final List<Item> foundItems = new ArrayList<>();
    private final List<Item> claimedItems = new ArrayList<>();
    private ItemAdapter foundAdapter;
    private ItemAdapter claimedAdapter;

    private TextView foundEmpty, claimedEmpty, profileEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        itemRepository = new ItemRepository();
        claimRepository = new ClaimRepository();

        profileEmail = findViewById(R.id.profileEmail);
        foundEmpty = findViewById(R.id.foundEmpty);
        claimedEmpty = findViewById(R.id.claimedEmpty);
        Button backButton = findViewById(R.id.backButton);
        Button signOutButton = findViewById(R.id.signOutButton);

        RecyclerView foundRecycler = findViewById(R.id.foundRecyclerView);
        RecyclerView claimedRecycler = findViewById(R.id.claimedRecyclerView);

        // Show who's signed in
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String label = user.getEmail() != null ? user.getEmail() : user.getDisplayName();
            profileEmail.setText("Signed in as " + label);
        }

        // Set up both RecyclerViews with the SAME adapter class we used on the feed.
        // Tapping an item here also opens its detail page.
        foundAdapter = new ItemAdapter(foundItems, item -> openDetail(item));
        foundRecycler.setLayoutManager(new LinearLayoutManager(this));
        foundRecycler.setAdapter(foundAdapter);

        claimedAdapter = new ItemAdapter(claimedItems, item -> openDetail(item));
        claimedRecycler.setLayoutManager(new LinearLayoutManager(this));
        claimedRecycler.setAdapter(claimedAdapter);

        backButton.setOnClickListener(v -> finish());
        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            // Go all the way back to login
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadMyData();
    }

    private void openDetail(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("itemId", item.getItemId());
        startActivity(intent);
    }

    private void loadMyData() {
        String uid = mAuth.getCurrentUser().getUid();

        // ----- Items I found -----
        itemRepository.loadItemsIFound(uid, new ItemRepository.ItemsCallback() {
            @Override
            public void onItemsLoaded(List<Item> items) {
                foundItems.clear();
                foundItems.addAll(items);
                foundAdapter.notifyDataSetChanged();
                foundEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load your items", Toast.LENGTH_SHORT).show();
            }
        });

        // ----- Items I claimed (two steps: my claims → their items) -----
        claimRepository.loadMyClaims(uid, new ClaimRepository.ClaimsCallback() {
            @Override
            public void onClaimsLoaded(List<Claim> claims) {
                // Collect the item IDs from my claims
                List<String> itemIds = new ArrayList<>();
                for (Claim c : claims) {
                    if (!itemIds.contains(c.getItemId())) {
                        itemIds.add(c.getItemId());
                    }
                }
                // Then fetch those items
                itemRepository.loadItemsByIds(itemIds, new ItemRepository.ItemsCallback() {
                    @Override
                    public void onItemsLoaded(List<Item> items) {
                        claimedItems.clear();
                        claimedItems.addAll(items);
                        claimedAdapter.notifyDataSetChanged();
                        claimedEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ProfileActivity.this,
                                "Failed to load claimed items", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this,
                        "Failed to load your claims", Toast.LENGTH_SHORT).show();
            }
        });
    }
}