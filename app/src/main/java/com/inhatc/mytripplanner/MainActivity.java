package com.inhatc.mytripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_with_navigation);

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // 로그인 상태 확인
        if (!isUserLoggedIn()) {
            // 로그인되어 있지 않으면 로그인 화면으로 이동
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupBottomNavigation();
        setupBackPressedCallback(); // 새로운 뒤로가기 처리

        // 기본으로 홈 프래그먼트 표시
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_schedule) {
                    selectedFragment = new ScheduleFragment();
                } else if (itemId == R.id.nav_map) {
                    selectedFragment = new MapFragment();
                } else if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_budget) {
                    selectedFragment = new BudgetFragment();
                } else if (itemId == R.id.nav_mypage) {
                    selectedFragment = new MyPageFragment();
                }

                if (selectedFragment != null) {
                    return loadFragment(selectedFragment);
                }
                return false;
            }
        });
    }

    // 새로운 뒤로가기 처리 방식 (Android 13+ 대응)
    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 홈 프래그먼트가 아닌 경우 홈으로 이동
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                if (!(currentFragment instanceof HomeFragment)) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                } else {
                    // 홈 프래그먼트에서 뒤로가기 시 앱 종료 확인
                    showExitDialog();
                }
            }
        };

        // 콜백을 Activity에 등록
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showExitDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("앱을 종료하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> finishAffinity())
                .setNegativeButton("아니오", null)
                .show();
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

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

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}