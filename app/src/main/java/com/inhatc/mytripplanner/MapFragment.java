package com.inhatc.mytripplanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MapFragment extends Fragment {

    public MapFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 초기화
        TextView titleText = view.findViewById(R.id.textViewTitle);
        TextView subtitleText = view.findViewById(R.id.textViewSubtitle);
        TextView descriptionText = view.findViewById(R.id.textViewDescription);

        titleText.setText("지도");
        subtitleText.setText("여행지 위치 확인");
        descriptionText.setText("Google Maps를 활용한 여행지 위치 확인 기능입니다.\n경로 탐색, 주변 관광지, 교통편 정보 등을 제공할 예정입니다.");
    }
}