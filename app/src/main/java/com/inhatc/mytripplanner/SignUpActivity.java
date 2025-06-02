package com.inhatc.mytripplanner;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextPasswordConfirm;
    private RadioGroup radioGroupGender;
    private Button buttonSignUp;
    private Button buttonCheckEmail;
    private ProgressBar progressBar;

    // Firebase Realtime Database
    private DatabaseReference database;
    private boolean isEmailChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Firebase Database 초기화
        database = FirebaseDatabase.getInstance().getReference();

        initViews();
        setClickListeners();
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPasswordConfirm = findViewById(R.id.editTextPasswordConfirm);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonCheckEmail = findViewById(R.id.buttonCheckEmail);
        progressBar = findViewById(R.id.progressBar); // XML에 추가 필요
    }

    private void setClickListeners() {
        // 이메일 중복확인 버튼
        buttonCheckEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkEmailDuplicate();
            }
        });

        // 회원가입 버튼
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignUp();
            }
        });

        // 이메일 입력창 변경 시 중복확인 상태 초기화
        editTextEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    isEmailChecked = false;
                    buttonCheckEmail.setText("중복확인");
                }
            }
        });
    }

    private void checkEmailDuplicate() {
        String email = editTextEmail.getText().toString().trim();

        if (email.isEmpty()) {
            editTextEmail.setError("이메일을 입력하세요");
            editTextEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            editTextEmail.setError("올바른 이메일 형식이 아닙니다");
            editTextEmail.requestFocus();
            return;
        }

        // 로딩 표시
        showLoading(true);
        buttonCheckEmail.setEnabled(false);

        // Firebase에서 이메일 중복 확인
        Query query = database.child("users").orderByChild("email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showLoading(false);
                buttonCheckEmail.setEnabled(true);

                if (dataSnapshot.exists()) {
                    // 이메일 중복
                    Toast.makeText(SignUpActivity.this,
                            "이미 사용 중인 이메일입니다", Toast.LENGTH_SHORT).show();
                    isEmailChecked = false;
                    buttonCheckEmail.setText("중복확인");
                } else {
                    // 사용 가능한 이메일
                    Toast.makeText(SignUpActivity.this,
                            "사용 가능한 이메일입니다", Toast.LENGTH_SHORT).show();
                    isEmailChecked = true;
                    buttonCheckEmail.setText("확인완료");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                buttonCheckEmail.setEnabled(true);
                Toast.makeText(SignUpActivity.this,
                        "이메일 확인 중 오류가 발생했습니다: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSignUp() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordConfirm = editTextPasswordConfirm.getText().toString().trim();

        // 성별 선택 확인
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedGenderId != -1) {
            RadioButton selectedGender = findViewById(selectedGenderId);
            gender = selectedGender.getText().toString();
        }

        // 입력 검증
        if (name.isEmpty()) {
            editTextName.setError("이름을 입력하세요");
            editTextName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError("이메일을 입력하세요");
            editTextEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            editTextEmail.setError("올바른 이메일 형식이 아닙니다");
            editTextEmail.requestFocus();
            return;
        }

        if (!isEmailChecked) {
            Toast.makeText(this, "이메일 중복확인을 해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("비밀번호를 입력하세요");
            editTextPassword.requestFocus();
            return;
        }

        if (!isValidPassword(password)) {
            editTextPassword.setError("비밀번호는 8자 이상, 영어와 숫자를 포함해야 합니다");
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            editTextPasswordConfirm.setError("비밀번호가 일치하지 않습니다");
            editTextPasswordConfirm.requestFocus();
            return;
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "성별을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 회원가입 처리
        registerUser(name, email, password, gender);
    }

    private void registerUser(String name, String email, String password, String gender) {
        // 로딩 표시
        showLoading(true);
        buttonSignUp.setEnabled(false);

        // 새 사용자 ID 생성
        String userId = database.child("users").push().getKey();

        if (userId == null) {
            showLoading(false);
            buttonSignUp.setEnabled(true);
            Toast.makeText(this, "회원가입 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 사용자 정보 객체 생성
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", name);
        userInfo.put("email", email);
        userInfo.put("password", hashPassword(password)); // 비밀번호 해싱
        userInfo.put("gender", gender);
        userInfo.put("createdAt", getCurrentDate());
        userInfo.put("isActive", true);
        userInfo.put("lastLoginAt", "");

        // Firebase에 사용자 정보 저장
        database.child("users").child(userId).setValue(userInfo)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    buttonSignUp.setEnabled(true);

                    if (task.isSuccessful()) {
                        // 회원가입 성공
                        Toast.makeText(SignUpActivity.this,
                                "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                        finish(); // 로그인 화면으로 돌아가기

                    } else {
                        // 회원가입 실패
                        String errorMessage = "회원가입에 실패했습니다.";
                        if (task.getException() != null) {
                            errorMessage += " " + task.getException().getMessage();
                        }
                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    buttonSignUp.setEnabled(true);
                    Toast.makeText(SignUpActivity.this,
                            "회원가입 중 오류가 발생했습니다: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // 이메일 형식 검증
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    // 비밀번호 검증 (8자 이상, 영어+숫자)
    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasNumber = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasNumber = true;
            }
        }

        return hasLetter && hasNumber;
    }

    // 간단한 비밀번호 해싱 (실제로는 더 강력한 해싱 사용 권장)
    private String hashPassword(String password) {
        // 실제 프로젝트에서는 BCrypt, SHA-256 등 사용
        return password + "_hashed"; // 임시 해싱
    }

    // 현재 날짜 반환
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // 로딩 상태 표시/숨김
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    // 로그인 화면으로 이동 (XML에서 onClick 호출)
    public void goToLogin(View view) {
        finish(); // 현재 화면 종료하고 로그인 화면으로 돌아가기
    }
}