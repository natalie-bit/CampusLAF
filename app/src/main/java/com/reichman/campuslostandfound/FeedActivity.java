package com.reichman.campuslostandfound;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FeedActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        mAuth = FirebaseAuth.getInstance();

        // Show who is logged in, to prove sign-in worked
        TextView welcomeText = findViewById(R.id.welcomeText);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getEmail();
            if (name == null) {
                name = user.getDisplayName(); // Google users have a display name
            }
            welcomeText.setText("Signed in as:\n" + name);
        }

        // Sign-out button — logs the user out and returns to login
        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                finish(); // closes this screen; login screen's onStart will show again
            }
        });
    }
}