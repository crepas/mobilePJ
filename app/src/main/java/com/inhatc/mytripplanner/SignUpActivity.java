package com.inhatc.mytripplanner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private RadioGroup radioGroupGender;
    private Button buttonSignUp;
    private Button buttonCheckEmail;

    private SharedPreferences sharedPreferences;
    private boolean isEmailChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonCheckEmail = findViewById(R.id.buttonCheckEmail);
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
                }
            }
        });
    }

    private void checkEmailDuplicate() {
        String email = editTextEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "올바른 이메일 형식이 아닙니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 중복확인
        String existingPassword = sharedPreferences.getString(email, null);
        if (existingPassword != null) {
            Toast.makeText(this, "이미 사용 중인 이메일입니다", Toast.LENGTH_SHORT).show();
            isEmailChecked = false;
        } else {
            Toast.makeText(this, "사용 가능한 이메일입니다", Toast.LENGTH_SHORT).show();
            isEmailChecked = true;
        }
    }

    private void handleSignUp() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 성별 선택 확인
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedGenderId != -1) {
            RadioButton selectedGender = findViewById(selectedGenderId);
            gender = selectedGender.getText().toString();
        }

        // 입력 검증
        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "올바른 이메일 형식이 아닙니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEmailChecked) {
            Toast.makeText(this, "이메일 중복확인을 해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "비밀번호는 8자 이상, 영어와 숫자를 포함해야 합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "성별을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 회원가입 처리
        if (registerUser(name, email, password, gender)) {
            Toast.makeText(this, "회원가입이 성공하였습니다!", Toast.LENGTH_SHORT).show();
            finish(); // 현재 화면 종료하고 로그인 화면으로 돌아가기
        } else {
            Toast.makeText(this, "회원가입에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
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

    // 사용자 등록
    private boolean registerUser(String name, String email, String password, String gender) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(email, password); // 로그인용
            editor.putString(email + "_name", name); // 사용자 이름
            editor.putString(email + "_gender", gender); // 성별
            editor.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 로그인 화면으로 이동 (XML에서 onClick 호출)
    public void goToLogin(View view) {
        finish(); // 현재 화면 종료하고 로그인 화면으로 돌아가기
    }
}