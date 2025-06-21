package com.example.krishiconnect;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid;

    private TextView itemCountView, orderTotalView;
    private LinearLayout itemsContainer;
    private Spinner paymentSpinner;
    private EditText shippingDateInput, shippingAddressInput;

    private int totalAmount = 0;
    private int totalItems = 0;
    private List<Map<String, Object>> itemsList = new ArrayList<>();

    private Calendar selectedShippingDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        itemCountView = findViewById(R.id.itemCount);
        orderTotalView = findViewById(R.id.orderTotal);
        itemsContainer = findViewById(R.id.itemsContainer);
        paymentSpinner = findViewById(R.id.paymentSpinner);
        shippingDateInput = findViewById(R.id.shippingDateInput);
        shippingAddressInput = findViewById(R.id.shippingAddress);

        setupPaymentSpinner();
        setupDatePicker();
        setupButtons();

        loadCartAndPopulateSummary();
    }

    private void setupPaymentSpinner() {
        String[] paymentMethods = {"Credit Card", "PayPal", "Bank Transfer", "Cash on Delivery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, paymentMethods);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        paymentSpinner.setAdapter(adapter);
    }

    private void setupDatePicker() {
        shippingDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedShippingDate = Calendar.getInstance();
                        selectedShippingDate.set(year, month, day);
                        shippingDateInput.setText(String.format(Locale.getDefault(), "%02d-%02d-%d", day, month + 1, year));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show();
        });
    }

    private void setupButtons() {
        findViewById(R.id.completeButton).setOnClickListener(v -> {
            String address = shippingAddressInput.getText().toString().trim();
            String dateStr = shippingDateInput.getText().toString().trim();

            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter shipping address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dateStr.isEmpty()) {
                Toast.makeText(this, "Please select shipping date", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isShippingDateValid(selectedShippingDate)) {
                Toast.makeText(this, "Shipping date must be at least 7 days from today", Toast.LENGTH_SHORT).show();
                return;
            }

            saveOrderAndClearCart(paymentSpinner.getSelectedItem().toString(), dateStr);
        });

        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
    }

    private boolean isShippingDateValid(Calendar selectedDate) {
        if (selectedDate == null) return false;
        Calendar todayPlus7 = Calendar.getInstance();
        todayPlus7.add(Calendar.DAY_OF_MONTH, 7);
        return selectedDate.after(todayPlus7);
    }

    private void loadCartAndPopulateSummary() {
        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(cartSnapshot -> {
                    if (cartSnapshot.isEmpty()) {
                        Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    totalAmount = 0;
                    totalItems = 0;
                    itemsList.clear();
                    itemsContainer.removeAllViews();

                    Map<String, Map<String, Object>> itemMap = new HashMap<>();

                    for (DocumentSnapshot doc : cartSnapshot) {
                        String itemName = doc.getString("item_name");
                        String sellerId = doc.getString("seller_id");
                        long price = parsePrice(doc.getString("item_price"));

                        if (!itemMap.containsKey(itemName)) {
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("itemName", itemName);
                            itemData.put("price", price);
                            itemData.put("quantity", 1L);
                            itemData.put("sellerId", sellerId);
                            itemMap.put(itemName, itemData);
                        } else {
                            Map<String, Object> existing = itemMap.get(itemName);
                            long currentQty = (long) existing.get("quantity");
                            existing.put("quantity", currentQty + 1);
                        }
                    }


                    for (Map<String, Object> item : itemMap.values()) {
                        String itemName = (String) item.get("itemName");
                        long price = (long) item.get("price");
                        long quantity = (long) item.get("quantity");
                        long itemTotal = price * quantity;

                        totalAmount += itemTotal;
                        totalItems += quantity;

                        itemsList.add(item);

                        // Add to UI
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));

                        TextView nameView = new TextView(this);
                        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                        nameView.setText(itemName + " x" + quantity);
                        nameView.setTextColor(0xFF000000);
                        nameView.setTextSize(14);

                        TextView priceView = new TextView(this);
                        priceView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        priceView.setText("₹" + itemTotal);
                        priceView.setTextColor(0xFF000000);
                        priceView.setTextSize(14);

                        row.addView(nameView);
                        row.addView(priceView);
                        itemsContainer.addView(row);
                    }

                    totalAmount+=7;
                    itemCountView.setText("Total Items: " + totalItems);
                    orderTotalView.setText("Total Amount: ₹" + totalAmount);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load cart: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void addItemRowToSummary(String name, long quantity, long itemTotal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView nameView = new TextView(this);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        nameView.setText(name + " x" + quantity);
        nameView.setTextColor(0xFF000000);
        nameView.setTextSize(14);

        TextView priceView = new TextView(this);
        priceView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        priceView.setText("₹" + itemTotal);
        priceView.setTextColor(0xFF000000);
        priceView.setTextSize(14);

        row.addView(nameView);
        row.addView(priceView);
        itemsContainer.addView(row);
    }

    private void saveOrderAndClearCart(String paymentMethod, String shippingDate) {
        String orderId = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String shippingAddress = shippingAddressInput.getText().toString();

        Map<String, List<Map<String, Object>>> sellerItemMap = new HashMap<>();
        for (Map<String, Object> item : itemsList) {
            String sellerId = (String) item.get("sellerId");
            sellerItemMap.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("totalAmount", totalAmount);
        orderData.put("totalItems", totalItems);
        orderData.put("paymentMethod", paymentMethod);
        orderData.put("shippingAddress", shippingAddress);
        orderData.put("expectedDeliveryDate", shippingDate);
        orderData.put("orderDate", FieldValue.serverTimestamp());
        orderData.put("items", itemsList);
        orderData.put("status", "Waiting");

        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("order")
                .document(orderId)
                .set(orderData);

        for (Map.Entry<String, List<Map<String, Object>>> entry : sellerItemMap.entrySet()) {
            Map<String, Object> sellerOrderData = new HashMap<>();
            sellerOrderData.put("orderId", orderId);
            sellerOrderData.put("buyerId", uid);
            sellerOrderData.put("items", entry.getValue());
            sellerOrderData.put("shippingAddress", shippingAddress);
            sellerOrderData.put("paymentMethod", paymentMethod);
            sellerOrderData.put("expectedDeliveryDate", shippingDate);
            sellerOrderData.put("orderDate", FieldValue.serverTimestamp());
            sellerOrderData.put("status", "Waiting");

            db.collection("Users")
                    .document("Seller")
                    .collection("users")
                    .document(entry.getKey())
                    .collection("order_received")
                    .document(orderId)
                    .set(sellerOrderData);
        }

        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(cartSnapshot -> {
                    for (DocumentSnapshot doc : cartSnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });

        Intent intent = new Intent(this, OrderSummaryActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("totalItems", totalItems);
        intent.putExtra("totalAmount", totalAmount);
        intent.putExtra("paymentMethod", paymentMethod);
        intent.putExtra("shippingAddress", shippingAddress);
        intent.putExtra("shipping_date", shippingDate);
        startActivity(intent);
    }

    private long parsePrice(String priceStr) {
        if (priceStr == null || priceStr.trim().isEmpty()) return 0L;
        priceStr = priceStr.replaceAll("[^0-9]", "");
        try {
            return priceStr.isEmpty() ? 0L : Long.parseLong(priceStr);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long parseLongSafe(Object obj) {
        if (obj == null) return 0L;
        try {
            if (obj instanceof Number) {
                return ((Number) obj).longValue();
            } else if (obj instanceof String) {
                return Long.parseLong(((String) obj).trim());
            }
        } catch (Exception ignored) {}
        return 0L;
    }
}
