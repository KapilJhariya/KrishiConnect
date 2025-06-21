package com.example.krishiconnect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText emailEt, passwordEt;
    private Button continueBtn, phoneBtn, googleBtn, microsoftBtn, appleBtn;
    private TextView signUpLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEt = findViewById(R.id.emailInput);
        passwordEt = findViewById(R.id.passwordInput);  // Ensure this exists in your XML
        continueBtn = findViewById(R.id.continueButton);
        signUpLink = findViewById(R.id.signUpLink);
        phoneBtn = findViewById(R.id.phoneButton);
        googleBtn = findViewById(R.id.googleButton);
        microsoftBtn = findViewById(R.id.microsoftButton);
        appleBtn = findViewById(R.id.appleButton);

        continueBtn.setOnClickListener(v -> loginUser());
        signUpLink.setOnClickListener(v -> startActivity(new Intent(this, Register.class)));

        phoneBtn.setOnClickListener(v -> Toast.makeText(this, "Phone login not implemented yet", Toast.LENGTH_SHORT).show());
        googleBtn.setOnClickListener(v -> Toast.makeText(this, "Google login not implemented yet", Toast.LENGTH_SHORT).show());
        microsoftBtn.setOnClickListener(v -> Toast.makeText(this, "Microsoft login not implemented yet", Toast.LENGTH_SHORT).show());
        appleBtn.setOnClickListener(v -> Toast.makeText(this, "Apple login not implemented yet", Toast.LENGTH_SHORT).show());

        // 🚀 Check if already logged in
        if (mAuth.getCurrentUser() != null) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Checking user type...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            String uid = mAuth.getCurrentUser().getUid();
            checkUserType(uid, progressDialog);
        }
    }

    private void loginUser() {
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = mAuth.getCurrentUser().getUid();
                    checkUserType(uid, progressDialog);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUserType(String uid, ProgressDialog progressDialog) {
        db.collection("Users").document("Buyer").collection("users").document(uid)
                .get()
                .addOnSuccessListener(buyerDoc -> {
                    if (buyerDoc.exists()) {
                        progressDialog.dismiss();
                        goToBuyer(buyerDoc);
                    } else {
                        db.collection("Users").document("Seller").collection("users").document(uid)
                                .get()
                                .addOnSuccessListener(sellerDoc -> {
                                    progressDialog.dismiss();
                                    if (sellerDoc.exists()) {
                                        goToSeller(sellerDoc);
                                    } else {
                                        Toast.makeText(this, "User type not found. Please contact support.", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();  // Log out invalid session
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "Error fetching seller data", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error fetching buyer data", Toast.LENGTH_SHORT).show();
                });
    }

    private void goToBuyer(DocumentSnapshot doc) {
        Toast.makeText(this, "Welcome Buyer!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, test.class));  // Replace `test.class` with your actual Buyer activity if needed
        finish();
    }

    private void goToSeller(DocumentSnapshot doc) {
        Toast.makeText(this, "Welcome Seller!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, SellerDashboardActivity.class));
        finish();
    }
}
