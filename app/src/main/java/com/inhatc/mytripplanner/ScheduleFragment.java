package com.inhatc.mytripplanner;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.inhatc.mytripplanner.adapter.TravelAdapter;
import com.inhatc.mytripplanner.adapter.TravelDetailPagerAdapter;
import com.inhatc.mytripplanner.model.Travel;
import com.inhatc.mytripplanner.service.TravelDatabaseService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class ScheduleFragment extends Fragment implements TravelAdapter.OnTravelClickListener {

    private RecyclerView recyclerView;
    private TravelAdapter travelAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddTravel;
    private View emptyView;

    private TravelDatabaseService databaseService;
    private SharedPreferences sharedPreferences;
    private String currentUserId;
    private SimpleDateFormat dateFormat;
    private Travel currentTravel;

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

        initViews(view);
        initServices();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();

        loadTravels();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewTravels);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        fabAddTravel = view.findViewById(R.id.fabAddTravel);
        emptyView = view.findViewById(R.id.emptyView);
        // fabAddPlace는 상세 화면이 표시될 때 초기화됨
    }

    private void initServices() {
        databaseService = TravelDatabaseService.getInstance();
        sharedPreferences = getActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userId", "");
        dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
    }

    private void setupRecyclerView() {
        travelAdapter = new TravelAdapter(getContext());
        travelAdapter.setOnTravelClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(travelAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadTravels);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void setupFab() {
        fabAddTravel.setOnClickListener(v -> showCreateTravelDialog());
    }

    private void loadTravels() {
        if (currentUserId.isEmpty()) {
            showError("사용자 정보를 찾을 수 없습니다.");
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        databaseService.getUserTravels(currentUserId, new TravelDatabaseService.OnDataLoadedListener<List<Travel>>() {
            @Override
            public void onSuccess(List<Travel> travels) {
                swipeRefreshLayout.setRefreshing(false);
                travelAdapter.setTravels(travels);
                updateEmptyView(travels.isEmpty());
            }

            @Override
            public void onFailure(String error) {
                swipeRefreshLayout.setRefreshing(false);
                showError("여행 목록 로드 실패: " + error);
                updateEmptyView(true);
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showCreateTravelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_travel, null);

        EditText editTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editDestination = dialogView.findViewById(R.id.editTextDestination);
        EditText editDescription = dialogView.findViewById(R.id.editTextDescription);
        EditText editStartDate = dialogView.findViewById(R.id.editTextStartDate);
        EditText editEndDate = dialogView.findViewById(R.id.editTextEndDate);

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        // 시작일 선택
        editStartDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        startCalendar.set(year, month, dayOfMonth);
                        editStartDate.setText(dateFormat.format(startCalendar.getTime()));

                        // 종료일이 시작일보다 빠르면 시작일과 같게 설정
                        if (endCalendar.before(startCalendar)) {
                            endCalendar.setTime(startCalendar.getTime());
                            editEndDate.setText(dateFormat.format(endCalendar.getTime()));
                        }
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // 종료일 선택
        editEndDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        endCalendar.set(year, month, dayOfMonth);
                        editEndDate.setText(dateFormat.format(endCalendar.getTime()));
                    },
                    endCalendar.get(Calendar.YEAR),
                    endCalendar.get(Calendar.MONTH),
                    endCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(startCalendar.getTimeInMillis());
            datePickerDialog.show();
        });

        builder.setView(dialogView)
                .setTitle("새 여행 계획")
                .setPositiveButton("생성", (dialog, which) -> {
                    String title = editTitle.getText().toString().trim();
                    String destination = editDestination.getText().toString().trim();
                    String description = editDescription.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(getContext(), "여행 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (destination.isEmpty()) {
                        Toast.makeText(getContext(), "목적지를 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (editStartDate.getText().toString().isEmpty() || editEndDate.getText().toString().isEmpty()) {
                        Toast.makeText(getContext(), "날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createTravel(title, destination, description, startCalendar.getTime(), endCalendar.getTime());
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void createTravel(String title, String destination, String description, Date startDate, Date endDate) {
        Travel travel = new Travel();
        travel.setTitle(title);
        travel.setDestination(destination);
        travel.setDescription(description);
        travel.setStartDate(startDate);
        travel.setEndDate(endDate);
        travel.setUserId(currentUserId);
        travel.setLatitude(0.0); // 나중에 지도에서 설정
        travel.setLongitude(0.0);

        databaseService.createTravel(travel, new TravelDatabaseService.OnDataLoadedListener<String>() {
            @Override
            public void onSuccess(String travelId) {
                travel.setId(travelId);
                travelAdapter.addTravel(travel);
                updateEmptyView(false);
                showSuccess("여행 계획이 생성되었습니다!");
            }

            @Override
            public void onFailure(String error) {
                showError("여행 생성 실패: " + error);
            }
        });
    }

    private void showEditTravelDialog(Travel travel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_travel, null);

        EditText editTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editDestination = dialogView.findViewById(R.id.editTextDestination);
        EditText editDescription = dialogView.findViewById(R.id.editTextDescription);
        EditText editStartDate = dialogView.findViewById(R.id.editTextStartDate);
        EditText editEndDate = dialogView.findViewById(R.id.editTextEndDate);

        // 기존 데이터 설정
        editTitle.setText(travel.getTitle());
        editDestination.setText(travel.getDestination());
        editDescription.setText(travel.getDescription());

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        if (travel.getStartDate() != null) {
            startCalendar.setTime(travel.getStartDate());
            editStartDate.setText(dateFormat.format(travel.getStartDate()));
        }

        if (travel.getEndDate() != null) {
            endCalendar.setTime(travel.getEndDate());
            editEndDate.setText(dateFormat.format(travel.getEndDate()));
        }

        // 날짜 선택 리스너들 (createTravelDialog와 동일)
        editStartDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        startCalendar.set(year, month, dayOfMonth);
                        editStartDate.setText(dateFormat.format(startCalendar.getTime()));

                        if (endCalendar.before(startCalendar)) {
                            endCalendar.setTime(startCalendar.getTime());
                            editEndDate.setText(dateFormat.format(endCalendar.getTime()));
                        }
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        editEndDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        endCalendar.set(year, month, dayOfMonth);
                        editEndDate.setText(dateFormat.format(endCalendar.getTime()));
                    },
                    endCalendar.get(Calendar.YEAR),
                    endCalendar.get(Calendar.MONTH),
                    endCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(startCalendar.getTimeInMillis());
            datePickerDialog.show();
        });

        builder.setView(dialogView)
                .setTitle("여행 계획 수정")
                .setPositiveButton("수정", (dialog, which) -> {
                    travel.setTitle(editTitle.getText().toString().trim());
                    travel.setDestination(editDestination.getText().toString().trim());
                    travel.setDescription(editDescription.getText().toString().trim());
                    travel.setStartDate(startCalendar.getTime());
                    travel.setEndDate(endCalendar.getTime());

                    updateTravel(travel);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateTravel(Travel travel) {
        databaseService.updateTravel(travel, new TravelDatabaseService.OnDataLoadedListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                travelAdapter.updateTravel(travel);
                showSuccess("여행 계획이 수정되었습니다!");
            }

            @Override
            public void onFailure(String error) {
                showError("여행 수정 실패: " + error);
            }
        });
    }

    private void showDeleteConfirmDialog(Travel travel) {
        new AlertDialog.Builder(getContext())
                .setTitle("여행 삭제")
                .setMessage("'" + travel.getTitle() + "' 여행을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteTravel(travel))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteTravel(Travel travel) {
        databaseService.deleteTravel(travel.getId(), new TravelDatabaseService.OnDataLoadedListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                travelAdapter.removeTravel(travel.getId());
                showSuccess("여행이 삭제되었습니다!");

                if (travelAdapter.getItemCount() == 0) {
                    updateEmptyView(true);
                }
            }

            @Override
            public void onFailure(String error) {
                showError("여행 삭제 실패: " + error);
            }
        });
    }

    private void showTravelDetailDialog(Travel travel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_travel_detail, null);

        // 상세 정보 설정
        TextView titleText = dialogView.findViewById(R.id.textViewDetailTitle);
        TextView destinationText = dialogView.findViewById(R.id.textViewDetailDestination);
        TextView dateText = dialogView.findViewById(R.id.textViewDetailDate);
        TextView durationText = dialogView.findViewById(R.id.textViewDetailDuration);
        TextView descriptionText = dialogView.findViewById(R.id.textViewDetailDescription);
        TextView statusText = dialogView.findViewById(R.id.textViewDetailStatus);
        View descriptionLayout = dialogView.findViewById(R.id.layoutDescription);

        titleText.setText(travel.getTitle());
        destinationText.setText(travel.getDestination());

        // 날짜 정보
        if (travel.getStartDate() != null && travel.getEndDate() != null) {
            String startDateStr = dateFormat.format(travel.getStartDate());
            String endDateStr = dateFormat.format(travel.getEndDate());
            dateText.setText(startDateStr + " ~ " + endDateStr);

            long duration = travel.getDurationInDays();
            durationText.setText("⏰ " + duration + "일간의 여행");
        } else {
            dateText.setText("날짜 미정");
            durationText.setText("⏰ 기간 미정");
        }

        // 설명 표시
        if (travel.getDescription() != null && !travel.getDescription().trim().isEmpty()) {
            descriptionLayout.setVisibility(View.VISIBLE);
            descriptionText.setText(travel.getDescription());
        } else {
            descriptionLayout.setVisibility(View.GONE);
        }

        // 여행 상태 설정
        Date currentDate = new Date();
        if (travel.getEndDate() != null && travel.getEndDate().before(currentDate)) {
            statusText.setText("✅ 완료된 여행");
            statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            statusText.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        } else if (travel.getStartDate() != null && travel.getStartDate().before(currentDate) &&
                travel.getEndDate() != null && travel.getEndDate().after(currentDate)) {
            statusText.setText("🎯 진행 중인 여행");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            statusText.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        } else {
            statusText.setText("🎯 예정된 여행");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            statusText.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        }

        builder.setView(dialogView)
                .setPositiveButton("확인", null)
                .setNeutralButton("수정", (dialog, which) -> showEditTravelDialog(travel))
                .show();
    }

    @Override
    public void onEditClick(Travel travel) {
        showEditTravelDialog(travel);
    }

    @Override
    public void onDeleteClick(Travel travel) {
        showDeleteConfirmDialog(travel);
    }

    private void showSuccess(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    // TravelAdapter.OnTravelClickListener 구현
    @Override
    public void onTravelClick(Travel travel) {
        // 여행 클릭 시 상세 일정 화면으로 이동
        showTravelDetailScreen(travel);
    }

    private void showTravelDetailScreen(Travel travel) {
        this.currentTravel = travel; // 현재 여행 설정

        // 메인 리스트 화면 숨기기
        recyclerView.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        // 상세 화면 표시
        View detailContainer = getView().findViewById(R.id.detailContainer);
        if (detailContainer != null) {
            detailContainer.setVisibility(View.VISIBLE);
            setupTravelDetailScreen(travel);
        }

        // FAB 아이콘을 뒤로가기로 변경
        fabAddTravel.setImageResource(R.drawable.ic_arrow_back);
        fabAddTravel.setOnClickListener(v -> hideTravelDetailScreen());
    }

    private void hideTravelDetailScreen() {
        this.currentTravel = null; // 현재 여행 초기화

        // 상세 화면 숨기기
        View detailContainer = getView().findViewById(R.id.detailContainer);
        if (detailContainer != null) {
            detailContainer.setVisibility(View.GONE);
        }

        // 메인 리스트 화면 표시
        recyclerView.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        updateEmptyView(travelAdapter.getItemCount() == 0);

        // FAB 아이콘을 추가로 되돌리기
        fabAddTravel.setImageResource(R.drawable.ic_add);
        fabAddTravel.setOnClickListener(v -> showCreateTravelDialog());
    }

    private void setupTravelDetailScreen(Travel travel) {
        View detailContainer = getView().findViewById(R.id.detailContainer);

        // 여행 제목 설정
        TextView titleText = detailContainer.findViewById(R.id.textViewTravelTitle);
        titleText.setText(travel.getTitle() + " - " + travel.getDestination());

        // 날짜별 탭 설정
        androidx.viewpager2.widget.ViewPager2 viewPager = detailContainer.findViewById(R.id.viewPagerDays);
        com.google.android.material.tabs.TabLayout tabLayout = detailContainer.findViewById(R.id.tabLayoutDays);

        TravelDetailPagerAdapter adapter = new TravelDetailPagerAdapter(this);
        adapter.setTravelDates(travel, travel.getStartDate(), travel.getEndDate());
        viewPager.setAdapter(adapter);

        new com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText("Day " + (position + 1));
        }).attach();
    }

    // 불필요한 메서드 제거
    // private void showAddPlaceForCurrentDay(int dayNumber) {
    //     Toast.makeText(getContext(), "Day " + dayNumber + "에 장소 추가 기능은 각 탭의 + 버튼을 사용해주세요!",
    //             Toast.LENGTH_SHORT).show();
    // }

    @Override
    public void onTravelLongClick(Travel travel) {
        // 길게 눌렀을 때 편집/삭제 메뉴 표시
        new AlertDialog.Builder(getContext())
                .setTitle(travel.getTitle())
                .setItems(new String[]{"수정", "삭제"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditTravelDialog(travel);
                    } else {
                        showDeleteConfirmDialog(travel);
                    }
                })
                .show();
    }
}