package com.example.krishiconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class launcher extends AppCompatActivity {

    TextView viewPrices, shopNow, rentEquipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        viewPrices = findViewById(R.id.viewPrices);
        shopNow = findViewById(R.id.shopNow);
        rentEquipment = findViewById(R.id.rentEquipment);

//        viewPrices.setOnClickListener(v ->
//                startActivity(new Intent(this, CropPriceActivity.class)));




        Intent intent = new Intent(this, MainActivity.class);


        shopNow.setOnClickListener(v ->
                intent.putExtra("type", "Goods"));
        startActivity(intent);

        rentEquipment.setOnClickListener(v ->
                intent.putExtra("type", "machine"));
        startActivity(intent);
    }
}
