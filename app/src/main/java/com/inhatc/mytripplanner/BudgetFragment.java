package com.inhatc.mytripplanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BudgetFragment extends Fragment {

    public BudgetFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 초기화
        TextView titleText = view.findViewById(R.id.textViewTitle);
        TextView subtitleText = view.findViewById(R.id.textViewSubtitle);
        TextView descriptionText = view.findViewById(R.id.textViewDescription);

        titleText.setText("예산");
        subtitleText.setText("여행 예산 관리");
        descriptionText.setText("여행 예산을 계획하고 지출을 관리할 수 있습니다.\n카테고리별 예산 설정, 실시간 지출 추적 등의 기능이 제공될 예정입니다.");
    }
}