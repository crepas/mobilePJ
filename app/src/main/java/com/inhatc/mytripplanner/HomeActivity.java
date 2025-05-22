package com.inhatc.mytripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView textViewWelcome;
    private Button buttonLogout;
    private Button buttonNewTrip;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        initViews();
        setWelcomeMessage();
        setClickListeners();
    }

    private void initViews() {
        textViewWelcome = findViewById(R.id.textViewWelcome);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonNewTrip = findViewById(R.id.buttonNewTrip);
    }

    private void setWelcomeMessage() {
        String userEmail = sharedPreferences.getString("userEmail", "사용자");
        textViewWelcome.setText(userEmail + "님, 환영합니다!");
    }

    private void setClickListeners() {
        // 로그아웃 버튼
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        // 새 여행 계획 버튼
        buttonNewTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 여행 계획 화면으로 이동 (나중에 구현)
                // Intent intent = new Intent(HomeActivity.this, TripPlanActivity.class);
                // startActivity(intent);
            }
        });
    }

    private void logout() {
        // 로그인 상태 삭제
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("userEmail");
        editor.apply();

        // 로그인 화면으로 이동
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}