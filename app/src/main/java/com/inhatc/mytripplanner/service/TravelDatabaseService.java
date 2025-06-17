package com.inhatc.mytripplanner.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.inhatc.mytripplanner.model.Travel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TravelDatabaseService {

    private static final String TRAVELS_NODE = "travels";
    private DatabaseReference database;
    private static TravelDatabaseService instance;
    private SimpleDateFormat dateFormat;

    public interface OnDataLoadedListener<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private TravelDatabaseService() {
        database = FirebaseDatabase.getInstance().getReference();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    public static synchronized TravelDatabaseService getInstance() {
        if (instance == null) {
            instance = new TravelDatabaseService();
        }
        return instance;
    }

    // 여행 생성
    public void createTravel(Travel travel, OnDataLoadedListener<String> listener) {
        String travelId = database.child(TRAVELS_NODE).push().getKey();

        if (travelId == null) {
            listener.onFailure("여행 ID 생성에 실패했습니다.");
            return;
        }

        travel.setId(travelId);
        travel.setCreatedAt(new Date());
        travel.setUpdatedAt(new Date());

        Map<String, Object> travelData = travelToMap(travel);

        database.child(TRAVELS_NODE).child(travelId).setValue(travelData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(travelId);
                    } else {
                        String errorMessage = "여행 생성에 실패했습니다.";
                        if (task.getException() != null) {
                            errorMessage += " " + task.getException().getMessage();
                        }
                        listener.onFailure(errorMessage);
                    }
                });
    }

    // 사용자별 여행 목록 조회
    public void getUserTravels(String userId, OnDataLoadedListener<List<Travel>> listener) {
        Query query = database.child(TRAVELS_NODE).orderByChild("userId").equalTo(userId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Travel> travels = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Travel travel = mapToTravel(snapshot);
                    if (travel != null) {
                        travels.add(travel);
                    }
                }

                // 날짜순으로 정렬 (최신순)
                travels.sort((t1, t2) -> {
                    if (t1.getStartDate() == null) return 1;
                    if (t2.getStartDate() == null) return -1;
                    return t2.getStartDate().compareTo(t1.getStartDate());
                });

                listener.onSuccess(travels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure("데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    // 특정 여행 조회
    public void getTravel(String travelId, OnDataLoadedListener<Travel> listener) {
        database.child(TRAVELS_NODE).child(travelId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Travel travel = mapToTravel(dataSnapshot);
                    if (travel != null) {
                        listener.onSuccess(travel);
                    } else {
                        listener.onFailure("여행 데이터 변환에 실패했습니다.");
                    }
                } else {
                    listener.onFailure("여행 정보를 찾을 수 없습니다.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure("데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    // 여행 수정
    public void updateTravel(Travel travel, OnDataLoadedListener<Void> listener) {
        travel.setUpdatedAt(new Date());
        Map<String, Object> travelData = travelToMap(travel);

        database.child(TRAVELS_NODE).child(travel.getId()).setValue(travelData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(null);
                    } else {
                        String errorMessage = "여행 수정에 실패했습니다.";
                        if (task.getException() != null) {
                            errorMessage += " " + task.getException().getMessage();
                        }
                        listener.onFailure(errorMessage);
                    }
                });
    }

    // 여행 삭제
    public void deleteTravel(String travelId, OnDataLoadedListener<Void> listener) {
        database.child(TRAVELS_NODE).child(travelId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess(null);
                    } else {
                        String errorMessage = "여행 삭제에 실패했습니다.";
                        if (task.getException() != null) {
                            errorMessage += " " + task.getException().getMessage();
                        }
                        listener.onFailure(errorMessage);
                    }
                });
    }

    // 위치가 있는 모든 여행 조회 (지도용)
    public void getAllTravelsWithLocation(OnDataLoadedListener<List<Travel>> listener) {
        database.child(TRAVELS_NODE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Travel> travels = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Travel travel = mapToTravel(snapshot);
                    if (travel != null && travel.hasValidLocation()) {
                        travels.add(travel);
                    }
                }

                listener.onSuccess(travels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure("데이터 로드 실패: " + databaseError.getMessage());
            }
        });
    }

    // Travel 객체를 Map으로 변환 (Firebase 저장용)
    private Map<String, Object> travelToMap(Travel travel) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", travel.getId());
        map.put("title", travel.getTitle());
        map.put("destination", travel.getDestination());
        map.put("description", travel.getDescription());
        map.put("latitude", travel.getLatitude());
        map.put("longitude", travel.getLongitude());
        map.put("userId", travel.getUserId());

        // 날짜를 문자열로 변환하여 저장
        if (travel.getStartDate() != null) {
            map.put("startDate", dateFormat.format(travel.getStartDate()));
        }
        if (travel.getEndDate() != null) {
            map.put("endDate", dateFormat.format(travel.getEndDate()));
        }
        if (travel.getCreatedAt() != null) {
            map.put("createdAt", dateFormat.format(travel.getCreatedAt()));
        }
        if (travel.getUpdatedAt() != null) {
            map.put("updatedAt", dateFormat.format(travel.getUpdatedAt()));
        }

        return map;
    }

    // DataSnapshot을 Travel 객체로 변환
    private Travel mapToTravel(DataSnapshot snapshot) {
        try {
            Travel travel = new Travel();
            travel.setId(snapshot.child("id").getValue(String.class));
            travel.setTitle(snapshot.child("title").getValue(String.class));
            travel.setDestination(snapshot.child("destination").getValue(String.class));
            travel.setDescription(snapshot.child("description").getValue(String.class));
            travel.setUserId(snapshot.child("userId").getValue(String.class));

            // Double 값 처리
            Double latitude = snapshot.child("latitude").getValue(Double.class);
            if (latitude != null) travel.setLatitude(latitude);

            Double longitude = snapshot.child("longitude").getValue(Double.class);
            if (longitude != null) travel.setLongitude(longitude);

            // 날짜 문자열을 Date 객체로 변환
            String startDateStr = snapshot.child("startDate").getValue(String.class);
            if (startDateStr != null) {
                try {
                    travel.setStartDate(dateFormat.parse(startDateStr));
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 null로 설정
                }
            }

            String endDateStr = snapshot.child("endDate").getValue(String.class);
            if (endDateStr != null) {
                try {
                    travel.setEndDate(dateFormat.parse(endDateStr));
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 null로 설정
                }
            }

            String createdAtStr = snapshot.child("createdAt").getValue(String.class);
            if (createdAtStr != null) {
                try {
                    travel.setCreatedAt(dateFormat.parse(createdAtStr));
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 null로 설정
                }
            }

            String updatedAtStr = snapshot.child("updatedAt").getValue(String.class);
            if (updatedAtStr != null) {
                try {
                    travel.setUpdatedAt(dateFormat.parse(updatedAtStr));
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 null로 설정
                }
            }

            return travel;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}