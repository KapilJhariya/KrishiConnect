package com.example.krishiconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Register extends AppCompatActivity {

    private EditText nameEt, surnameEt, emailEt, passwordEt, retypePasswordEt;
    private Spinner userTypeSpinner;
    private Button registerBtn, goToLoginBtn, checkEmailVerifiedBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final List<String> userTypes = Arrays.asList("Select User Type", "Buyer", "Seller");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEt = findViewById(R.id.register_name);
        surnameEt = findViewById(R.id.register_surname);
        emailEt = findViewById(R.id.register_email);
        passwordEt = findViewById(R.id.register_password);
        retypePasswordEt = findViewById(R.id.register_retype_password);
        userTypeSpinner = findViewById(R.id.user_type_spinner);
        registerBtn = findViewById(R.id.register_button);
        goToLoginBtn = findViewById(R.id.goto_login);
        checkEmailVerifiedBtn = findViewById(R.id.check_email_verified_button);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up spinner using hard-coded types
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                userTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);

        registerBtn.setOnClickListener(v -> registerUser());
        goToLoginBtn.setOnClickListener(v -> startActivity(new Intent(this, WelcomeActivity.class)));
        checkEmailVerifiedBtn.setOnClickListener(v -> proceedIfEmailVerified());
    }

    private void registerUser() {
        String name = nameEt.getText().toString().trim();
        String surname = surnameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString();
        String rePassword = retypePasswordEt.getText().toString();
        String userType = userTypeSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname)) {
            Toast.makeText(this, "Name & Surname required", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Select User Type".equals(userType)) {
            Toast.makeText(this, "Please select Buyer or Seller", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(rePassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Verification email sent! Check your inbox & spam folder.", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void proceedIfEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in. Please register or login first.", Toast.LENGTH_LONG).show();
            return;
        }

        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    saveUserToFirestore();
                } else {
                    Toast.makeText(this, "Please verify your email before proceeding.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Failed to reload user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToFirestore() {
        String uid = mAuth.getCurrentUser().getUid();
        String name = nameEt.getText().toString().trim();
        String surname = surnameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String userType = userTypeSpinner.getSelectedItem().toString();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("surname", surname);
        userMap.put("email", email);
        userMap.put("userType", userType);
        userMap.put("method", "email");

        db.collection("Users")
                .document(userType)
                .collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registered successfully as " + userType + "!", Toast.LENGTH_SHORT).show();
                    if(userType.equals("Buyer")){
                        startActivity(new Intent(this, test.class));
                    }else{
                        startActivity(new Intent(this, SellerDashboardActivity.class));
                    }

                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
