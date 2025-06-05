package com.inhatc.mytripplanner;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
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

import static android.content.Context.MODE_PRIVATE;

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
    private Button buttonViewChart;
    private Button buttonViewAllExpenses;

    // Firebase 및 사용자 정보
    private DatabaseReference database;
    private SharedPreferences sharedPreferences;
    private String currentUserId;

    // 데이터
    private List<TravelItem> travelList = new ArrayList<>();
    private TravelItem selectedTravel;
    private Map<String, Integer> categoryExpenses = new HashMap<>();
    private List<ExpenseItem> recentExpenses = new ArrayList<>();
    private int totalBudget = 0;
    private int totalUsed = 0;

    // 카테고리 목록
    private final String[] EXPENSE_CATEGORIES = {"숙박", "교통", "식비", "관광", "기타"};
    private final String[] CATEGORY_EMOJIS = {"🏨", "🚗", "🍽️", "🎭", "📦"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

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

        return view;
    }

    private void initViews(View view) {
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
        buttonViewChart = view.findViewById(R.id.buttonViewChart);
        buttonViewAllExpenses = view.findViewById(R.id.buttonViewAllExpenses);
    }

    private void initCategoryExpenses() {
        for (String category : EXPENSE_CATEGORIES) {
            categoryExpenses.put(category, 0);
        }
    }

    private void setClickListeners() {
        // 여행 선택 스피너
        spinnerTravels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= travelList.size()) {
                    selectedTravel = travelList.get(position - 1);
                    loadBudgetData();
                    loadExpenseData();
                } else {
                    selectedTravel = null;
                    clearBudgetData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTravel = null;
                clearBudgetData();
            }
        });

        // 지출 추가 버튼
        buttonAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        // 예산 설정 버튼
        buttonSetBudget.setOnClickListener(v -> showSetBudgetDialog());

        // 차트 보기 버튼
        buttonViewChart.setOnClickListener(v -> showExpenseChart());

        // 전체 지출 보기 버튼
        buttonViewAllExpenses.setOnClickListener(v -> showAllExpenses());
    }

    private void loadTravelData() {
        database.child("travels")
                .orderByChild("userId")
                .equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
                            }

                            travelList.add(travel);
                        }

                        updateTravelSpinner();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), "여행 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateTravelSpinner() {
        List<String> travelTitles = new ArrayList<>();
        travelTitles.add("여행을 선택하세요");

        for (TravelItem travel : travelList) {
            String title = travel.title;
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
    }

    private void loadBudgetData() {
        if (selectedTravel == null) return;

        totalBudget = selectedTravel.budget;
        updateBudgetDisplay();
    }

    private void loadExpenseData() {
        if (selectedTravel == null) return;

        database.child("expenses")
                .orderByChild("travelId")
                .equalTo(selectedTravel.id)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
                        updateRecentExpensesDisplay();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), "지출 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateBudgetDisplay() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

        textViewTotalBudget.setText("₩" + numberFormat.format(totalBudget));
        textViewUsedAmount.setText("₩" + numberFormat.format(totalUsed));

        int remaining = totalBudget - totalUsed;
        textViewRemainingBudget.setText("₩" + numberFormat.format(remaining));

        // 진행률 계산
        int progress = 0;
        if (totalBudget > 0) {
            progress = Math.min((totalUsed * 100) / totalBudget, 100);
        }

        progressBarBudget.setProgress(progress);
        textViewBudgetProgress.setText(progress + "%");

        // 예산 초과 경고
        if (totalUsed > totalBudget && totalBudget > 0) {
            layoutBudgetWarning.setVisibility(View.VISIBLE);
        } else {
            layoutBudgetWarning.setVisibility(View.GONE);
        }

        // 진행률에 따른 색상 변경
        if (progress >= 100) {
            progressBarBudget.setProgressTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        } else if (progress >= 80) {
            progressBarBudget.setProgressTintList(getResources().getColorStateList(android.R.color.holo_orange_dark));
        } else {
            progressBarBudget.setProgressTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
        }
    }

    private void updateCategoryDisplay() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

        textViewAccommodationAmount.setText("₩" + numberFormat.format(categoryExpenses.get("숙박")));
        textViewTransportAmount.setText("₩" + numberFormat.format(categoryExpenses.get("교통")));
        textViewFoodAmount.setText("₩" + numberFormat.format(categoryExpenses.get("식비")));
        textViewTourismAmount.setText("₩" + numberFormat.format(categoryExpenses.get("관광")));
        textViewOtherAmount.setText("₩" + numberFormat.format(categoryExpenses.get("기타")));
    }

    private void updateRecentExpensesDisplay() {
        layoutRecentExpenses.removeAllViews();

        if (recentExpenses.isEmpty()) {
            layoutRecentExpenses.addView(textViewNoExpenses);
        } else {
            // 최대 5개까지만 표시
            int maxDisplay = Math.min(recentExpenses.size(), 5);
            for (int i = 0; i < maxDisplay; i++) {
                View expenseView = createExpenseItemView(recentExpenses.get(i));
                layoutRecentExpenses.addView(expenseView);
            }
        }
    }

    private View createExpenseItemView(ExpenseItem expense) {
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
    }

    private String getCategoryEmoji(String category) {
        for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
            if (EXPENSE_CATEGORIES[i].equals(category)) {
                return CATEGORY_EMOJIS[i];
            }
        }
        return "📦";
    }

    private void showAddExpenseDialog() {
        if (selectedTravel == null) {
            Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

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
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // 버튼 이벤트
        dialogView.findViewById(R.id.buttonCancelExpense).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.buttonSaveExpense).setOnClickListener(v -> {
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
        });

        dialog.show();
    }

    private void saveExpense(String title, int amount, String category, String date, String note) {
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
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "지출이 저장되었습니다", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "지출 저장 실패", Toast.LENGTH_SHORT).show());
    }

    private void showSetBudgetDialog() {
        if (selectedTravel == null) {
            Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

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

        // 카테고리 예산 합계 실시간 계산
        EditText[] categoryBudgetInputs = {
                editTextAccommodationBudget, editTextTransportBudget,
                editTextFoodBudget, editTextTourismBudget, editTextOtherBudget
        };

        for (EditText input : categoryBudgetInputs) {
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // 버튼 이벤트
        dialogView.findViewById(R.id.buttonCancelBudget).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.buttonSaveBudget).setOnClickListener(v -> {
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

            saveBudget(newTotalBudget);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateCategoryTotal(EditText[] inputs, TextView totalView) {
        int total = 0;
        for (EditText input : inputs) {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                try {
                    total += Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        totalView.setText("₩" + numberFormat.format(total));
    }

    private void saveBudget(int newBudget) {
        database.child("travels").child(selectedTravel.id).child("budget").setValue(newBudget)
                .addOnSuccessListener(aVoid -> {
                    totalBudget = newBudget;
                    selectedTravel.budget = newBudget;
                    updateBudgetDisplay();
                    Toast.makeText(getContext(), "예산이 설정되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "예산 설정 실패", Toast.LENGTH_SHORT).show());
    }

    private void showExpenseChart() {
        if (selectedTravel == null) {
            Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 간단한 차트 정보를 대화상자로 표시
        StringBuilder chartInfo = new StringBuilder();
        chartInfo.append("📊 카테고리별 지출 현황\n\n");

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

        for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
            String category = EXPENSE_CATEGORIES[i];
            int amount = categoryExpenses.get(category);
            double percentage = totalUsed > 0 ? (amount * 100.0 / totalUsed) : 0;

            chartInfo.append(String.format("%s %s: ₩%s (%.1f%%)\n",
                    CATEGORY_EMOJIS[i], category,
                    numberFormat.format(amount), percentage));
        }

        chartInfo.append(String.format("\n💰 총 지출: ₩%s", numberFormat.format(totalUsed)));

        new AlertDialog.Builder(requireContext())
                .setTitle("지출 차트")
                .setMessage(chartInfo.toString())
                .setPositiveButton("확인", null)
                .show();
    }

    private void showAllExpenses() {
        if (selectedTravel == null) {
            Toast.makeText(getContext(), "먼저 여행을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recentExpenses.isEmpty()) {
            Toast.makeText(getContext(), "지출 내역이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 모든 지출 내역을 문자열로 구성
        StringBuilder allExpenses = new StringBuilder();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

        for (ExpenseItem expense : recentExpenses) {
            allExpenses.append(String.format("%s %s\n",
                    getCategoryEmoji(expense.category), expense.title));
            allExpenses.append(String.format("₩%s | %s | %s\n",
                    numberFormat.format(expense.amount),
                    expense.category, expense.date));
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
    }

    private void clearBudgetData() {
        totalBudget = 0;
        totalUsed = 0;
        initCategoryExpenses();
        recentExpenses.clear();

        updateBudgetDisplay();
        updateCategoryDisplay();
        updateRecentExpensesDisplay();
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
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