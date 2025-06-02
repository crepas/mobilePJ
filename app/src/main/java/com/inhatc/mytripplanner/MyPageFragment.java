package com.inhatc.mytripplanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MyPageFragment extends Fragment {

    public MyPageFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mypage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 초기화
        TextView titleText = view.findViewById(R.id.textViewTitle);
        TextView subtitleText = view.findViewById(R.id.textViewSubtitle);
        TextView descriptionText = view.findViewById(R.id.textViewDescription);

        titleText.setText("마이페이지");
        subtitleText.setText("개인 정보 및 설정");
        descriptionText.setText("개인 정보 수정, 여행 통계, 앱 설정 등을 관리할 수 있습니다.\n로그아웃, 회원 탈퇴, 알림 설정 등의 기능도 포함될 예정입니다.");
    }
}