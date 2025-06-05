package com.inhatc.mytripplanner;

import static android.content.Context.MODE_PRIVATE;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

public class BudgetFragment extends Fragment {

    // UI 요소들
    private Spinner spinnerTravels;
    private TextView textViewTotalBudget;
    private TextView textViewUsedAmount;
    private TextView textViewRemainingBudget;
    private TextView textViewBudgetProgress;
    private ProgressBar progressBarBudget;
    private LinearLayout layoutBudgetWarning;
    private LinearLayout layoutRecentExpenses;
    private TextView textViewNoExpenses;

    // 카테고리별 금액 표시 TextView들
    private TextView textViewAccommodationAmount;
    private TextView textViewTransportAmount;
    private TextView textViewFoodAmount;
    private TextView textViewTourismAmount;
    private TextView textViewOtherAmount;

    // 버튼들
    private Button buttonAddExpense;
    private Button buttonSetBudget;
    private Button buttonViewAllExpenses;

    // 원형 차트 (MPAndroidChart)
    private PieChart pieChart;

    // Firebase 및 사용자 정보
    private DatabaseReference database;
    private SharedPreferences sharedPreferences;
    private String currentUserId;

    // 데이터
    private List<TravelItem> travelList = new ArrayList<>();
    private TravelItem selectedTravel;
    private Map<String, Integer> categoryExpenses = new HashMap<>();
    private Map<String, Integer> categoryBudgets = new HashMap<>(); // 카테고리별 예산 저장
    private List<ExpenseItem> recentExpenses = new ArrayList<>();
    private int totalBudget = 0;
    private int totalUsed = 0;
    private int selectedTravelPosition = 0; // 선택된 여행의 스피너 위치 저장

    // 카테고리 목록
    private final String[] EXPENSE_CATEGORIES = {"숙박", "교통", "식비", "관광", "기타"};
    private final String[] CATEGORY_EMOJIS = {"🏨", "🚗", "🍽️", "🎭", "📦"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        try {
            // Firebase 및 SharedPreferences 초기화
            database = FirebaseDatabase.getInstance().getReference();
            sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            currentUserId = sharedPreferences.getString("userId", "");

            if (currentUserId.isEmpty()) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return view;
            }

            initViews(view);
            initCategoryExpenses();
            setClickListeners();
            loadTravelData();

        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "예산 화면 초기화 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void initViews(View view) {
        try {
            // 스피너
            spinnerTravels = view.findViewById(R.id.spinnerTravels);

            // 예산 현황 TextView들
            textViewTotalBudget = view.findViewById(R.id.textViewTotalBudget);
            textViewUsedAmount = view.findViewById(R.id.textViewUsedAmount);
            textViewRemainingBudget = view.findViewById(R.id.textViewRemainingBudget);
            textViewBudgetProgress = view.findViewById(R.id.textViewBudgetProgress);
            progressBarBudget = view.findViewById(R.id.progressBarBudget);

            // 카테고리별 금액 TextView들
            textViewAccommodationAmount = view.findViewById(R.id.textViewAccommodationAmount);
            textViewTransportAmount = view.findViewById(R.id.textViewTransportAmount);
            textViewFoodAmount = view.findViewById(R.id.textViewFoodAmount);
            textViewTourismAmount = view.findViewById(R.id.textViewTourismAmount);
            textViewOtherAmount = view.findViewById(R.id.textViewOtherAmount);

            // 레이아웃들
            layoutBudgetWarning = view.findViewById(R.id.layoutBudgetWarning);
            layoutRecentExpenses = view.findViewById(R.id.layoutRecentExpenses);
            textViewNoExpenses = view.findViewById(R.id.textViewNoExpenses);

            // 버튼들
            buttonAddExpense = view.findViewById(R.id.buttonAddExpense);
            buttonSetBudget = view.findViewById(R.id.buttonSetBudget);
            buttonViewAllExpenses = view.findViewById(R.id.buttonViewAllExpenses);

            // 원형 차트
            pieChart = view.findViewById(R.id.pieChart);
            setupPieChart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCategoryExpenses() {
        try {
            categoryExpenses.clear();
            for (String category : EXPENSE_CATEGORIES) {
                categoryExpenses.put(category, 0);
            }

            // 카테고리별 예산도 초기화 (필요시)
            if (categoryBudgets.isEmpty()) {
                for (String category : EXPENSE_CATEGORIES) {
                    categoryBudgets.put(category, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setClickListeners() {
        try {
            // 여행 선택 스피너
            if (spinnerTravels != null) {
                spinnerTravels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        try {
                            selectedTravelPosition = position; // 선택된 위치 저장

                            if (position > 0 && position <= travelList.size()) {
                                selectedTravel = travelList.get(position - 1);
                                loadBudgetData();
                                loadExpenseData();
                            } else {
                                selectedTravel = null;
                                clearBudgetData();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        selectedTravel = null;
                        selectedTravelPosition = 0;
                        clearBudgetData();
                    }
                });
            }

            // 지출 추가 버튼
            if (buttonAddExpense != null) {
                buttonAddExpense.setOnClickListener(v -> {
                    try {
                        showAddExpenseDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // 예산 설정 버튼
            if (buttonSetBudget != null) {
                buttonSetBudget.setOnClickListener(v -> {
                    try {
                        showSetBudgetDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // 전체 지출 보기 버튼
            if (buttonViewAllExpenses != null) {
                buttonViewAllExpenses.setOnClickListener(v -> {
                    try {
                        showAllExpenses();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTravelData() {
        try {
            if (database == null || currentUserId.isEmpty()) return;

            database.child("travels")
                    .orderByChild("userId")
                    .equalTo(currentUserId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                travelList.clear();

                                for (DataSnapshot travelSnapshot : dataSnapshot.getChildren()) {
                                    TravelItem travel = new TravelItem();
                                    travel.id = travelSnapshot.getKey();
                                    travel.title = travelSnapshot.child("title").getValue(String.class);
                                    travel.startDate = travelSnapshot.child("startDate").getValue(String.class);
                                    travel.endDate = travelSnapshot.child("endDate").getValue(String.class);

                                    Object budgetObj = travelSnapshot.child("budget").getValue();
                                    if (budgetObj instanceof Number) {
                                        travel.budget = ((Number) budgetObj).intValue();
                                    } else {
                                        travel.budget = 0;
                                    }

                                    travelList.add(travel);
                                }

                                updateTravelSpinner();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "여행 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTravelSpinner() {
        try {
            if (spinnerTravels == null || getContext() == null) return;

            List<String> travelTitles = new ArrayList<>();
            travelTitles.add("여행을 선택하세요");

            for (TravelItem travel : travelList) {
                String title = travel.title != null ? travel.title : "제목 없음";
                if (travel.startDate != null && !travel.startDate.isEmpty()) {
                    title += " (" + travel.startDate + ")";
                }
                travelTitles.add(title);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    travelTitles
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTravels.setAdapter(adapter);

            // 이전에 선택된 위치가 있고 유효한 범위라면 해당 위치로 설정
            if (selectedTravelPosition > 0 && selectedTravelPosition < travelTitles.size()) {
                spinnerTravels.setSelection(selectedTravelPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBudgetData() {
        try {
            if (selectedTravel == null) return;

            totalBudget = selectedTravel.budget;

            // Firebase에서 카테고리별 예산 데이터 로드
            loadCategoryBudgets();

            updateBudgetDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadExpenseData() {
        try {
            if (selectedTravel == null || database == null) return;

            database.child("expenses")
                    .orderByChild("travelId")
                    .equalTo(selectedTravel.id)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                // 카테고리별 지출 초기화
                                initCategoryExpenses();
                                recentExpenses.clear();
                                totalUsed = 0;

                                for (DataSnapshot expenseSnapshot : dataSnapshot.getChildren()) {
                                    ExpenseItem expense = new ExpenseItem();
                                    expense.id = expenseSnapshot.getKey();
                                    expense.title = expenseSnapshot.child("title").getValue(String.class);
                                    expense.category = expenseSnapshot.child("category").getValue(String.class);
                                    expense.date = expenseSnapshot.child("date").getValue(String.class);
                                    expense.note = expenseSnapshot.child("note").getValue(String.class);

                                    Object amountObj = expenseSnapshot.child("amount").getValue();
                                    if (amountObj instanceof Number) {
                                        expense.amount = ((Number) amountObj).intValue();
                                    }

                                    // 카테고리별 합계 계산
                                    if (expense.category != null && categoryExpenses.containsKey(expense.category)) {
                                        int currentAmount = categoryExpenses.get(expense.category);
                                        categoryExpenses.put(expense.category, currentAmount + expense.amount);
                                    }

                                    totalUsed += expense.amount;
                                    recentExpenses.add(expense);
                                }

                                // 최신 순으로 정렬
                                recentExpenses.sort((e1, e2) -> {
                                    if (e1.date == null || e2.date == null) return 0;
                                    return e2.date.compareTo(e1.date);
                                });

                                updateBudgetDisplay();
                                updateCategoryDisplay();
                                updatePieChart();
                                updateRecentExpensesDisplay();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "지출 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBudgetDisplay() {
        try {
            if (getContext() == null) return;

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

            if (textViewTotalBudget != null) {
                textViewTotalBudget.setText("₩" + numberFormat.format(totalBudget));
            }
            if (textViewUsedAmount != null) {
                textViewUsedAmount.setText("₩" + numberFormat.format(totalUsed));
            }

            int remaining = totalBudget - totalUsed;
            if (textViewRemainingBudget != null) {
                textViewRemainingBudget.setText("₩" + numberFormat.format(remaining));
            }

            // 진행률 계산
            int progress = 0;
            if (totalBudget > 0) {
                progress = Math.min((totalUsed * 100) / totalBudget, 100);
            }

            if (progressBarBudget != null) {
                progressBarBudget.setProgress(progress);
            }
            if (textViewBudgetProgress != null) {
                textViewBudgetProgress.setText(progress + "%");
            }

            // 예산 초과 경고
            if (layoutBudgetWarning != null) {
                if (totalUsed > totalBudget && totalBudget > 0) {
                    layoutBudgetWarning.setVisibility(View.VISIBLE);
                } else {
                    layoutBudgetWarning.setVisibility(View.GONE);
                }
            }

            // 진행률에 따른 색상 변경
            if (progressBarBudget != null && getResources() != null) {
                if (progress >= 100) {
                    progressBarBudget.setProgressTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
                } else if (progress >= 80) {
                    progressBarBudget.setProgressTintList(getResources().getColorStateList(android.R.color.holo_orange_dark));
                } else {
                    progressBarBudget.setProgressTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCategoryDisplay() {
        try {
            if (getContext() == null) return;

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

            if (textViewAccommodationAmount != null) {
                Integer amount = categoryExpenses.get("숙박");
                textViewAccommodationAmount.setText("₩" + numberFormat.format(amount != null ? amount : 0));
            }
            if (textViewTransportAmount != null) {
                Integer amount = categoryExpenses.get("교통");
                textViewTransportAmount.setText("₩" + numberFormat.format(amount != null ? amount : 0));
            }
            if (textViewFoodAmount != null) {
                Integer amount = categoryExpenses.get("식비");
                textViewFoodAmount.setText("₩" + numberFormat.format(amount != null ? amount : 0));
            }
            if (textViewTourismAmount != null) {
                Integer amount = categoryExpenses.get("관광");
                textViewTourismAmount.setText("₩" + numberFormat.format(amount != null ? amount : 0));
            }
            if (textViewOtherAmount != null) {
                Integer amount = categoryExpenses.get("기타");
                textViewOtherAmount.setText("₩" + numberFormat.format(amount != null ? amount : 0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePieChart() {
        try {
            if (pieChart == null || getContext() == null) return;

            List<PieEntry> entries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            // 카테고리별 색상 정의
            int[] CATEGORY_COLORS = {
                    Color.parseColor("#FF5722"), // 숙박 - 빨강
                    Color.parseColor("#2196F3"), // 교통 - 파랑
                    Color.parseColor("#4CAF50"), // 식비 - 초록
                    Color.parseColor("#FF9800"), // 관광 - 주황
                    Color.parseColor("#9C27B0")  // 기타 - 보라
            };

            // 지출이 있는 카테고리만 차트에 추가
            for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
                String category = EXPENSE_CATEGORIES[i];
                Integer amount = categoryExpenses.get(category);
                int amountValue = amount != null ? amount : 0;

                if (amountValue > 0) {
                    // 카테고리명에 이모지 포함
                    String label = CATEGORY_EMOJIS[i] + " " + category;
                    entries.add(new PieEntry(amountValue, label));
                    colors.add(CATEGORY_COLORS[i]);
                }
            }

            if (entries.isEmpty()) {
                // 지출이 없는 경우
                entries.add(new PieEntry(100, "지출 없음"));
                colors.add(Color.parseColor("#E0E0E0"));
            }

            // 데이터셋 생성
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(new PercentFormatter(pieChart));

            // 슬라이스 간격과 효과 설정
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(8f);

            // 데이터 설정
            PieData data = new PieData(dataSet);
            pieChart.setData(data);

            // 차트 새로고침
            pieChart.invalidate();

            // 애니메이션 효과
            pieChart.animateY(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupPieChart() {
        try {
            if (pieChart == null) return;

            // 차트 기본 설정
            pieChart.setUsePercentValues(true);
            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleColor(Color.WHITE);
            pieChart.setHoleRadius(45f);
            pieChart.setTransparentCircleRadius(50f);
            pieChart.setDrawCenterText(true);
            pieChart.setCenterText("💰\n지출 비율");
            pieChart.setCenterTextSize(14f);
            pieChart.setCenterTextColor(Color.parseColor("#333333"));
            pieChart.setRotationAngle(0);
            pieChart.setRotationEnabled(true);
            pieChart.setHighlightPerTapEnabled(true);

            // 설명 제거
            Description description = new Description();
            description.setText("");
            pieChart.setDescription(description);

            // 범례 설정
            Legend legend = pieChart.getLegend();
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
            legend.setTextSize(10f);
            legend.setEnabled(false); // 범례 숨기기 (아래 리스트가 있으니까)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateRecentExpensesDisplay() {
        try {
            if (layoutRecentExpenses == null || getContext() == null) return;

            layoutRecentExpenses.removeAllViews();

            if (recentExpenses.isEmpty()) {
                if (textViewNoExpenses != null) {
                    layoutRecentExpenses.addView(textViewNoExpenses);
                }
            } else {
                // 최대 5개까지만 표시
                int maxDisplay = Math.min(recentExpenses.size(), 5);
                for (int i = 0; i < maxDisplay; i++) {
                    View expenseView = createExpenseItemView(recentExpenses.get(i));
                    if (expenseView != null) {
                        layoutRecentExpenses.addView(expenseView);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View createExpenseItemView(ExpenseItem expense) {
        try {
            if (getContext() == null) return null;

            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            itemLayout.setPadding(16, 12, 16, 12);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            itemLayout.setLayoutParams(params);

            // 상단: 제목과 금액
            LinearLayout topLayout = new LinearLayout(getContext());
            topLayout.setOrientation(LinearLayout.HORIZONTAL);
            topLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView titleView = new TextView(getContext());
            titleView.setText(expense.title != null ? expense.title : "제목 없음");
            titleView.setTextSize(14);
            titleView.setTextColor(getResources().getColor(android.R.color.black));
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            titleView.setLayoutParams(titleParams);

            TextView amountView = new TextView(getContext());
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
            amountView.setText("₩" + numberFormat.format(expense.amount));
            amountView.setTextSize(14);
            amountView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            amountView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            topLayout.addView(titleView);
            topLayout.addView(amountView);

            // 하단: 카테고리와 날짜
            LinearLayout bottomLayout = new LinearLayout(getContext());
            bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
            bottomLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView categoryView = new TextView(getContext());
            String categoryText = expense.category != null ? expense.category : "기타";
            categoryView.setText(getCategoryEmoji(categoryText) + " " + categoryText);
            categoryView.setTextSize(12);
            categoryView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            categoryView.setLayoutParams(categoryParams);

            TextView dateView = new TextView(getContext());
            dateView.setText(expense.date != null ? expense.date : "");
            dateView.setTextSize(12);
            dateView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            dateView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            bottomLayout.addView(categoryView);
            bottomLayout.addView(dateView);

            itemLayout.addView(topLayout);
            itemLayout.addView(bottomLayout);

            return itemLayout;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCategoryEmoji(String category) {
        try {
            for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
                if (EXPENSE_CATEGORIES[i].equals(category)) {
                    return CATEGORY_EMOJIS[i];
                }
            }
            return "📦";
        } catch (Exception e) {
            return "📦";
        }
    }

    private void showAddExpenseDialog() {
        try {
            if (selectedTravel == null) {
                Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (getContext() == null) return;

            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);

            EditText editTextTitle = dialogView.findViewById(R.id.editTextExpenseTitle);
            EditText editTextAmount = dialogView.findViewById(R.id.editTextExpenseAmount);
            Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerExpenseCategory);
            EditText editTextDate = dialogView.findViewById(R.id.editTextExpenseDate);
            Button buttonSelectDate = dialogView.findViewById(R.id.buttonSelectDate);
            EditText editTextNote = dialogView.findViewById(R.id.editTextExpenseNote);

            // 카테고리 스피너 설정
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    EXPENSE_CATEGORIES
            );
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(categoryAdapter);

            // 현재 날짜를 기본값으로 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            editTextDate.setText(currentDate);

            // 날짜 선택 버튼
            buttonSelectDate.setOnClickListener(v -> {
                try {
                    Calendar calendar = Calendar.getInstance();
                    DatePickerDialog datePickerDialog = new DatePickerDialog(
                            requireContext(),
                            (view, year, month, dayOfMonth) -> {
                                String selectedDate = String.format(Locale.getDefault(),
                                        "%d-%02d-%02d", year, month + 1, dayOfMonth);
                                editTextDate.setText(selectedDate);
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                    datePickerDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            // 버튼 이벤트
            dialogView.findViewById(R.id.buttonCancelExpense).setOnClickListener(v -> dialog.dismiss());

            dialogView.findViewById(R.id.buttonSaveExpense).setOnClickListener(v -> {
                try {
                    String title = editTextTitle.getText().toString().trim();
                    String amountStr = editTextAmount.getText().toString().trim();
                    String category = (String) spinnerCategory.getSelectedItem();
                    String date = editTextDate.getText().toString().trim();
                    String note = editTextNote.getText().toString().trim();

                    if (title.isEmpty()) {
                        editTextTitle.setError("제목을 입력하세요");
                        return;
                    }

                    if (amountStr.isEmpty()) {
                        editTextAmount.setError("금액을 입력하세요");
                        return;
                    }

                    int amount;
                    try {
                        amount = Integer.parseInt(amountStr);
                        if (amount <= 0) {
                            editTextAmount.setError("올바른 금액을 입력하세요");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        editTextAmount.setError("올바른 금액을 입력하세요");
                        return;
                    }

                    saveExpense(title, amount, category, date, note);
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

    private void saveExpense(String title, int amount, String category, String date, String note) {
        try {
            if (database == null || selectedTravel == null || currentUserId.isEmpty()) return;

            String expenseId = database.child("expenses").push().getKey();
            if (expenseId == null) return;

            Map<String, Object> expenseData = new HashMap<>();
            expenseData.put("travelId", selectedTravel.id);
            expenseData.put("userId", currentUserId);
            expenseData.put("title", title);
            expenseData.put("amount", amount);
            expenseData.put("category", category);
            expenseData.put("date", date);
            expenseData.put("note", note);
            expenseData.put("createdAt", getCurrentDateTime());

            database.child("expenses").child(expenseId).setValue(expenseData)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "지출이 저장되었습니다", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "지출 저장 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSetBudgetDialog() {
        try {
            if (selectedTravel == null) {
                Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (getContext() == null) return;

            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_set_budget, null);

            EditText editTextTotalBudget = dialogView.findViewById(R.id.editTextTotalBudget);
            EditText editTextAccommodationBudget = dialogView.findViewById(R.id.editTextAccommodationBudget);
            EditText editTextTransportBudget = dialogView.findViewById(R.id.editTextTransportBudget);
            EditText editTextFoodBudget = dialogView.findViewById(R.id.editTextFoodBudget);
            EditText editTextTourismBudget = dialogView.findViewById(R.id.editTextTourismBudget);
            EditText editTextOtherBudget = dialogView.findViewById(R.id.editTextOtherBudget);
            TextView textViewCategoryTotal = dialogView.findViewById(R.id.textViewCategoryTotal);

            // 기존 예산 값 표시
            if (totalBudget > 0) {
                editTextTotalBudget.setText(String.valueOf(totalBudget));
            }

            // 기존 카테고리별 예산 값 표시 (0이 아닌 값들만)
            Integer accommodationBudget = categoryBudgets.get("숙박");
            if (accommodationBudget != null && accommodationBudget > 0) {
                editTextAccommodationBudget.setText(String.valueOf(accommodationBudget));
            }

            Integer transportBudget = categoryBudgets.get("교통");
            if (transportBudget != null && transportBudget > 0) {
                editTextTransportBudget.setText(String.valueOf(transportBudget));
            }

            Integer foodBudget = categoryBudgets.get("식비");
            if (foodBudget != null && foodBudget > 0) {
                editTextFoodBudget.setText(String.valueOf(foodBudget));
            }

            Integer tourismBudget = categoryBudgets.get("관광");
            if (tourismBudget != null && tourismBudget > 0) {
                editTextTourismBudget.setText(String.valueOf(tourismBudget));
            }

            Integer otherBudget = categoryBudgets.get("기타");
            if (otherBudget != null && otherBudget > 0) {
                editTextOtherBudget.setText(String.valueOf(otherBudget));
            }

            // 카테고리 예산 합계 실시간 계산
            EditText[] categoryBudgetInputs = {
                    editTextAccommodationBudget, editTextTransportBudget,
                    editTextFoodBudget, editTextTourismBudget, editTextOtherBudget
            };

            // 초기 카테고리 합계 계산
            updateCategoryTotal(categoryBudgetInputs, textViewCategoryTotal);

            for (EditText input : categoryBudgetInputs) {
                if (input != null) {
                    input.addTextChangedListener(new android.text.TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            updateCategoryTotal(categoryBudgetInputs, textViewCategoryTotal);
                        }

                        @Override
                        public void afterTextChanged(android.text.Editable s) {}
                    });
                }
            }

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            // 버튼 이벤트
            dialogView.findViewById(R.id.buttonCancelBudget).setOnClickListener(v -> dialog.dismiss());

            dialogView.findViewById(R.id.buttonSaveBudget).setOnClickListener(v -> {
                try {
                    String totalBudgetStr = editTextTotalBudget.getText().toString().trim();

                    if (totalBudgetStr.isEmpty()) {
                        editTextTotalBudget.setError("총 예산을 입력하세요");
                        return;
                    }

                    int newTotalBudget;
                    try {
                        newTotalBudget = Integer.parseInt(totalBudgetStr);
                        if (newTotalBudget <= 0) {
                            editTextTotalBudget.setError("올바른 예산을 입력하세요");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        editTextTotalBudget.setError("올바른 예산을 입력하세요");
                        return;
                    }

                    saveBudget(newTotalBudget, categoryBudgetInputs);
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "예산 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCategoryTotal(EditText[] inputs, TextView totalView) {
        try {
            if (inputs == null || totalView == null) return;

            int total = 0;
            for (EditText input : inputs) {
                if (input != null) {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            total += Integer.parseInt(text);
                        } catch (NumberFormatException e) {
                            // 무시
                        }
                    }
                }
            }

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
            totalView.setText("₩" + numberFormat.format(total));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveBudget(int newBudget, EditText[] categoryInputs) {
        try {
            if (database == null || selectedTravel == null) return;

            // 현재 선택된 여행 정보 백업
            final String selectedTravelId = selectedTravel.id;
            final int currentPosition = selectedTravelPosition;

            // 카테고리별 예산 수집
            Map<String, Object> budgetData = new HashMap<>();
            budgetData.put("totalBudget", newBudget);

            // 카테고리별 예산 저장
            Map<String, Object> categoryBudgetData = new HashMap<>();
            if (categoryInputs != null && categoryInputs.length == EXPENSE_CATEGORIES.length) {
                for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
                    if (categoryInputs[i] != null) {
                        String budgetStr = categoryInputs[i].getText().toString().trim();
                        int budget = 0;
                        if (!budgetStr.isEmpty()) {
                            try {
                                budget = Integer.parseInt(budgetStr);
                            } catch (NumberFormatException e) {
                                budget = 0;
                            }
                        }
                        categoryBudgetData.put(EXPENSE_CATEGORIES[i], budget);
                        categoryBudgets.put(EXPENSE_CATEGORIES[i], budget);
                    }
                }
            }
            budgetData.put("categoryBudgets", categoryBudgetData);

            // Firebase에 저장
            database.child("travels").child(selectedTravel.id).child("budget").setValue(newBudget)
                    .addOnSuccessListener(aVoid -> {
                        // 카테고리별 예산도 저장
                        database.child("travels").child(selectedTravelId).child("categoryBudgets").setValue(categoryBudgetData)
                                .addOnSuccessListener(aVoid2 -> {
                                    try {
                                        // UI 업데이트를 안전하게 실행
                                        if (getActivity() != null && !getActivity().isFinishing()) {
                                            getActivity().runOnUiThread(() -> {
                                                try {
                                                    // 예산 업데이트
                                                    totalBudget = newBudget;
                                                    if (selectedTravel != null && selectedTravel.id.equals(selectedTravelId)) {
                                                        selectedTravel.budget = newBudget;
                                                    }

                                                    // 여행 리스트에서도 업데이트
                                                    for (TravelItem travel : travelList) {
                                                        if (travel.id.equals(selectedTravelId)) {
                                                            travel.budget = newBudget;
                                                            break;
                                                        }
                                                    }

                                                    // UI 업데이트 (스피너 선택 상태 유지)
                                                    updateBudgetDisplay();

                                                    // 스피너 선택 상태 복원 (약간의 지연을 두어 안정성 확보)
                                                    if (spinnerTravels != null && currentPosition > 0) {
                                                        spinnerTravels.post(() -> {
                                                            try {
                                                                if (currentPosition < spinnerTravels.getAdapter().getCount()) {
                                                                    selectedTravelPosition = currentPosition;
                                                                    spinnerTravels.setSelection(currentPosition);
                                                                }
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        });
                                                    }

                                                    if (getContext() != null) {
                                                        Toast.makeText(getContext(), "예산이 설정되었습니다", Toast.LENGTH_SHORT).show();
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "카테고리 예산 저장 실패", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        try {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "예산 설정 실패", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAllExpenses() {
        try {
            if (selectedTravel == null) {
                Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (recentExpenses.isEmpty()) {
                Toast.makeText(getContext(), "지출 내역이 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            if (getContext() == null) return;

            // 모든 지출 내역을 문자열로 구성
            StringBuilder allExpenses = new StringBuilder();
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

            for (ExpenseItem expense : recentExpenses) {
                allExpenses.append(String.format("%s %s\n",
                        getCategoryEmoji(expense.category),
                        expense.title != null ? expense.title : "제목 없음"));
                allExpenses.append(String.format("₩%s | %s | %s\n",
                        numberFormat.format(expense.amount),
                        expense.category != null ? expense.category : "기타",
                        expense.date != null ? expense.date : "날짜 없음"));
                if (expense.note != null && !expense.note.isEmpty()) {
                    allExpenses.append(String.format("메모: %s\n", expense.note));
                }
                allExpenses.append("\n");
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("전체 지출 내역")
                    .setMessage(allExpenses.toString())
                    .setPositiveButton("확인", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearBudgetData() {
        try {
            totalBudget = 0;
            totalUsed = 0;
            initCategoryExpenses();
            recentExpenses.clear();

            updateBudgetDisplay();
            updateCategoryDisplay();
            updatePieChart();
            updateRecentExpensesDisplay();

            // 선택 해제시에만 스피너 위치 초기화
            if (selectedTravel == null) {
                selectedTravelPosition = 0;
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

    private void loadCategoryBudgets() {
        try {
            if (database == null || selectedTravel == null) return;

            database.child("travels").child(selectedTravel.id).child("categoryBudgets")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                categoryBudgets.clear();

                                // 기본값으로 초기화
                                for (String category : EXPENSE_CATEGORIES) {
                                    categoryBudgets.put(category, 0);
                                }

                                // Firebase에서 저장된 값 불러오기
                                if (dataSnapshot.exists()) {
                                    for (String category : EXPENSE_CATEGORIES) {
                                        Object budgetObj = dataSnapshot.child(category).getValue();
                                        if (budgetObj instanceof Number) {
                                            categoryBudgets.put(category, ((Number) budgetObj).intValue());
                                        }
                                    }
                                }

                                // UI 업데이트가 필요한 경우 (예산 설정 다이얼로그가 열려있는 경우를 대비)
                                // 이 부분은 다이얼로그가 열릴 때 자동으로 처리됩니다.
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // 오류 발생 시 기본값으로 초기화
                            categoryBudgets.clear();
                            for (String category : EXPENSE_CATEGORIES) {
                                categoryBudgets.put(category, 0);
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        try {
            super.onDestroyView();
            // 메모리 누수 방지를 위한 정리
            if (pieChart != null) {
                pieChart.clear();
                pieChart = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 내부 클래스들
    private static class TravelItem {
        String id;
        String title;
        String startDate;
        String endDate;
        int budget;
    }

    private static class ExpenseItem {
        String id;
        String title;
        int amount;
        String category;
        String date;
        String note;
    }
}