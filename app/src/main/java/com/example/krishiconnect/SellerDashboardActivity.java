package com.example.krishiconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SellerDashboardActivity extends AppCompatActivity {

    private Button addGoodsBtn, manageOrdersBtn, logoutBtn;
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        // Initialize views
        addGoodsBtn = findViewById(R.id.addGoodsButton);
        manageOrdersBtn = findViewById(R.id.manageOrdersButton);
        logoutBtn = findViewById(R.id.logoutButton);
        welcomeText = findViewById(R.id.welcomeText);

        // Set click listeners
        addGoodsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SellerDashboardActivity.this, AddItemActivity.class);
            startActivity(intent);
        });

        Button manageItemsButton = findViewById(R.id.manageItemsButton);
        manageItemsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SellerItemsActivity.class));
        });


        manageOrdersBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SellerDashboardActivity.this, ManageOrdersActivity.class);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        });

        // Fetch and display username
        fetchAndDisplaySellerName();
    }

    private void fetchAndDisplaySellerName() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document("Seller")
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("name");
                        if (username != null && !username.isEmpty()) {
                            welcomeText.setText("Welcome, " + username);
                        } else {
                            welcomeText.setText("Welcome, Seller");
                        }
                    } else {
                        welcomeText.setText("Welcome, Seller");
                    }
                })
                .addOnFailureListener(e -> welcomeText.setText("Welcome, Seller"));
    }
}
