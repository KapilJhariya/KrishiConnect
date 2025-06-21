package com.example.krishiconnect;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ManageOrdersActivity extends AppCompatActivity {

    private LinearLayout orderContainer;
    private FirebaseFirestore db;
    private String sellerUid;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        orderContainer = findViewById(R.id.sellerOrderContainer);
        db = FirebaseFirestore.getInstance();
        sellerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadOrders();
    }

    private void loadOrders() {
        db.collection("Users")
                .document("Seller")
                .collection("users")
                .document(sellerUid)
                .collection("order_received")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderContainer.removeAllViews();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        addOrderView(doc);
                    }
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to load seller orders", e));
    }

    private void addOrderView(DocumentSnapshot doc) {
        View view = getLayoutInflater().inflate(R.layout.order_manage_row, null);

        String orderId = doc.getString("orderId");
        String buyerId = doc.getString("buyerId");
        String shippingAddress = doc.getString("shippingAddress");
        String paymentMethod = doc.getString("paymentMethod");
        String expectedDeliveryDate = doc.getString("expectedDeliveryDate");
        Date orderDate = doc.getDate("orderDate");
        String status = doc.getString("status");
        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");

        ((TextView) view.findViewById(R.id.manageOrderId)).setText("Order ID: " + orderId);
        ((TextView) view.findViewById(R.id.manageOrderDate)).setText("Date: " + (orderDate != null ? dateFormat.format(orderDate) : "-"));
        ((TextView) view.findViewById(R.id.manageAddress)).setText("Address: " + shippingAddress);
        ((TextView) view.findViewById(R.id.managePayment)).setText("Payment: " + paymentMethod);
        ((TextView) view.findViewById(R.id.manageExpectedDelivery)).setText("Expected: " + expectedDeliveryDate);

        TextView statusTv = view.findViewById(R.id.manageStatus);
        statusTv.setText("Status: " + status);
        setStatusTextColor(statusTv, status);

        StringBuilder itemDetails = new StringBuilder();
        if (items != null) {
            for (Map<String, Object> item : items) {
                itemDetails.append(item.get("itemName")).append(" x ")
                        .append(item.get("quantity")).append(" (₹")
                        .append(item.get("price")).append(")\n");
            }
        }
        ((TextView) view.findViewById(R.id.manageItems)).setText(itemDetails.toString().trim());

        Spinner statusSpinner = view.findViewById(R.id.statusSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                new String[]{"Processing", "Shipped", "Delivered", "Cancelled"});
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
        statusSpinner.setSelection(getStatusPosition(status));

        Button updateBtn = view.findViewById(R.id.updateStatusBtn);
        updateBtn.setOnClickListener(v -> {
            String newStatus = statusSpinner.getSelectedItem().toString();
            updateOrderStatus(orderId, buyerId, newStatus);
        });

        // Optional: add margin between orders
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16); // 16px bottom margin between orders
        view.setLayoutParams(params);

        orderContainer.addView(view);
    }

    private void setStatusTextColor(TextView statusTv, String status) {
        switch (status) {
            case "Cancelled":
                statusTv.setTextColor(Color.RED);
                break;
            case "Delivered":
                statusTv.setTextColor(Color.parseColor("#4CAF50")); // green
                break;
            case "Shipped":
                statusTv.setTextColor(Color.parseColor("#2196F3")); // blue
                break;
            case "Processing":
                statusTv.setTextColor(Color.parseColor("#FFEB3B")); // yellow
                break;
            default:
                statusTv.setTextColor(Color.BLACK);
                break;
        }
    }

    private int getStatusPosition(String status) {
        switch (status) {
            case "Shipped": return 1;
            case "Delivered": return 2;
            case "Cancelled": return 3;
            default: return 0;
        }
    }

    private void updateOrderStatus(String orderId, String buyerId, String newStatus) {
        DocumentReference sellerOrderRef = db.collection("Users")
                .document("Seller")
                .collection("users")
                .document(sellerUid)
                .collection("order_received")
                .document(orderId);

        DocumentReference buyerOrderRef = db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(buyerId)
                .collection("order")
                .document(orderId);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("status", newStatus);

        sellerOrderRef.update(updateMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE", "Seller order status updated");
                    reloadOrdersAfterUpdate(newStatus);
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to update seller order", e));

        buyerOrderRef.update(updateMap)
                .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Buyer order status updated"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to update buyer order", e));
    }

    private void reloadOrdersAfterUpdate(String newStatus) {
        Toast.makeText(this, "Order status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
        loadOrders();  // Refresh the order list
    }
}
