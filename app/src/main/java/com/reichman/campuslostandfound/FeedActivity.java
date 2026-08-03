package com.reichman.campuslostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ItemRepository itemRepository;

    // The list of items, and the adapter that displays them
    private final List<Item> items = new ArrayList<>();
    private ItemAdapter adapter;

    // Views
    private RecyclerView recyclerView;
    private ProgressBar loadingSpinner;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        mAuth = FirebaseAuth.getInstance();
        itemRepository = new ItemRepository();

        // Find the views
        recyclerView = findViewById(R.id.itemsRecyclerView);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        emptyMessage = findViewById(R.id.emptyMessage);
        Button signOutButton = findViewById(R.id.signOutButton);
        Button addButton = findViewById(R.id.addButton);

        // Set up the RecyclerView: a vertical list, using our adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(items, item -> {
            // For now, just show which item was tapped.
            // Later this opens the Item Detail page.
            Toast.makeText(this, "Tapped: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        // Sign out
        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });

        // Open the Report page when + is tapped
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        // Load the items from Firestore
        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems(); // reload whenever we return to this screen
    }

    private void loadItems() {
        // Show the spinner, hide the empty message while loading
        loadingSpinner.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        itemRepository.loadFoundItems(new ItemRepository.ItemsCallback() {
            @Override
            public void onItemsLoaded(List<Item> loadedItems) {
                loadingSpinner.setVisibility(View.GONE);

                // Replace our list contents with what came back
                items.clear();
                items.addAll(loadedItems);
                adapter.notifyDataSetChanged();

                // Show the empty message if there's nothing
                if (items.isEmpty()) {
                    emptyMessage.setVisibility(View.VISIBLE);
                } else {
                    emptyMessage.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                loadingSpinner.setVisibility(View.GONE);
                Toast.makeText(FeedActivity.this,
                        "Failed to load items: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}