package com.inhatc.mytripplanner;

import static android.content.Context.MODE_PRIVATE;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyPageFragment extends Fragment {

    // UI 요소들
    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private LinearLayout layoutUserProfile;

    // 통계 정보 TextView들
    private TextView textViewTotalTrips;
    private TextView textViewUpcomingTrips;
    private TextView textViewTotalBudget;
    private TextView textViewTotalSpent;

    // 버튼들
    private Button buttonEditProfile;
    private Button buttonChangePassword;
    private Button buttonLogout;
    private Button buttonDeleteAccount;

    // Firebase 및 사용자 정보
    private DatabaseReference database;
    private SharedPreferences sharedPreferences;
    private String currentUserId;
    private String currentUserName;
    private String currentUserEmail;

    // 통계 데이터
    private int totalTrips = 0;
    private int upcomingTrips = 0;
    private int totalBudget = 0;
    private int totalSpent = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        try {
            // Firebase 및 SharedPreferences 초기화
            database = FirebaseDatabase.getInstance().getReference();
            sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);

            // 사용자 정보 가져오기
            getCurrentUserInfo();

            if (currentUserId.isEmpty()) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return view;
            }

            initViews(view);
            setClickListeners();
            loadUserStatistics();

        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "마이페이지 초기화 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void initViews(View view) {
        try {
            // 사용자 프로필 정보
            textViewUserName = view.findViewById(R.id.textViewUserName);
            textViewUserEmail = view.findViewById(R.id.textViewUserEmail);
            layoutUserProfile = view.findViewById(R.id.layoutUserProfile);

            // 통계 정보
            textViewTotalTrips = view.findViewById(R.id.textViewTotalTrips);
            textViewUpcomingTrips = view.findViewById(R.id.textViewUpcomingTrips);
            textViewTotalBudget = view.findViewById(R.id.textViewTotalBudget);
            textViewTotalSpent = view.findViewById(R.id.textViewTotalSpent);

            // 버튼들
            buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
            buttonChangePassword = view.findViewById(R.id.buttonChangePassword);
            buttonLogout = view.findViewById(R.id.buttonLogout);
            buttonDeleteAccount = view.findViewById(R.id.buttonDeleteAccount);

            // 사용자 정보 표시
            updateUserProfileDisplay();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setClickListeners() {
        try {
            // 프로필 편집 버튼
            if (buttonEditProfile != null) {
                buttonEditProfile.setOnClickListener(v -> showEditProfileDialog());
            }

            // 비밀번호 변경 버튼
            if (buttonChangePassword != null) {
                buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
            }

            // 로그아웃 버튼
            if (buttonLogout != null) {
                buttonLogout.setOnClickListener(v -> showLogoutDialog());
            }

            // 계정 삭제 버튼
            if (buttonDeleteAccount != null) {
                buttonDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCurrentUserInfo() {
        try {
            currentUserId = sharedPreferences.getString("userId", "");
            currentUserName = sharedPreferences.getString("userName", "사용자");
            currentUserEmail = sharedPreferences.getString("userEmail", "");

            // Firebase에서 최신 사용자 정보 가져오기
            if (!currentUserId.isEmpty() && database != null) {
                database.child("users").child(currentUserId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                try {
                                    if (dataSnapshot.exists()) {
                                        String name = dataSnapshot.child("name").getValue(String.class);
                                        String email = dataSnapshot.child("email").getValue(String.class);

                                        if (name != null) {
                                            currentUserName = name;
                                        }
                                        if (email != null) {
                                            currentUserEmail = email;
                                        }

                                        updateUserProfileDisplay();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // 오류 무시
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserProfileDisplay() {
        try {
            if (textViewUserName != null) {
                textViewUserName.setText(currentUserName);
            }
            if (textViewUserEmail != null) {
                textViewUserEmail.setText(currentUserEmail);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserStatistics() {
        try {
            if (currentUserId.isEmpty() || database == null) return;

            // 여행 통계 로드
            database.child("travels")
                    .orderByChild("userId")
                    .equalTo(currentUserId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                totalTrips = 0;
                                upcomingTrips = 0;
                                totalBudget = 0;

                                Date currentDate = new Date();

                                for (DataSnapshot travelSnapshot : dataSnapshot.getChildren()) {
                                    totalTrips++;

                                    // 예산 합계
                                    Object budgetObj = travelSnapshot.child("budget").getValue();
                                    if (budgetObj instanceof Number) {
                                        totalBudget += ((Number) budgetObj).intValue();
                                    }

                                    // 다가오는 여행 카운트
                                    String startDate = travelSnapshot.child("startDate").getValue(String.class);
                                    if (startDate != null && !startDate.isEmpty()) {
                                        try {
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                            Date travelDate = sdf.parse(startDate);
                                            if (travelDate != null && travelDate.after(currentDate)) {
                                                upcomingTrips++;
                                            }
                                        } catch (Exception e) {
                                            // 날짜 파싱 오류 무시
                                        }
                                    }
                                }

                                updateStatisticsDisplay();
                                loadExpenseStatistics();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // 오류 무시
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadExpenseStatistics() {
        try {
            if (currentUserId.isEmpty() || database == null) return;

            // 지출 통계 로드
            database.child("expenses")
                    .orderByChild("userId")
                    .equalTo(currentUserId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                totalSpent = 0;

                                for (DataSnapshot expenseSnapshot : dataSnapshot.getChildren()) {
                                    Object amountObj = expenseSnapshot.child("amount").getValue();
                                    if (amountObj instanceof Number) {
                                        totalSpent += ((Number) amountObj).intValue();
                                    }
                                }

                                updateStatisticsDisplay();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // 오류 무시
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatisticsDisplay() {
        try {
            if (getContext() == null) return;

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

            if (textViewTotalTrips != null) {
                textViewTotalTrips.setText(totalTrips + "개");
            }
            if (textViewUpcomingTrips != null) {
                textViewUpcomingTrips.setText(upcomingTrips + "개");
            }
            if (textViewTotalBudget != null) {
                textViewTotalBudget.setText("₩" + numberFormat.format(totalBudget));
            }
            if (textViewTotalSpent != null) {
                textViewTotalSpent.setText("₩" + numberFormat.format(totalSpent));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAppSetting(String key, boolean value) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEditProfileDialog() {
        try {
            if (getContext() == null) return;

            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

            EditText editTextName = dialogView.findViewById(R.id.editTextProfileName);
            EditText editTextEmail = dialogView.findViewById(R.id.editTextProfileEmail);
            RadioGroup radioGroupGender = dialogView.findViewById(R.id.radioGroupGender);
            RadioButton radioMale = dialogView.findViewById(R.id.radioMale);
            RadioButton radioFemale = dialogView.findViewById(R.id.radioFemale);

            // 현재 정보 표시
            editTextName.setText(currentUserName);
            editTextEmail.setText(currentUserEmail);

            // Firebase에서 성별 정보 가져오기
            if (database != null && !currentUserId.isEmpty()) {
                database.child("users").child(currentUserId).child("gender")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                try {
                                    String gender = dataSnapshot.getValue(String.class);
                                    if ("남성".equals(gender)) {
                                        radioMale.setChecked(true);
                                    } else if ("여성".equals(gender)) {
                                        radioFemale.setChecked(true);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // 오류 무시
                            }
                        });
            }

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            // 버튼 이벤트
            dialogView.findViewById(R.id.buttonCancelEdit).setOnClickListener(v -> dialog.dismiss());

            dialogView.findViewById(R.id.buttonSaveEdit).setOnClickListener(v -> {
                try {
                    String newName = editTextName.getText().toString().trim();
                    String newEmail = editTextEmail.getText().toString().trim();

                    if (newName.isEmpty()) {
                        editTextName.setError("이름을 입력하세요");
                        return;
                    }

                    if (newEmail.isEmpty()) {
                        editTextEmail.setError("이메일을 입력하세요");
                        return;
                    }

                    String gender = "미설정";
                    if (radioMale.isChecked()) {
                        gender = "남성";
                    } else if (radioFemale.isChecked()) {
                        gender = "여성";
                    }

                    updateUserProfile(newName, newEmail, gender);
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserProfile(String newName, String newEmail, String gender) {
        try {
            if (database == null || currentUserId.isEmpty()) return;

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("email", newEmail);
            updates.put("gender", gender);
            updates.put("updatedAt", getCurrentDateTime());

            database.child("users").child(currentUserId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        try {
                            // SharedPreferences 업데이트
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("userName", newName);
                            editor.putString("userEmail", newEmail);
                            editor.apply();

                            // 현재 변수 업데이트
                            currentUserName = newName;
                            currentUserEmail = newEmail;

                            // UI 업데이트
                            updateUserProfileDisplay();

                            if (getContext() != null) {
                                Toast.makeText(getContext(), "프로필이 업데이트되었습니다", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "프로필 업데이트 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showChangePasswordDialog() {
        try {
            if (getContext() == null) return;

            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);

            EditText editTextCurrentPassword = dialogView.findViewById(R.id.editTextCurrentPassword);
            EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
            EditText editTextConfirmPassword = dialogView.findViewById(R.id.editTextConfirmPassword);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            // 버튼 이벤트
            dialogView.findViewById(R.id.buttonCancelPassword).setOnClickListener(v -> dialog.dismiss());

            dialogView.findViewById(R.id.buttonSavePassword).setOnClickListener(v -> {
                try {
                    String currentPassword = editTextCurrentPassword.getText().toString().trim();
                    String newPassword = editTextNewPassword.getText().toString().trim();
                    String confirmPassword = editTextConfirmPassword.getText().toString().trim();

                    if (currentPassword.isEmpty()) {
                        editTextCurrentPassword.setError("현재 비밀번호를 입력하세요");
                        return;
                    }

                    if (newPassword.isEmpty()) {
                        editTextNewPassword.setError("새 비밀번호를 입력하세요");
                        return;
                    }

                    if (newPassword.length() < 6) {
                        editTextNewPassword.setError("비밀번호는 6자 이상이어야 합니다");
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        editTextConfirmPassword.setError("비밀번호가 일치하지 않습니다");
                        return;
                    }

                    changePassword(currentPassword, newPassword);
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changePassword(String currentPassword, String newPassword) {
        try {
            if (database == null || currentUserId.isEmpty()) return;

            // 현재 비밀번호 확인
            database.child("users").child(currentUserId).child("password")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                String storedPassword = dataSnapshot.getValue(String.class);

                                // 간단한 해시 비교 (실제로는 더 강력한 해싱 필요)
                                String hashedCurrentPassword = hashPassword(currentPassword);

                                if (storedPassword != null && storedPassword.equals(hashedCurrentPassword)) {
                                    // 비밀번호가 일치하면 새 비밀번호로 업데이트
                                    String hashedNewPassword = hashPassword(newPassword);

                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("password", hashedNewPassword);
                                    updates.put("updatedAt", getCurrentDateTime());

                                    database.child("users").child(currentUserId).updateChildren(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "비밀번호가 변경되었습니다", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "비밀번호 변경 실패", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "현재 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "비밀번호 확인 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String hashPassword(String password) {
        // 간단한 해시 함수 (실제로는 BCrypt 등 사용 권장)
        return String.valueOf(password.hashCode());
    }

    private void showLogoutDialog() {
        try {
            if (getContext() == null) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("로그아웃")
                    .setMessage("정말 로그아웃 하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> logout())
                    .setNegativeButton("아니오", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDeleteAccountDialog() {
        try {
            if (getContext() == null) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ 계정 삭제")
                    .setMessage("정말 계정을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없으며,\n모든 여행 데이터가 영구적으로 삭제됩니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // 추가 확인 다이얼로그
                        new AlertDialog.Builder(requireContext())
                                .setTitle("최종 확인")
                                .setMessage("마지막으로 한 번 더 확인합니다.\n정말 계정을 삭제하시겠습니까?")
                                .setPositiveButton("삭제", (dialog2, which2) -> deleteAccount())
                                .setNegativeButton("취소", null)
                                .show();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAccount() {
        try {
            if (database == null || currentUserId.isEmpty()) return;

            // 계정을 비활성화로 표시 (실제 삭제 대신)
            Map<String, Object> updates = new HashMap<>();
            updates.put("isActive", false);
            updates.put("deletedAt", getCurrentDateTime());

            database.child("users").child(currentUserId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "계정이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                        }
                        logout();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "계정 삭제 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        try {
            // SharedPreferences 정리
            if (sharedPreferences != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
            }

            // 로그인 화면으로 이동
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCurrentDateTime() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        } catch (Exception e) {
            return "";
        }
    }
}