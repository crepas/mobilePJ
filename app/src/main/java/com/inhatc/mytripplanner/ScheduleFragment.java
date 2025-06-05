package com.inhatc.mytripplanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ScheduleFragment extends Fragment {

    public ScheduleFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 초기화
        TextView titleText = view.findViewById(R.id.textViewTitle);
        TextView subtitleText = view.findViewById(R.id.textViewSubtitle);
        TextView descriptionText = view.findViewById(R.id.textViewDescription);

        titleText.setText("일정");
        subtitleText.setText("여행 일정 관리");
        descriptionText.setText("날짜별 여행 계획을 세우고 관리할 수 있습니다.\n일정 추가, 수정, 시간 관리 등의 기능이 제공될 예정입니다.");
    }
}