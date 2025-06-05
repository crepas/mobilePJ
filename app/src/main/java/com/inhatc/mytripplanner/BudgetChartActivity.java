package com.inhatc.mytripplanner;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetChartActivity extends AppCompatActivity {

    private DatabaseReference database;
    private String travelId;
    private String travelTitle;

    private TextView textViewChartTitle;
    private TextView textViewTotalExpense;

    // 간단한 원형 차트를 위한 뷰들
    private TextView textViewAccommodationChart;
    private TextView textViewTransportChart;
    private TextView textViewFoodChart;
    private TextView textViewTourismChart;
    private TextView textViewOtherChart;

    private Map<String, Integer> categoryExpenses = new HashMap<>();
    private final String[] EXPENSE_CATEGORIES = {"숙박", "교통", "식비", "관광", "기타"};
    private final String[] CATEGORY_EMOJIS = {"🏨", "🚗", "🍽️", "🎭", "📦"};
    private final int[] CATEGORY_COLORS = {
            Color.parseColor("#FF5722"), // 숙박 - 빨강
            Color.parseColor("#2196F3"), // 교통 - 파랑
            Color.parseColor("#4CAF50"), // 식비 - 초록
            Color.parseColor("#FF9800"), // 관광 - 주황
            Color.parseColor("#9C27B0")  // 기타 - 보라
    };

    private int totalExpense = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_chart);

        // Intent에서 데이터 받기
        Intent intent = getIntent();
        travelId = intent.getStringExtra("travelId");
        travelTitle = intent.getStringExtra("travelTitle");

        if (travelId == null || travelId.isEmpty()) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance().getReference();
        initViews();
        initCategoryExpenses();
        loadExpenseData();
    }

    private void initViews() {
        textViewChartTitle = findViewById(R.id.textViewChartTitle);
        textViewTotalExpense = findViewById(R.id.textViewTotalExpense);
        textViewAccommodationChart = findViewById(R.id.textViewAccommodationChart);
        textViewTransportChart = findViewById(R.id.textViewTransportChart);
        textViewFoodChart = findViewById(R.id.textViewFoodChart);
        textViewTourismChart = findViewById(R.id.textViewTourismChart);
        textViewOtherChart = findViewById(R.id.textViewOtherChart);

        if (travelTitle != null) {
            textViewChartTitle.setText(travelTitle + " - 지출 분석");
        }
    }

    private void initCategoryExpenses() {
        for (String category : EXPENSE_CATEGORIES) {
            categoryExpenses.put(category, 0);
        }
    }

    private void loadExpenseData() {
        database.child("expenses")
                .orderByChild("travelId")
                .equalTo(travelId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // 초기화
                        initCategoryExpenses();
                        totalExpense = 0;

                        for (DataSnapshot expenseSnapshot : dataSnapshot.getChildren()) {
                            String category = expenseSnapshot.child("category").getValue(String.class);
                            Object amountObj = expenseSnapshot.child("amount").getValue();

                            int amount = 0;
                            if (amountObj instanceof Number) {
                                amount = ((Number) amountObj).intValue();
                            }

                            if (category != null && categoryExpenses.containsKey(category)) {
                                int currentAmount = categoryExpenses.get(category);
                                categoryExpenses.put(category, currentAmount + amount);
                            }

                            totalExpense += amount;
                        }

                        updateChartDisplay();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(BudgetChartActivity.this,
                                "데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateChartDisplay() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        textViewTotalExpense.setText("총 지출: ₩" + numberFormat.format(totalExpense));

        TextView[] chartViews = {
                textViewAccommodationChart, textViewTransportChart,
                textViewFoodChart, textViewTourismChart, textViewOtherChart
        };

        for (int i = 0; i < EXPENSE_CATEGORIES.length; i++) {
            String category = EXPENSE_CATEGORIES[i];
            int amount = categoryExpenses.get(category);
            double percentage = totalExpense > 0 ? (amount * 100.0 / totalExpense) : 0;

            String chartText = String.format("%s %s\n₩%s (%.1f%%)",
                    CATEGORY_EMOJIS[i], category,
                    numberFormat.format(amount), percentage);

            chartViews[i].setText(chartText);
            chartViews[i].setBackgroundColor(CATEGORY_COLORS[i]);
            chartViews[i].setTextColor(Color.WHITE);
            chartViews[i].setPadding(16, 16, 16, 16);

            // 지출이 있는 카테고리만 진하게 표시
            if (amount > 0) {
                chartViews[i].setAlpha(1.0f);
            } else {
                chartViews[i].setAlpha(0.3f);
            }
        }
    }
}