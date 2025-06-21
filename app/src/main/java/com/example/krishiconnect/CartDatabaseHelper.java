package com.example.krishiconnect;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class CartDatabaseHelper {

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final Context context;

    public CartDatabaseHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    public void addItem(String name, String price, String imageName, String type, String sellerId) {
        String uid = getUserId();
        if (uid == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        getUserType(uid, userType -> {
            if (userType == null) {
                Toast.makeText(context, "User type not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("item_name", name);
            item.put("item_price", price);
            item.put("image_name", imageName);
            item.put("type", type);
            item.put("seller_id", sellerId);

            db.collection("Users").document(userType)
                    .collection("users").document(uid)
                    .collection("cart")
                    .add(item)
                    .addOnSuccessListener(documentReference ->
                            Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    public void removeItem(String name) {
        String uid = getUserId();
        if (uid == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        getUserType(uid, userType -> {
            if (userType == null) {
                Toast.makeText(context, "User type not found", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Users").document(userType)
                    .collection("users").document(uid)
                    .collection("cart")
                    .whereEqualTo("item_name", name)
                    .limit(1)  // ⚡ REMOVE ONLY ONE DOCUMENT!
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Failed to remove item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(context, "Item not found in cart", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to access cart: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }


    /**
     * Fetch the count of a specific item in the user's cart
     *
     * @param itemName item name to look for
     * @param callback returns the count (int) asynchronously
     */
    public void getItemCount(String itemName, ItemCountCallback callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onItemCount(0);
            return;
        }

        getUserType(uid, userType -> {
            if (userType == null) {
                callback.onItemCount(0);
                return;
            }

            db.collection("Users").document(userType)
                    .collection("users").document(uid)
                    .collection("cart")
                    .whereEqualTo("item_name", itemName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int count = queryDocumentSnapshots.size();
                        callback.onItemCount(count);
                    })
                    .addOnFailureListener(e -> {
                        callback.onItemCount(0);
                    });
        });
    }

    private void getUserType(String uid, UserTypeCallback callback) {
        DocumentReference buyerRef = db.collection("Users").document("Buyer")
                .collection("users").document(uid);

        buyerRef.get().addOnSuccessListener(buyerDoc -> {
            if (buyerDoc.exists()) {
                callback.onUserTypeFound("Buyer");
            } else {
                DocumentReference sellerRef = db.collection("Users").document("Seller")
                        .collection("users").document(uid);

                sellerRef.get().addOnSuccessListener(sellerDoc -> {
                    if (sellerDoc.exists()) {
                        callback.onUserTypeFound("Seller");
                    } else {
                        callback.onUserTypeFound(null);
                    }
                }).addOnFailureListener(e -> callback.onUserTypeFound(null));
            }
        }).addOnFailureListener(e -> callback.onUserTypeFound(null));
    }

    // ✅ Callback interface for item count
    public interface ItemCountCallback {
        void onItemCount(int count);
    }

    // Already present
    interface UserTypeCallback {
        void onUserTypeFound(String userType);
    }
}
