package com.inhatc.mytripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewSignUp;

    // 간단한 사용자 정보 저장용
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // 이미 로그인된 사용자인지 확인
        if (isUserLoggedIn()) {
            // 이미 로그인되어 있으면 메인 화면으로 이동
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // UI 요소 연결
        initViews();

        // 버튼 클릭 이벤트 설정
        setClickListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.textViewSignUp);
    }

    private void setClickListeners() {
        // 로그인 버튼 클릭
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        // 회원가입 링크 클릭
        textViewSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 회원가입 화면으로 이동
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 입력 검증
        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "올바른 이메일 형식이 아닙니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로그인 처리 (간단한 방식)
        if (validateUser(email, password)) {
            // 로그인 성공
            saveLoginState(email);
            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show();

            // 메인 화면으로 이동
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            // 로그인 실패
            Toast.makeText(this, "이메일 또는 비밀번호가 잘못되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 이메일 형식 검증
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    // 로그인 검증
    private boolean validateUser(String email, String password) {
        String savedPassword = sharedPreferences.getString(email, null);
        return savedPassword != null && savedPassword.equals(password);
    }

    // 로그인 상태 저장
    private void saveLoginState(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userEmail", email);
        editor.apply();
    }

    // 로그인 상태 확인
    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }
}