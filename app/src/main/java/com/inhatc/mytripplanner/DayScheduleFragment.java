// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/DayScheduleFragment.java
package com.inhatc.mytripplanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.inhatc.mytripplanner.adapter.PlaceAdapter;
import com.inhatc.mytripplanner.model.Place;
import com.inhatc.mytripplanner.service.TravelDatabaseService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayScheduleFragment extends Fragment implements PlaceAdapter.OnPlaceClickListener {

    private static final String ARG_TRAVEL_ID = "travel_id";
    private static final String ARG_SCHEDULE_DATE = "schedule_date";
    private static final String ARG_DAY_NUMBER = "day_number";

    private String travelId;
    private Date scheduleDate;
    private int dayNumber;

    // UI 컴포넌트들 (기존 디자인 유지)
    private TextView textViewDayInfo;
    private RecyclerView recyclerViewPlaces;
    private PlaceAdapter placeAdapter;
    private FloatingActionButton fabAddPlace;
    private View emptyView;

    private TravelDatabaseService databaseService;
    private SimpleDateFormat dateFormat;

    public static DayScheduleFragment newInstance(String travelId, Date scheduleDate, int dayNumber) {
        DayScheduleFragment fragment = new DayScheduleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRAVEL_ID, travelId);
        args.putSerializable(ARG_SCHEDULE_DATE, scheduleDate);
        args.putInt(ARG_DAY_NUMBER, dayNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            travelId = getArguments().getString(ARG_TRAVEL_ID);
            scheduleDate = (Date) getArguments().getSerializable(ARG_SCHEDULE_DATE);
            dayNumber = getArguments().getInt(ARG_DAY_NUMBER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_day_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initServices();
        initViews(view);
        setupRecyclerView();
        setupDayInfo();
        loadPlaces();
    }

    private void initServices() {
        databaseService = TravelDatabaseService.getInstance();
        dateFormat = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
    }

    private void initViews(View view) {
        textViewDayInfo = view.findViewById(R.id.textViewDayInfo);
        recyclerViewPlaces = view.findViewById(R.id.recyclerViewPlaces);
        fabAddPlace = view.findViewById(R.id.fabAddPlace);
        emptyView = view.findViewById(R.id.emptyView);

        fabAddPlace.setOnClickListener(v -> showAddPlaceDialog());
    }

    private void setupRecyclerView() {
        placeAdapter = new PlaceAdapter(getContext());
        placeAdapter.setOnPlaceClickListener(this);

        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPlaces.setAdapter(placeAdapter);
    }

    private void setupDayInfo() {
        String dayText = "Day " + dayNumber;
        if (scheduleDate != null) {
            dayText += " - " + dateFormat.format(scheduleDate);
        }
        textViewDayInfo.setText(dayText);
    }

    private void loadPlaces() {
        // 임시로 빈 목록 설정 (실제로는 데이터베이스에서 로드)
        List<Place> places = new ArrayList<>();
        placeAdapter.setPlaces(places);
        updateEmptyView(places.isEmpty());
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerViewPlaces.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerViewPlaces.setVisibility(View.VISIBLE);
        }
    }

    private void showAddPlaceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_place, null);

        EditText editPlaceName = dialogView.findViewById(R.id.editTextPlaceName);
        EditText editPlaceAddress = dialogView.findViewById(R.id.editTextPlaceAddress);
        EditText editPlaceNotes = dialogView.findViewById(R.id.editTextPlaceNotes);

        builder.setView(dialogView)
                .setTitle("새 장소 추가")
                .setPositiveButton("추가", (dialog, which) -> {
                    String placeName = editPlaceName.getText().toString().trim();
                    String placeAddress = editPlaceAddress.getText().toString().trim();
                    String placeNotes = editPlaceNotes.getText().toString().trim();

                    if (placeName.isEmpty()) {
                        Toast.makeText(getContext(), "장소명을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createPlace(placeName, placeAddress, placeNotes);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void createPlace(String placeName, String placeAddress, String placeNotes) {
        Place place = new Place();
        place.setName(placeName);
        place.setAddress(placeAddress);
        place.setNotes(placeNotes);
        place.setTravelId(travelId);
        place.setScheduleDate(scheduleDate);
        place.setDayNumber(dayNumber);
        place.setOrderIndex(placeAdapter.getItemCount() + 1);

        // 리스트에 추가
        placeAdapter.addPlace(place);
        updateEmptyView(false);

        Toast.makeText(getContext(), "장소가 추가되었습니다!", Toast.LENGTH_SHORT).show();
    }

    // PlaceAdapter.OnPlaceClickListener 구현
    @Override
    public void onPlaceClick(Place place) {
        showPlaceDetailDialog(place);
    }

    @Override
    public void onPlaceLongClick(Place place) {
        new AlertDialog.Builder(getContext())
                .setTitle(place.getName())
                .setItems(new String[]{"수정", "삭제"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditPlaceDialog(place);
                    } else {
                        showDeletePlaceDialog(place);
                    }
                })
                .show();
    }

    @Override
    public void onPlaceOrderChange(Place place, int newOrder) {
        // TODO: 순서 변경 처리
        Toast.makeText(getContext(), "순서가 변경되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void showPlaceDetailDialog(Place place) {
        String message = "📍 " + place.getName();
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            message += "\n🏠 " + place.getAddress();
        }
        if (place.getNotes() != null && !place.getNotes().isEmpty()) {
            message += "\n📝 " + place.getNotes();
        }

        new AlertDialog.Builder(getContext())
                .setTitle("장소 정보")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    private void showEditPlaceDialog(Place place) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_place, null);

        EditText editPlaceName = dialogView.findViewById(R.id.editTextPlaceName);
        EditText editPlaceAddress = dialogView.findViewById(R.id.editTextPlaceAddress);
        EditText editPlaceNotes = dialogView.findViewById(R.id.editTextPlaceNotes);

        // 기존 데이터 설정
        editPlaceName.setText(place.getName());
        editPlaceAddress.setText(place.getAddress());
        editPlaceNotes.setText(place.getNotes());

        builder.setView(dialogView)
                .setTitle("장소 수정")
                .setPositiveButton("수정", (dialog, which) -> {
                    place.setName(editPlaceName.getText().toString().trim());
                    place.setAddress(editPlaceAddress.getText().toString().trim());
                    place.setNotes(editPlaceNotes.getText().toString().trim());

                    placeAdapter.updatePlace(place);
                    Toast.makeText(getContext(), "장소가 수정되었습니다!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeletePlaceDialog(Place place) {
        new AlertDialog.Builder(getContext())
                .setTitle("장소 삭제")
                .setMessage("'" + place.getName() + "' 장소를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    placeAdapter.removePlace(place);
                    updateEmptyView(placeAdapter.getItemCount() == 0);
                    Toast.makeText(getContext(), "장소가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }
}