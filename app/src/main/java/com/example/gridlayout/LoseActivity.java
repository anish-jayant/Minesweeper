package com.example.gridlayout;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("YOU LOSE");
        tv.setTextSize(32);
        tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        setContentView(tv);
    }
}
