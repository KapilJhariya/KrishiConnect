package com.example.krishiconnect;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderSummaryActivity extends AppCompatActivity {

    private TextView orderIdText, buyerNameText, sellerNameText, itemSummary, amountSummary, paymentSummary,
            addressSummary, orderDateText, shippingText;
    private LinearLayout itemListContainer;
    private Button printReceipt;

    private FirebaseFirestore db;
    private String orderId;

    private DocumentSnapshot currentOrderDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        db = FirebaseFirestore.getInstance();

        orderIdText = findViewById(R.id.orderIdText);
        buyerNameText = findViewById(R.id.buyerNameText);
        sellerNameText = findViewById(R.id.sellerNameText);
        itemSummary = findViewById(R.id.itemSummary);
        amountSummary = findViewById(R.id.amountSummary);
        paymentSummary = findViewById(R.id.paymentSummary);
        addressSummary = findViewById(R.id.addressSummary);
        orderDateText = findViewById(R.id.orderDateText);
        shippingText = findViewById(R.id.shippingText);
        itemListContainer = findViewById(R.id.itemListContainer);

        orderId = getIntent().getStringExtra("orderId");
        orderIdText.setText("Order ID: " + orderId);

        fetchBuyerNameFromFirestore();
        fetchOrderDetails();

        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrderSummaryActivity.this, test.class);
            startActivity(intent);
            finish();
        });
    }

    private void fetchBuyerNameFromFirestore() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            buyerNameText.setText("Buyer: N/A");
            return;
        }

        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        String name = userSnapshot.getString("name");
                        buyerNameText.setText("Buyer: " + (name != null ? name : "N/A"));
                    } else {
                        buyerNameText.setText("Buyer: N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    buyerNameText.setText("Buyer: N/A");
                    Toast.makeText(this, "Failed to fetch buyer name", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchOrderDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users")
                .document("Buyer")
                .collection("users")
                .document(uid)
                .collection("order")
                .document(orderId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentOrderDoc = snapshot;
                        populateOrderDetails(snapshot);
                    } else {
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load order", Toast.LENGTH_SHORT).show());
    }

    private void populateOrderDetails(DocumentSnapshot doc) {
        addressSummary.setText("Delivery Address: " + doc.getString("shippingAddress"));
        paymentSummary.setText("Payment Mode: " + doc.getString("paymentMethod"));
        shippingText.setText("Expected Delivery: " + doc.getString("expectedDeliveryDate"));
        amountSummary.setText("Amount Paid: ₹" + doc.getLong("totalAmount"));
        itemSummary.setText("Total Items: " + doc.getLong("totalItems"));

        if (doc.contains("orderDate")) {
            orderDateText.setText("Order Date: " + doc.getTimestamp("orderDate").toDate().toString());
        }

        itemListContainer.removeAllViews();
        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                String itemName = (String) item.get("itemName");
                long qty = (long) item.get("quantity");
                long price = (long) item.get("price");
                long total = qty * price;

                TextView itemRow = new TextView(this);
                itemRow.setText(String.format(Locale.getDefault(), "%s x%d - ₹%d", itemName, qty, total));
                itemRow.setTextColor(0xFF000000);
                itemRow.setTextSize(14);
                itemListContainer.addView(itemRow);

                if (item.containsKey("sellerId")) {
                    String sellerId = (String) item.get("sellerId");
                    sellerNameText.setText("Seller: " + sellerId);
                }
            }
        }
    }

    private void generatePDF() {
        if (currentOrderDoc == null) {
            Toast.makeText(this, "Order details not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int y = 50;

        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.agrilabour);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 100, 100, false);
        canvas.drawBitmap(scaledLogo, (pageInfo.getPageWidth() - scaledLogo.getWidth()) / 2, y, paint);
        y += 120;

        paint.setTextSize(16);
        paint.setColor(0xFF000000);

        canvas.drawText(orderIdText.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(buyerNameText.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(sellerNameText.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(orderDateText.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(shippingText.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(addressSummary.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(paymentSummary.getText().toString(), 50, y, paint);
        y += 25;
        canvas.drawText(amountSummary.getText().toString(), 50, y, paint);
        y += 25;

        canvas.drawText("Items:", 50, y, paint);
        y += 20;

        List<Map<String, Object>> items = (List<Map<String, Object>>) currentOrderDoc.get("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                String itemName = (String) item.get("itemName");
                long qty = (long) item.get("quantity");
                long price = (long) item.get("price");
                long total = qty * price;

                String itemLine = String.format(Locale.getDefault(), "%s x%d - ₹%d", itemName, qty, total);
                canvas.drawText(itemLine, 70, y, paint);
                y += 20;

                if (y > pageInfo.getPageHeight() - 50) {
                    pdf.finishPage(page);
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }
            }
        }

        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        canvas.drawText("Thank You!", (pageInfo.getPageWidth() / 2) - 60, pageInfo.getPageHeight() - 50, paint);

        pdf.finishPage(page);

        try {
            File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OrderReceipt_" + orderId + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this, "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
