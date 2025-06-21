package com.example.krishiconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    LinearLayout cartContainer;
    TextView subtotalText, totalText;
    int shippingCost = 7;
    int subtotal = 0;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartContainer = findViewById(R.id.cartContainer);
        subtotalText = findViewById(R.id.subtotalText);
        totalText = findViewById(R.id.totalText);
        Button checkoutButton = findViewById(R.id.checkoutButton);
        Button continueButton = findViewById(R.id.continueButton);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadCartItems();

        checkoutButton.setOnClickListener(v -> proceedToCheckout());
        continueButton.setOnClickListener(v -> finish());
    }

    private void loadCartItems() {
        cartContainer.removeAllViews();
        subtotal = 0;

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        subtotalText.setText("Subtotal: ₹0");
                        totalText.setText("Total: ₹0");
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("item_name");

                        String priceRaw = doc.getString("item_price");
                        int price = parsePrice(priceRaw);
                        int quantity = parseIntSafe(doc.get("quantity"), 1);

                        int itemTotal = price * quantity;
                        subtotal += itemTotal;

                        View itemView = getLayoutInflater().inflate(R.layout.cart_item_row, null);
                        TextView nameText = itemView.findViewById(R.id.cartItemName);
                        TextView priceText = itemView.findViewById(R.id.cartItemPrice);
                        Button deleteBtn = itemView.findViewById(R.id.deleteItemButton);

                        nameText.setText(name + " x" + quantity);
                        priceText.setText("₹" + itemTotal);

                        deleteBtn.setOnClickListener(v -> deleteCartItem(uid, id));

                        cartContainer.addView(itemView);
                    }

                    subtotalText.setText("Subtotal: ₹" + subtotal);
                    totalText.setText("Total: ₹" + (subtotal + shippingCost));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading cart: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return 0;
        // Remove ₹ symbol and non-digit characters
        priceStr = priceStr.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseIntSafe(Object obj, int defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    private void deleteCartItem(String uid, String docId) {
        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("cart")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show();
                    loadCartItems();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error deleting item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void proceedToCheckout() {
        if (cartContainer.getChildCount() == 0) {
            Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> cartItems = new ArrayList<>();

        for (int i = 0; i < cartContainer.getChildCount(); i++) {
            View itemView = cartContainer.getChildAt(i);
            TextView nameText = itemView.findViewById(R.id.cartItemName);
            TextView priceText = itemView.findViewById(R.id.cartItemPrice);

            String name = nameText.getText().toString();
            String price = priceText.getText().toString();
            cartItems.add(name + " - " + price);
        }

        Intent intent = new Intent(CartActivity.this, DetailsActivity.class);
        intent.putExtra("total_amount", subtotal + shippingCost);
        intent.putExtra("total_items", cartContainer.getChildCount());
        intent.putStringArrayListExtra("cart_items", cartItems);
        startActivity(intent);
    }
}
