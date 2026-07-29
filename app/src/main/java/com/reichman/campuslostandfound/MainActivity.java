package com.reichman.campuslostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    // A tag used for log messages, so we can find them in Logcat
    private static final String TAG = "CampusLAF-Login";

    // The request code that identifies our Google sign-in result
    private static final int RC_GOOGLE_SIGN_IN = 1001;

    // Firebase's authentication object — our gateway to sign in / sign up
    private FirebaseAuth mAuth;

    // The Google sign-in helper
    private GoogleSignInClient mGoogleSignInClient;

    // References to the views on screen
    private EditText emailInput;
    private EditText passwordInput;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect to Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Configure Google Sign-In:
        // we ask Google for the user's ID token (so Firebase can verify them) and their email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("345395019660-atfj3kbi0db4152n30mp4p0u0roqtncp.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Find the views from the layout by their IDs
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);
        Button googleButton = findViewById(R.id.googleButton);
        TextView signupLink = findViewById(R.id.signupLink);

        // When "Log in" is tapped, try to log in with email + password
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithEmail();
            }
        });

        // When "Create an account" is tapped, sign up a new email/password user
        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpWithEmail();
            }
        });

        // When "Continue with Google" is tapped, start the Google sign-in flow
        googleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
            }
        });
    }

    // If the user is already signed in, skip the login screen
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToFeed();
        }
    }

    // ---------- EMAIL / PASSWORD ----------

    private void loginWithEmail() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Make sure the fields aren't empty before we try
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter your email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToFeed();
                    } else {
                        Toast.makeText(this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signUpWithEmail() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter an email and password to sign up", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                        goToFeed();
                    } else {
                        Toast.makeText(this,
                                "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ---------- GOOGLE ----------

    // This runs when Google sends back the result of the sign-in screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google sign-in worked — now hand the account to Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToFeed();
                    } else {
                        Toast.makeText(this, "Firebase authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------- SHARED ----------

    // Move to the Feed screen and close the login screen
    private void goToFeed() {
        // Save this user to Firestore (only creates a doc if they're new)
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            User user = new User(
                    firebaseUser.getUid(),
                    firebaseUser.getDisplayName(),
                    firebaseUser.getEmail(),
                    firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null,
                    System.currentTimeMillis()
            );
            userRepository.createUserIfNew(user);
        }

        // Then move to the Feed screen
        Intent intent = new Intent(MainActivity.this, FeedActivity.class);
        startActivity(intent);
        finish();
    }
}