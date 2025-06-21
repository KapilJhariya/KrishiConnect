package com.example.krishiconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class test extends AppCompatActivity {

    private TextView viewPrices, shopNow, rentEquipment, welcomeText, logoutText;
    private Button viewCartButton, myOrdersButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        shopNow = findViewById(R.id.shopNow);
        rentEquipment = findViewById(R.id.rentEquipment);
//        viewPrices = findViewById(R.id.viewPrices);
        welcomeText = findViewById(R.id.welcomeText);
        logoutText = findViewById(R.id.logoutText);
        viewCartButton = findViewById(R.id.viewCartButton);
        myOrdersButton = findViewById(R.id.myOrdersButton);

        // View Cart button click
        viewCartButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CartActivity.class);
            startActivity(intent);
        });

        // My Orders button click
        myOrdersButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderHistoryActivity.class);
            startActivity(intent);
        });

        // Logout text click
        logoutText.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Shop Now click
        shopNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("type", "Goods");
            startActivity(intent);
        });

        // Rent Equipment click
        rentEquipment.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("type", "machine");
            startActivity(intent);
        });

//        // View Prices click
//        viewPrices.setOnClickListener(v -> {
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.putExtra("type", "price");
//            startActivity(intent);
//        });

        // Fetch and display user's name
        fetchUserName();
    }

    private void fetchUserName() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            welcomeText.setText("Hello, User!");
            return;
        }

        db.collection("Users").document("Buyer").collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getString("name") != null) {
                        String name = document.getString("name");
                        welcomeText.setText("Hello, " + name + "!");
                    } else {
                        db.collection("Users").document("Seller").collection("users").document(uid)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists() && doc.getString("name") != null) {
                                        String name = doc.getString("name");
                                        welcomeText.setText("Hello, " + name + "!");
                                    } else {
                                        welcomeText.setText("Hello, User!");
                                        Toast.makeText(this, "User type not found.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    welcomeText.setText("Hello, User!");
                                    Toast.makeText(this, "Error fetching seller data.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    welcomeText.setText("Hello, User!");
                    Toast.makeText(this, "Error fetching buyer data.", Toast.LENGTH_SHORT).show();
                });
    }
}
