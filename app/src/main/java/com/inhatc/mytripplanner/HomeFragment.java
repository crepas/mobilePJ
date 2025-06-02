package com.inhatc.mytripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private TextView textViewWelcome;
    private TextView textViewSubTitle;
    private Button buttonNewTrip;
    private Button buttonLogout;

    // Firebase
    private DatabaseReference database;
    private SharedPreferences sharedPreferences;

    // 사용자 정보
    private String currentUserId;
    private String currentUserName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Firebase 초기화
        database = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // 사용자 정보 가져오기
        getCurrentUserInfo();

        // UI 초기화
        initViews(view);
        setClickListeners();

        // 사용자 여행 개수 로드
        loadUserTravelCount();

        return view;
    }

    private void initViews(View view) {
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewSubTitle = view.findViewById(R.id.textViewSubTitle);
        buttonNewTrip = view.findViewById(R.id.buttonNewTrip);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        updateWelcomeMessage();
    }

    private void setClickListeners() {
        // 새 여행 계획 버튼
        buttonNewTrip.setOnClickListener(v -> handleNewTripClick());

        // 로그아웃 버튼
        buttonLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void getCurrentUserInfo() {
        currentUserId = sharedPreferences.getString("userId", "");
        currentUserName = sharedPreferences.getString("userName", "사용자");

        // 로그인 정보가 없으면 로그인 화면으로 이동
        if (currentUserId.isEmpty()) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        // Firebase에서 최신 사용자 정보 확인
        database.child("users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            Boolean isActive = dataSnapshot.child("isActive").getValue(Boolean.class);

                            if (isActive != null && !isActive) {
                                Toast.makeText(getContext(),
                                        "계정이 비활성화되었습니다.", Toast.LENGTH_SHORT).show();
                                logout();
                                return;
                            }

                            if (name != null) {
                                currentUserName = name;
                                updateWelcomeMessage();
                            }

                        } else {
                            Toast.makeText(getContext(),
                                    "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            logout();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getContext(),
                                "사용자 정보 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateWelcomeMessage() {
        if (textViewWelcome != null) {
            textViewWelcome.setText(currentUserName + "님, 환영합니다!");
        }
    }

    private void loadUserTravelCount() {
        if (currentUserId.isEmpty()) return;

        // 사용자의 여행 개수 가져오기
        database.child("travels")
                .orderByChild("userId")
                .equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        long travelCount = dataSnapshot.getChildrenCount();
                        updateSubTitle(travelCount);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // 에러 처리 (선택적)
                    }
                });
    }

    private void updateSubTitle(long travelCount) {
        String subTitle;
        if (travelCount == 0) {
            subTitle = "첫 번째 여행을 계획해보세요!";
        } else {
            subTitle = "현재 " + travelCount + "개의 여행이 계획되어 있습니다";
        }

        if (textViewSubTitle != null) {
            textViewSubTitle.setText(subTitle);
        }
    }

    private void handleNewTripClick() {
        // 간단한 새 여행 생성
        createNewTrip();
    }

    private void createNewTrip() {
        if (currentUserId.isEmpty()) return;

        // 새 여행 ID 생성
        String travelId = database.child("travels").push().getKey();
        if (travelId == null) return;

        // 여행 데이터 생성
        java.util.Map<String, Object> travelData = new java.util.HashMap<>();
        travelData.put("userId", currentUserId);
        travelData.put("title", "새로운 여행 " + (System.currentTimeMillis() % 1000));
        travelData.put("description", "여행 설명을 입력하세요");
        travelData.put("startDate", "");
        travelData.put("endDate", "");
        travelData.put("budget", 0);
        travelData.put("createdAt", getCurrentDateTime());
        travelData.put("isActive", true);

        // Firebase에 저장
        database.child("travels").child(travelId).setValue(travelData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "새 여행이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "여행 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> logout())
                .setNegativeButton("아니오", null)
                .show();
    }

    private void logout() {
        // SharedPreferences 정리
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // 로그인 화면으로 이동
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private String getCurrentDateTime() {
        return new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());
    }
}