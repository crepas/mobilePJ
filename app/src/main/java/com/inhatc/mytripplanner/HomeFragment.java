package com.inhatc.mytripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private TextView textViewStats;
    private LinearLayout layoutUpcomingTrips;
    private TextView textViewNoUpcomingTrips;
    private Button buttonNewTrip;
    private Button buttonQuickSchedule;
    private Button buttonQuickMap;
    private Button buttonQuickBudget;
    private Button buttonQuickMyPage;

    // Firebase
    private DatabaseReference database;
    private SharedPreferences sharedPreferences;

    // 사용자 정보
    private String currentUserId;
    private String currentUserName;

    // 여행 데이터
    private List<TravelItem> upcomingTrips = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_home, container, false);

            // Firebase 초기화
            database = FirebaseDatabase.getInstance().getReference();
            sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);

            // 사용자 정보 가져오기
            getCurrentUserInfo();

            // UI 초기화
            initViews(view);
            setClickListeners();

            // 데이터 로드
            loadUserData();

        } catch (Exception e) {
            e.printStackTrace();
            // 오류 발생 시 기본 뷰 반환
            if (view == null) {
                view = createFallbackView(inflater, container);
            }
        }

        return view;
    }

    private View createFallbackView(LayoutInflater inflater, ViewGroup container) {
        // 오류 발생 시 간단한 기본 뷰 생성
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView errorText = new TextView(getContext());
        errorText.setText("홈 화면 로딩 중 오류가 발생했습니다.");
        errorText.setTextSize(16);

        Button retryButton = new Button(getContext());
        retryButton.setText("다시 시도");
        retryButton.setOnClickListener(v -> {
            // 프래그먼트 재시작
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        layout.addView(errorText);
        layout.addView(retryButton);

        return layout;
    }

    private void initViews(View view) {
        try {
            textViewStats = view.findViewById(R.id.textViewStats);
            layoutUpcomingTrips = view.findViewById(R.id.layoutUpcomingTrips);
            buttonNewTrip = view.findViewById(R.id.buttonNewTrip);

            // 퀵 액세스 버튼들 - null 체크
            buttonQuickSchedule = view.findViewById(R.id.buttonQuickSchedule);
            buttonQuickMap = view.findViewById(R.id.buttonQuickMap);
            buttonQuickBudget = view.findViewById(R.id.buttonQuickBudget);
            buttonQuickMyPage = view.findViewById(R.id.buttonQuickMyPage);

            // "데이터 없음" 텍스트뷰
            textViewNoUpcomingTrips = view.findViewById(R.id.textViewNoUpcomingTrips);

            updateWelcomeMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setClickListeners() {
        try {
            // 새 여행 계획 버튼
            if (buttonNewTrip != null) {
                buttonNewTrip.setOnClickListener(v -> handleNewTripClick());
            }

            // 퀵 액세스 버튼들 - null 체크 추가
            if (buttonQuickSchedule != null) {
                buttonQuickSchedule.setOnClickListener(v -> navigateToTab(R.id.nav_schedule));
            }
            if (buttonQuickMap != null) {
                buttonQuickMap.setOnClickListener(v -> navigateToTab(R.id.nav_map));
            }
            if (buttonQuickBudget != null) {
                buttonQuickBudget.setOnClickListener(v -> navigateToTab(R.id.nav_budget));
            }
            if (buttonQuickMyPage != null) {
                buttonQuickMyPage.setOnClickListener(v -> navigateToTab(R.id.nav_mypage));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCurrentUserInfo() {
        try {
            currentUserId = sharedPreferences.getString("userId", "");
            currentUserName = sharedPreferences.getString("userName", "사용자");

            // 로그인 정보가 없으면 로그인 화면으로 이동
            if (currentUserId.isEmpty()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
                return;
            }

            // Firebase에서 최신 사용자 정보 확인
            if (database != null) {
                database.child("users").child(currentUserId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                try {
                                    if (dataSnapshot.exists()) {
                                        String name = dataSnapshot.child("name").getValue(String.class);
                                        Boolean isActive = dataSnapshot.child("isActive").getValue(Boolean.class);

                                        if (isActive != null && !isActive) {
                                            if (getContext() != null) {
                                                Toast.makeText(getContext(),
                                                        "계정이 비활성화되었습니다.", Toast.LENGTH_SHORT).show();
                                            }
                                            logout();
                                            return;
                                        }

                                        if (name != null) {
                                            currentUserName = name;
                                            updateWelcomeMessage();
                                        }

                                    } else {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(),
                                                    "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                        logout();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(),
                                            "사용자 정보 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWelcomeMessage() {
        try {
            // 이제 환영 메시지는 updateUI()에서 동적으로 생성됨
            // 별도의 textViewWelcome은 사용하지 않음
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserData() {
        try {
            if (currentUserId.isEmpty() || database == null) return;

            // 사용자의 다가오는 여행 데이터만 가져오기
            database.child("travels")
                    .orderByChild("userId")
                    .equalTo(currentUserId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
                                upcomingTrips.clear();

                                Date currentDate = new Date();

                                for (DataSnapshot travelSnapshot : dataSnapshot.getChildren()) {
                                    TravelItem travel = new TravelItem();
                                    travel.id = travelSnapshot.getKey();
                                    travel.title = travelSnapshot.child("title").getValue(String.class);
                                    travel.description = travelSnapshot.child("description").getValue(String.class);
                                    travel.startDate = travelSnapshot.child("startDate").getValue(String.class);
                                    travel.endDate = travelSnapshot.child("endDate").getValue(String.class);

                                    // budget 안전하게 처리
                                    Object budgetObj = travelSnapshot.child("budget").getValue();
                                    if (budgetObj instanceof Number) {
                                        travel.budget = ((Number) budgetObj).intValue();
                                    } else {
                                        travel.budget = 0;
                                    }

                                    // 다가오는 여행만 필터링
                                    if (travel.startDate != null && !travel.startDate.isEmpty()) {
                                        Date startDate = parseDate(travel.startDate);
                                        if (startDate != null && startDate.after(currentDate)) {
                                            upcomingTrips.add(travel);
                                        }
                                    }
                                }

                                // 날짜 순으로 정렬 (가까운 것부터)
                                upcomingTrips.sort((t1, t2) -> {
                                    try {
                                        Date d1 = parseDate(t1.startDate);
                                        Date d2 = parseDate(t2.startDate);
                                        if (d1 == null || d2 == null) return 0;
                                        return d1.compareTo(d2);
                                    } catch (Exception e) {
                                        return 0;
                                    }
                                });

                                updateUI();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "데이터 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        try {
            if (getContext() == null) return;

            // 통계 정보를 날짜 + 환영 메시지로 변경
            if (textViewStats != null && currentUserName != null) {
                // 현재 날짜 가져오기
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA);
                String currentDate = sdf.format(new Date());

                // 시간대별 인사말
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String greeting;
                String emoji;

                if (hour >= 6 && hour < 12) {
                    greeting = "좋은 아침이에요";
                    emoji = "🌅";
                } else if (hour >= 12 && hour < 18) {
                    greeting = "좋은 오후에요";
                    emoji = "☀️";
                } else if (hour >= 18 && hour < 22) {
                    greeting = "좋은 저녁이에요";
                    emoji = "🌆";
                } else {
                    greeting = "좋은 밤이에요";
                    emoji = "🌙";
                }

                String welcomeText = "📅 " + currentDate + "\n" +
                        currentUserName + "님, 환영합니다! " + emoji + "\n" +
                        greeting + "!";

                textViewStats.setText(welcomeText);
            }

            // 다가오는 여행 목록 업데이트
            if (layoutUpcomingTrips != null) {
                layoutUpcomingTrips.removeAllViews();

                // 다가오는 여행 헤더 추가
                TextView tripHeader = new TextView(getContext());
                if (upcomingTrips.isEmpty()) {
                    tripHeader.setText("다가오는 여행이 없습니다 ✈️");
                } else {
                    tripHeader.setText("다가오는 여행 " + upcomingTrips.size() + "개 ✈️");
                }
                tripHeader.setTextSize(16);
                tripHeader.setTextColor(getResources().getColor(android.R.color.black));
                tripHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                tripHeader.setPadding(16, 16, 16, 12);
                layoutUpcomingTrips.addView(tripHeader);

                if (upcomingTrips.isEmpty()) {
                    if (textViewNoUpcomingTrips != null) {
                        layoutUpcomingTrips.addView(textViewNoUpcomingTrips);
                    }
                } else {
                    // 최대 3개까지만 표시
                    int maxUpcoming = Math.min(upcomingTrips.size(), 3);
                    for (int i = 0; i < maxUpcoming; i++) {
                        View tripView = createTripItemView(upcomingTrips.get(i));
                        if (tripView != null) {
                            layoutUpcomingTrips.addView(tripView);
                        }
                    }

                    // 더 많은 여행이 있으면 "더 보기" 버튼 추가
                    if (upcomingTrips.size() > 3) {
                        Button moreButton = new Button(getContext());
                        moreButton.setText("+" + (upcomingTrips.size() - 3) + "개 더 보기");
                        moreButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        moreButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                        moreButton.setOnClickListener(v -> {
                            Toast.makeText(getContext(), "전체 여행 목록 화면 (개발 예정)", Toast.LENGTH_SHORT).show();
                        });
                        layoutUpcomingTrips.addView(moreButton);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View createTripItemView(TravelItem travel) {
        try {
            if (getContext() == null) return null;

            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            itemLayout.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12);
            itemLayout.setLayoutParams(params);

            // 상단: 여행 제목과 D-day
            LinearLayout topLayout = new LinearLayout(getContext());
            topLayout.setOrientation(LinearLayout.HORIZONTAL);
            topLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView titleView = new TextView(getContext());
            titleView.setText(travel.title != null ? travel.title : "제목 없음");
            titleView.setTextSize(16);
            titleView.setTextColor(getResources().getColor(android.R.color.black));
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            titleView.setLayoutParams(titleParams);

            TextView ddayView = new TextView(getContext());
            String ddayText = getDdayText(travel.startDate);
            ddayView.setText(ddayText);
            ddayView.setTextSize(14);
            ddayView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            ddayView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            topLayout.addView(titleView);
            topLayout.addView(ddayView);

            // 여행 날짜
            TextView dateView = new TextView(getContext());
            String dateText = "";
            if (travel.startDate != null && !travel.startDate.isEmpty()) {
                if (travel.endDate != null && !travel.endDate.isEmpty()) {
                    dateText = "📅 " + travel.startDate + " ~ " + travel.endDate;
                } else {
                    dateText = "📅 " + travel.startDate;
                }
            }
            dateView.setText(dateText);
            dateView.setTextSize(14);
            dateView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            dateView.setPadding(0, 4, 0, 0);

            // 예산 정보
            TextView budgetView = new TextView(getContext());
            if (travel.budget > 0) {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
                budgetView.setText("💰 예산: ₩" + numberFormat.format(travel.budget));
            } else {
                budgetView.setText("💰 예산: 미설정");
            }
            budgetView.setTextSize(13);
            budgetView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            budgetView.setPadding(0, 2, 0, 0);

            itemLayout.addView(topLayout);
            itemLayout.addView(dateView);
            itemLayout.addView(budgetView);

            // 클릭 리스너 추가
            itemLayout.setOnClickListener(v -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            travel.title + " 상세 화면 (개발 예정)", Toast.LENGTH_SHORT).show();
                }
            });

            return itemLayout;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getDdayText(String startDate) {
        try {
            if (startDate == null || startDate.isEmpty()) return "";

            Date start = parseDate(startDate);
            if (start == null) return "";

            Date today = new Date();
            long diffTime = start.getTime() - today.getTime();
            long diffDays = diffTime / (1000 * 60 * 60 * 24);

            if (diffDays == 0) {
                return "D-Day! 🎉";
            } else if (diffDays > 0) {
                return "D-" + diffDays;
            } else {
                return "진행 중";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    private void navigateToTab(int tabId) {
        try {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(tabId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNewTripClick() {
        try {
            createNewTrip();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNewTrip() {
        try {
            if (currentUserId.isEmpty() || database == null) return;

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
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "새 여행이 생성되었습니다! ✨", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "여행 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
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
        return new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());
    }

    // 여행 데이터를 위한 내부 클래스
    private static class TravelItem {
        String id;
        String title;
        String description;
        String startDate;
        String endDate;
        int budget;
    }
}