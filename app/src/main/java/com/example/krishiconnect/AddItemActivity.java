package com.example.krishiconnect;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    private EditText nameInput, descInput, priceInput, imageUrlInput;
    private Spinner categorySpinner;
    private Button addButton;
    private FirebaseFirestore db;
    private String uid, sellerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        nameInput = findViewById(R.id.itemNameInput);
        descInput = findViewById(R.id.itemDescInput);
        priceInput = findViewById(R.id.itemPriceInput);
        imageUrlInput = findViewById(R.id.itemImageUrlInput);
        categorySpinner = findViewById(R.id.itemCategorySpinner);
        addButton = findViewById(R.id.addItemButton);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sellerName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (sellerName == null || sellerName.isEmpty()) {
            sellerName = "Unknown Seller";
        }

        String[] categories = {"Goods", "Machine"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        addButton.setOnClickListener(v -> addItem());
    }

    private void addItem() {
        String name = nameInput.getText().toString().trim();
        String desc = descInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String imageUrl = imageUrlInput.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || imageUrl.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long price;
        try {
            price = Long.parseLong(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("description", desc);
        item.put("price", price);
        item.put("imageUrl", imageUrl);
        item.put("sellerId", uid);
        item.put("sellerName", sellerName);
        item.put("category", category); // ⚡️ Add category for easy tracking
        item.put("timestamp", FieldValue.serverTimestamp());

        db.collection(category)  // main category collection
                .add(item)
                .addOnSuccessListener(docRef -> {
                    db.collection("Users")
                            .document("Seller")
                            .collection("users")
                            .document(uid)
                            .collection("items")
                            .document(docRef.getId())
                            .set(item)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                                clearFields();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Added to category, failed in seller: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    private void clearFields() {
        nameInput.setText("");
        descInput.setText("");
        priceInput.setText("");
        imageUrlInput.setText("");
        categorySpinner.setSelection(0);
    }
}
