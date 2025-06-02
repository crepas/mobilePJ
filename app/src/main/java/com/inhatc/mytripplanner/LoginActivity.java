package com.inhatc.mytripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewSignUp;
    private ProgressBar progressBar;

    // Firebase Realtime Database
    private DatabaseReference database;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Database 초기화
        database = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // 이미 로그인된 사용자인지 확인
        if (isUserLoggedIn()) {
            // 네비게이션 메인 화면으로 이동
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // UI 요소 연결
        initViews();
        setClickListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.textViewSignUp);
        progressBar = findViewById(R.id.progressBar);
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
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 입력 검증
        if (email.isEmpty()) {
            editTextEmail.setError("이메일을 입력하세요");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("비밀번호를 입력하세요");
            editTextPassword.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            editTextEmail.setError("올바른 이메일 형식이 아닙니다");
            editTextEmail.requestFocus();
            return;
        }

        // 로딩 표시
        showLoading(true);

        // Firebase에서 이메일로 사용자 검색
        Query query = database.child("users").orderByChild("email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showLoading(false);

                if (dataSnapshot.exists()) {
                    // 사용자 찾음
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        String storedPassword = userSnapshot.child("password").getValue(String.class);
                        String userName = userSnapshot.child("name").getValue(String.class);
                        Boolean isActive = userSnapshot.child("isActive").getValue(Boolean.class);

                        // 계정 활성화 확인
                        if (isActive != null && !isActive) {
                            Toast.makeText(LoginActivity.this,
                                    "비활성화된 계정입니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 비밀번호 확인
                        if (storedPassword != null && storedPassword.equals(hashPassword(password))) {
                            // 로그인 성공
                            saveLoginState(userId, email, userName);
                            Toast.makeText(LoginActivity.this,
                                    userName + "님, 환영합니다!", Toast.LENGTH_SHORT).show();

                            // 메인 네비게이션 화면으로 이동 ← 변경된 부분
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // 비밀번호 틀림
                            Toast.makeText(LoginActivity.this,
                                    "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                        break; // 첫 번째 결과만 처리
                    }
                } else {
                    // 사용자 없음
                    Toast.makeText(LoginActivity.this,
                            "등록되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "로그인 중 오류가 발생했습니다: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 이메일 형식 검증
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    // 간단한 비밀번호 해싱 (기존과 동일)
    private String hashPassword(String password) {
        return password + "_hashed"; // 임시 해싱
    }

    // 로그인 상태 저장
    private void saveLoginState(String userId, String email, String userName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userId);
        editor.putString("userEmail", email);
        editor.putString("userName", userName);
        editor.putLong("loginTime", System.currentTimeMillis());
        editor.apply();
    }

    // 로그인 상태 확인
    private boolean isUserLoggedIn() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        // 로그인 유효시간 확인 (예: 30일)
        if (isLoggedIn) {
            long loginTime = sharedPreferences.getLong("loginTime", 0);
            long currentTime = System.currentTimeMillis();
            long daysPassed = (currentTime - loginTime) / (1000 * 60 * 60 * 24);

            if (daysPassed > 30) {
                // 30일 지났으면 자동 로그아웃
                logout();
                return false;
            }
        }

        return isLoggedIn;
    }

    // 로그아웃
    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    // 로딩 상태 표시/숨김
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        buttonLogin.setEnabled(!isLoading);
        buttonLogin.setText(isLoading ? "로그인 중..." : "로그인");
    }
}