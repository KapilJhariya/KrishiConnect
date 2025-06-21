package com.example.krishiconnect;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class SellerItemsActivity extends AppCompatActivity {

    private LinearLayout itemContainer;
    private FirebaseFirestore db;
    private String sellerUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_items);

        itemContainer = findViewById(R.id.itemContainer);
        db = FirebaseFirestore.getInstance();
        sellerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadSellerItems();
    }

    private void loadSellerItems() {
        db.collection("Users")
                .document("Seller")
                .collection("users")
                .document(sellerUid)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        addItemCard(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load seller items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addItemCard(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String desc = doc.getString("description");
        Long price = doc.getLong("price");
        String imageUrl = doc.getString("imageUrl");

        View itemView = getLayoutInflater().inflate(R.layout.item_seller_card, itemContainer, false);
        ImageView img = itemView.findViewById(R.id.itemImage);
        TextView title = itemView.findViewById(R.id.itemTitle);
        TextView description = itemView.findViewById(R.id.itemDescription);
        TextView priceView = itemView.findViewById(R.id.itemPrice);
        Button editBtn = itemView.findViewById(R.id.editButton);
        Button deleteBtn = itemView.findViewById(R.id.deleteButton);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.loading)  // Show while loading
                    .error(R.drawable.loading)        // Show if failed
                    .into(img);
        } else {
            img.setImageResource(R.drawable.noimage);  // Default image if no URL
        }

        title.setText(name != null ? name : "N/A");
        description.setText(desc != null ? desc : "No description");
        priceView.setText("₹" + (price != null ? price : 0));

        deleteBtn.setOnClickListener(v -> confirmDelete(doc));
        editBtn.setOnClickListener(v -> showEditDialog(doc));

        itemContainer.addView(itemView);
    }

    private void confirmDelete(DocumentSnapshot doc) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String itemId = doc.getId();
                    String category = doc.getString("category");

                    db.collection("Users")
                            .document("Seller")
                            .collection("users")
                            .document(sellerUid)
                            .collection("items")
                            .document(itemId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                if (category != null) {
                                    db.collection(category)
                                            .document(itemId)
                                            .delete()
                                            .addOnSuccessListener(unused2 -> {
                                                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                                                loadSellerItems();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Deleted in seller, failed in main: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(this, "Deleted in seller, but category unknown", Toast.LENGTH_SHORT).show();
                                    loadSellerItems();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(DocumentSnapshot doc) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(0xFFFFFFFF);

        // Name
        TextView nameLabel = new TextView(this);
        nameLabel.setText("Item Name");
        nameLabel.setTextColor(0xFF000000);
        layout.addView(nameLabel);

        EditText nameInput = new EditText(this);
        nameInput.setTextColor(0xFF000000);
        nameInput.setHint("Name");
        nameInput.setText(doc.getString("name") != null ? doc.getString("name") : "");
        layout.addView(nameInput);

        // Price
        TextView priceLabel = new TextView(this);
        priceLabel.setText("Price");
        priceLabel.setTextColor(0xFF000000);
        layout.addView(priceLabel);

        EditText priceInput = new EditText(this);
        priceInput.setTextColor(0xFF000000);
        priceInput.setHint("Price");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        Long priceVal = doc.getLong("price");
        priceInput.setText(priceVal != null ? String.valueOf(priceVal) : "");
        layout.addView(priceInput);

        // Description
        TextView descLabel = new TextView(this);
        descLabel.setText("Description");
        descLabel.setTextColor(0xFF000000);
        layout.addView(descLabel);

        EditText descInput = new EditText(this);
        descInput.setTextColor(0xFF000000);
        descInput.setHint("Description");
        descInput.setText(doc.getString("description") != null ? doc.getString("description") : "");
        layout.addView(descInput);

        // Image URL
        TextView imageLabel = new TextView(this);
        imageLabel.setText("Image URL");
        imageLabel.setTextColor(0xFF000000);
        layout.addView(imageLabel);

        EditText imageInput = new EditText(this);
        imageInput.setTextColor(0xFF000000);
        imageInput.setHint("Image URL");
        imageInput.setText(doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "");
        layout.addView(imageInput);

        scrollView.addView(layout);

        new AlertDialog.Builder(this)
                .setTitle("Edit Item")
                .setView(scrollView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String desc = descInput.getText().toString().trim();
                    String priceStr = priceInput.getText().toString().trim();
                    String imageUrl = imageInput.getText().toString().trim();

                    if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long priceValue;
                    try {
                        priceValue = Long.parseLong(priceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String itemId = doc.getId();
                    String category = doc.getString("category");

                    db.collection("Users")
                            .document("Seller")
                            .collection("users")
                            .document(sellerUid)
                            .collection("items")
                            .document(itemId)
                            .update("name", name, "price", priceValue, "description", desc, "imageUrl", imageUrl)
                            .addOnSuccessListener(unused -> {
                                if (category != null) {
                                    db.collection(category)
                                            .document(itemId)
                                            .update("name", name, "price", priceValue, "description", desc, "imageUrl", imageUrl)
                                            .addOnSuccessListener(unused2 -> {
                                                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
                                                loadSellerItems();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Updated in seller, failed in main: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(this, "Updated in seller, but category unknown", Toast.LENGTH_SHORT).show();
                                    loadSellerItems();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
