package com.reichman.campuslostandfound;

import com.google.firebase.firestore.FirebaseFirestore;

// Central place for all reading and writing of user data in Firestore.
public class UserRepository {

    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Creates a user document the first time someone signs in.
    // If it already exists, we leave it alone.
    public void createUserIfNew(final User user) {
        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        db.collection(USERS_COLLECTION)
                                .document(user.getUid())
                                .set(user);
                    }
                });
    }
}