package com.reichman.campuslostandfound;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// Central place for reading and writing found items in Firestore.
public class ItemRepository {

    private final FirebaseFirestore db;
    private static final String ITEMS_COLLECTION = "items";

    public ItemRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // A small interface so the Feed screen can be told when items arrive (or fail).
    // Because Firestore is asynchronous, we can't just "return" the list —
    // we hand back the results later, through this callback.
    public interface ItemsCallback {
        void onItemsLoaded(List<Item> items);
        void onError(Exception e);
    }

    // Loads all items whose status is "found", newest first.
    public void loadFoundItems(final ItemsCallback callback) {
        db.collection(ITEMS_COLLECTION)
                .whereEqualTo("status", "found")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Item item = doc.toObject(Item.class);
                        item.setItemId(doc.getId()); // store the document's ID on the object
                        items.add(item);
                    }
                    callback.onItemsLoaded(items);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }
    // A callback for when the write finishes (success or failure).
    public interface CreateCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // Creates a new item document in Firestore.
    public void createItem(Item item, final CreateCallback callback) {
        db.collection(ITEMS_COLLECTION)
                .add(item)   // .add() creates a new document with an auto-generated ID
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e));
    }

    // ---------- ITEMS I FOUND ----------

    // Loads all items posted by a specific user (any status).
    public void loadItemsIFound(String uid, final ItemsCallback callback) {
        db.collection(ITEMS_COLLECTION)
                .whereEqualTo("finderId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Item item = doc.toObject(Item.class);
                        item.setItemId(doc.getId());
                        items.add(item);
                    }
                    callback.onItemsLoaded(items);
                })
                .addOnFailureListener(callback::onError);
    }

    // ---------- ITEMS BY THEIR IDs ----------

    // Loads a set of items given a list of their IDs (used for "items I claimed").
    public void loadItemsByIds(List<String> itemIds, final ItemsCallback callback) {
        List<Item> results = new ArrayList<>();
        if (itemIds.isEmpty()) {
            callback.onItemsLoaded(results);
            return;
        }
        // Firestore "in" queries handle up to 10 IDs at a time — fine for this project.
        db.collection(ITEMS_COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), itemIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Item item = doc.toObject(Item.class);
                        item.setItemId(doc.getId());
                        results.add(item);
                    }
                    callback.onItemsLoaded(results);
                })
                .addOnFailureListener(callback::onError);
    }
}