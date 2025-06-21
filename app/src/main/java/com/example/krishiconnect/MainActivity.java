package com.example.krishiconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class MainActivity extends AppCompatActivity {

    CartDatabaseHelper dbHelper;
    Button viewCartButton;
    LinearLayout container;
    TextView title, subtitle;
    FirebaseFirestore db;
    FirebaseAuth auth;
    String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button historyBtn = findViewById(R.id.button3);
        historyBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, OrderHistoryActivity.class)));

        Button homeBtn = findViewById(R.id.homeButton);
        homeBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, test.class)));

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FIREBASE", "Signed in anonymously");
                        } else {
                            Log.e("FIREBASE", "Anonymous sign-in failed", task.getException());
                        }
                    });
        }

        dbHelper = new CartDatabaseHelper(this);
        viewCartButton = findViewById(R.id.viewCartButton);
        viewCartButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CartActivity.class)));

        container = findViewById(R.id.container);
        db = FirebaseFirestore.getInstance();
        type = getIntent().getStringExtra("type");

        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        if ("machine".equals(type)) {
            title.setText("Equipment Rental");
            subtitle.setText("Rent machinery and labor for your farming operations");
        } else {
            title.setText("Farming Supplies");
            subtitle.setText("Purchase high-quality seeds, fertilizers, tools, and more");
        }

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        container.removeAllViews();
        loadItems();
    }

    private void loadItems() {
        db.collection(type)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    container.removeAllViews();  // Ensure clear before adding (double safety)
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        String description = document.getString("description");
                        Long price = document.getLong("price");
                        String imageUrl = document.getString("imageUrl");
                        String sellerId = document.getString("sellerId");

                        String priceText = "₹" + price + ("machine".equals(type) ? "/day" : "");
                        addItem(name, description, priceText, imageUrl, type, sellerId);
                    }
                });
    }


    private void addItem(String name, String description, String priceText, String imageUrl, String type, String sellerId) {
        View itemView = getLayoutInflater().inflate(R.layout.item_card, container, false);  // IMPORTANT: attachToRoot=false

        ImageView image = itemView.findViewById(R.id.itemImage);
        TextView itemTitle = itemView.findViewById(R.id.itemTitle);
        TextView itemDescription = itemView.findViewById(R.id.itemDescription);
        TextView itemPrice = itemView.findViewById(R.id.itemPrice);
        Button reserveButton = itemView.findViewById(R.id.reserveButton);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.loading)
                .error(R.drawable.noimage)
                .centerInside()
                .into(image);

        itemTitle.setText(name);
        itemDescription.setText(description != null ? description : "No description");
        itemPrice.setText(priceText);

        LinearLayout parentLayout = (LinearLayout) reserveButton.getParent();
        int index = parentLayout.indexOfChild(reserveButton);

        dbHelper.getItemCount(name, quantity -> {
            if (quantity > 0) {
                parentLayout.removeView(reserveButton);
                parentLayout.addView(createQuantityLayout(name, priceText, imageUrl, type, sellerId, reserveButton, quantity), index);
            } else {
                reserveButton.setOnClickListener(v -> {
                    dbHelper.addItem(name, priceText, imageUrl, type, sellerId);
                    refreshQuantityView(name, parentLayout, reserveButton, priceText, imageUrl, type, sellerId, index);
                });
            }
        });

        // Add the whole item view just ONCE
        container.addView(itemView);
    }


    private LinearLayout createQuantityLayout(String name, String priceText, String imageUrl, String type, String sellerId, Button reserveButtonTemplate, int quantity) {
        LinearLayout quantityLayout = new LinearLayout(this);
        quantityLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button minusBtn = new Button(this);
        minusBtn.setText("-");
        TextView qtyText = new TextView(this);
        qtyText.setText(String.valueOf(quantity));
        qtyText.setTextSize(18);
        qtyText.setTextColor(getResources().getColor(android.R.color.black));
        qtyText.setPadding(20, 0, 20, 0);
        Button plusBtn = new Button(this);
        plusBtn.setText("+");

        quantityLayout.addView(minusBtn);
        quantityLayout.addView(qtyText);
        quantityLayout.addView(plusBtn);

        plusBtn.setOnClickListener(v -> {
            dbHelper.addItem(name, priceText, imageUrl, type, sellerId);
            updateQtyText(name, qtyText);
        });

        minusBtn.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(qtyText.getText().toString());
            int newQty = currentQty - 1;

            if (newQty > 0) {
                qtyText.setText(String.valueOf(newQty));
            } else {
                LinearLayout parent = (LinearLayout) quantityLayout.getParent();
                parent.removeView(quantityLayout);
                parent.addView(reserveButtonTemplate);

                // ✅ Re-attach the click listener for reserveButtonTemplate
                reserveButtonTemplate.setOnClickListener(reserveClick -> {
                    dbHelper.addItem(name, priceText, imageUrl, type, sellerId);
                    parent.removeView(reserveButtonTemplate);
                    parent.addView(createQuantityLayout(name, priceText, imageUrl, type, sellerId, reserveButtonTemplate, 1));
                });
            }

            dbHelper.removeItem(name);
        });




        return quantityLayout;
    }

    private void updateQtyText(String name, TextView qtyText) {
        dbHelper.getItemCount(name, qty -> qtyText.setText(String.valueOf(qty)));
    }

    private void refreshQuantityView(String name, LinearLayout parentLayout, Button reserveButton, String priceText, String imageUrl, String type, String sellerId, int index) {
        dbHelper.getItemCount(name, qty -> {
            parentLayout.removeView(reserveButton);
            parentLayout.addView(createQuantityLayout(name, priceText, imageUrl, type, sellerId, reserveButton, qty), index);
        });
    }
}
