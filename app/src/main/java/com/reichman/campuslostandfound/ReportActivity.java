package com.reichman.campuslostandfound;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ReportActivity extends AppCompatActivity {

    // Request code to identify our location-permission request
    private static final int LOCATION_PERMISSION_REQUEST = 2001;

    // The categories shown in the dropdown
    private static final String[] CATEGORIES =
            {"Electronics", "Clothing", "Keys", "Bags", "Documents", "Other"};

    private EditText titleInput;
    private EditText descriptionInput;
    private Spinner categorySpinner;
    private TextView locationText;

    // Firebase + location helpers
    private FirebaseAuth mAuth;
    private ItemRepository itemRepository;
    private FusedLocationProviderClient locationClient;

    // The captured location, stored until submit. Null until the user gets it.
    private Double capturedLat = null;
    private Double capturedLng = null;
    private com.google.firebase.analytics.FirebaseAnalytics mAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mAuth = FirebaseAuth.getInstance();
        mAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this);
        itemRepository = new ItemRepository();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Find the views
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        locationText = findViewById(R.id.locationText);
        Button getLocationButton = findViewById(R.id.getLocationButton);
        Button submitButton = findViewById(R.id.submitButton);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Fill the dropdown with our categories
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
        categorySpinner.setAdapter(adapter);

        // "Use my current location" → check permission, then get GPS
        getLocationButton.setOnClickListener(v -> requestLocation());

        // "Submit" → validate and save
        submitButton.setOnClickListener(v -> submitItem());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // close this screen, return to the feed
        return true;
    }

    // ---------- LOCATION ----------

    private void requestLocation() {
        // Do we already have permission?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Yes → get the location
            fetchLocation();
        } else {
            // No → ask the user for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    // Called by Android after the user responds to the permission dialog
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this,
                        "Location permission is needed to tag where you found the item",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void fetchLocation() {
        // We've checked permission before calling this, but Android requires this guard
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationText.setText("Getting location...");

        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        capturedLat = location.getLatitude();
                        capturedLng = location.getLongitude();
                        locationText.setText(String.format("Location set: %.4f, %.4f",
                                capturedLat, capturedLng));
                    } else {
                        locationText.setText("Couldn't get location — try again");
                    }
                })
                .addOnFailureListener(e ->
                        locationText.setText("Location error: " + e.getMessage()));
    }

    // ---------- SUBMIT ----------

    private void submitItem() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();

        // Validate
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter what you found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (capturedLat == null || capturedLng == null) {
            Toast.makeText(this, "Please set the location", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the item. photoUrl is empty (GPS-only version). status starts as "found".
        Item item = new Item(
                null,                       // itemId — Firestore generates it
                title,
                description,
                category,
                "",                         // photoUrl — none in this version
                capturedLat,
                capturedLng,
                "Lat/Lng recorded",         // locationLabel
                user.getUid(),              // finderId
                "found",                    // status
                null,                       // claimedBy
                System.currentTimeMillis()  // createdAt
        );

        // Write it, then return to the feed
        itemRepository.createItem(item, new ItemRepository.CreateCallback() {
            @Override
            public void onSuccess() {
                android.os.Bundle reportBundle = new android.os.Bundle();
                reportBundle.putString("category", item.getCategory());
                mAnalytics.logEvent("item_reported", reportBundle);
                Toast.makeText(ReportActivity.this, "Item reported!", Toast.LENGTH_SHORT).show();
                finish(); // close this screen, back to the feed
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ReportActivity.this,
                        "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}