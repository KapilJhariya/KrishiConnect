package com.example.krishiconnect;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class OrderHistoryActivity extends AppCompatActivity {

    private LinearLayout orderContainer;
    private FirebaseFirestore db;
    private String uid;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        orderContainer = findViewById(R.id.orderContainer);
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadOrders();
    }

    private void loadOrders() {
        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("order")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderContainer.removeAllViews();
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyMsg = new TextView(this);
                        emptyMsg.setText("No orders found.");
                        orderContainer.addView(emptyMsg);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        View view = getLayoutInflater().inflate(R.layout.order_row, null);

                        String orderId = doc.getString("orderId");
                        String status = doc.getString("status") != null ? doc.getString("status") : "Processing";
                        Date orderDate = doc.getDate("orderDate");
                        String expectedDelivery = doc.getString("expectedDeliveryDate");
                        String paymentMethod = doc.getString("paymentMethod");
                        String shippingAddress = doc.getString("shippingAddress");
                        long totalAmount = doc.getLong("totalAmount") != null ? doc.getLong("totalAmount") : 0;
                        long totalItems = doc.getLong("totalItems") != null ? doc.getLong("totalItems") : 0;
                        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");

                        StringBuilder itemDetails = new StringBuilder();
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                String itemName = (String) item.get("itemName");
                                long price = item.get("price") != null ? ((Number) item.get("price")).longValue() : 0;
                                long quantity = item.get("quantity") != null ? ((Number) item.get("quantity")).longValue() : 0;
                                itemDetails.append("- ").append(itemName)
                                        .append(": ₹").append(price)
                                        .append(" x ").append(quantity)
                                        .append("\n");
                            }
                        }

                        ((TextView) view.findViewById(R.id.orderId)).setText("Order ID: " + orderId);
                        TextView statusTv = view.findViewById(R.id.orderStatus);
                        statusTv.setText("Status: " + status);
                        setStatusTextColor(statusTv, status);

                        ((TextView) view.findViewById(R.id.orderDate)).setText("Order Date: " + (orderDate != null ? dateFormat.format(orderDate) : "-"));
                        ((TextView) view.findViewById(R.id.deliveryDate)).setText("Expected Delivery: " + (expectedDelivery != null ? expectedDelivery : "-"));
                        ((TextView) view.findViewById(R.id.paymentMethod)).setText("Payment: " + (paymentMethod != null ? paymentMethod : "-"));
                        ((TextView) view.findViewById(R.id.shippingAddress)).setText("Address: " + (shippingAddress != null ? shippingAddress : "-"));
                        ((TextView) view.findViewById(R.id.orderAmount)).setText("Total: ₹" + totalAmount);
                        ((TextView) view.findViewById(R.id.totalItems)).setText("Items: " + totalItems);
                        ((TextView) view.findViewById(R.id.itemDetails)).setText(itemDetails.toString().trim());

                        View cancelButton = view.findViewById(R.id.cancelButton);
                        if (status.equals("Processing") || status.equals("Waiting")) {
                            cancelButton.setVisibility(View.VISIBLE);
                            cancelButton.setOnClickListener(v -> cancelOrder(orderId, items));
                        } else {
                            cancelButton.setVisibility(View.GONE);
                        }

                        orderContainer.addView(view);
                    }
                })
                .addOnFailureListener(e -> Log.w("FIREBASE", "Failed to load orders", e));
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

    private void cancelOrder(String orderId, List<Map<String, Object>> items) {
        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("order")
                .document(orderId)
                .update("status", "Cancelled")
                .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Order cancelled for buyer"))
                .addOnFailureListener(e -> Log.w("FIREBASE", "Failed to cancel order for buyer", e));

        if (items != null) {
            for (Map<String, Object> item : items) {
                String sellerId = (String) item.get("sellerId");
                if (sellerId != null) {
                    db.collection("Users")
                            .document("Seller")
                            .collection("users")
                            .document(sellerId)
                            .collection("order_received")
                            .document(orderId)
                            .update("status", "Cancelled")
                            .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Order cancelled for seller: " + sellerId))
                            .addOnFailureListener(e -> Log.w("FIREBASE", "Failed to cancel order for seller: " + sellerId, e));
                }
            }
        }

        Toast.makeText(this, "Order Cancelled", Toast.LENGTH_SHORT).show();
        loadOrders();
    }
}
