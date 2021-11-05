package com.example.usbong;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class InformationActivity extends AppCompatActivity {
    Button clay;
    Button peaty;
    Button loam;
    Button sandy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);


        clay = findViewById(R.id.clay);
        peaty = findViewById(R.id.peaty);
        loam = findViewById(R.id.loam);
        sandy = findViewById(R.id.sandy);

        clay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InformationActivity.this, ClayActivity.class);
                startActivity(intent);
            }
        });
        peaty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InformationActivity.this, PeatyActivity.class);
                startActivity(intent);
            }
        });
        loam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InformationActivity.this, LoamActivity.class);
                startActivity(intent);
            }
        });
        sandy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InformationActivity.this, SandyActivity.class);
                startActivity(intent);
            }
        });
    }
}