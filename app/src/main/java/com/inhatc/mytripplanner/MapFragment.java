package com.inhatc.mytripplanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.inhatc.mytripplanner.model.Travel;
import com.inhatc.mytripplanner.service.TravelDatabaseService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String GOOGLE_API_KEY = "AIzaSyDSOKmlc9jRVf5Nw-Us8OWnsGTDgn-nGHc"; // API 키 추가 필요

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TravelDatabaseService databaseService;

    // UI 컴포넌트들
    private EditText searchEditText;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabClearRoute;

    private Map<Marker, Travel> markerTravelMap;
    private List<Polyline> routePolylines;
    private SimpleDateFormat dateFormat;
    private ExecutorService executorService;

    // 경로 관련
    private LatLng currentLocation;
    private LatLng searchedLocation;
    private Marker searchMarker;

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

        initServices();
        initViews(view);
        setupMap();
        setupSearch();
    }

    private void initServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        databaseService = TravelDatabaseService.getInstance();
        markerTravelMap = new HashMap<>();
        routePolylines = new ArrayList<>();
        dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        executorService = Executors.newSingleThreadExecutor();
    }

    private void initViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        fabMyLocation = view.findViewById(R.id.fabMyLocation);
        fabClearRoute = view.findViewById(R.id.fabClearRoute);

        fabMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        fabClearRoute.setOnClickListener(v -> clearRoute());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapView);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    // 3글자 이상 입력하면 검색 시작 (디바운스 효과)
                    searchEditText.removeCallbacks(searchRunnable);
                    searchEditText.postDelayed(searchRunnable, 1000); // 1초 후 검색
                }
            }
        });
    }

    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchLocation(query);
            }
        }
    };

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // 지도 기본 설정
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // 기본 위치 설정 (서울)
        LatLng seoul = new LatLng(37.5665, 126.9780);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10));

        // 마커 클릭 리스너
        googleMap.setOnMarkerClickListener(marker -> {
            Travel travel = markerTravelMap.get(marker);
            if (travel != null) {
                showTravelInfo(travel);
            } else if (marker == searchMarker) {
                // 검색된 장소로 경로 표시
                if (currentLocation != null) {
                    drawRoute(currentLocation, searchedLocation);
                } else {
                    Toast.makeText(getContext(), "현재 위치를 먼저 확인해주세요", Toast.LENGTH_SHORT).show();
                    moveToCurrentLocation();
                }
            }
            return false;
        });

        // 위치 권한 확인 및 요청
        checkLocationPermission();

        // 여행지 마커 로드
        loadTravelMarkers();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            getCurrentLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableMyLocation() {
        if (googleMap != null && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                }
            }
        });
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null && googleMap != null) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                } else {
                    Toast.makeText(getContext(), "현재 위치를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void searchLocation(String query) {
        executorService.execute(() -> {
            try {
                // 한글 검색어 UTF-8 인코딩
                String encodedQuery = URLEncoder.encode(query, "UTF-8");

                // 한국 지역으로 제한하여 검색 정확도 향상
                String urlString = "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "address=" + encodedQuery +
                        "&region=kr" + // 한국 지역 설정
                        "&language=ko" + // 한국어 응답
                        "&key=" + GOOGLE_API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 10초 타임아웃
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // 메인 스레드에서 결과 처리
                    getActivity().runOnUiThread(() -> parseSearchResult(response.toString(), query));
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "검색 서버 오류: " + responseCode, Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "인터넷 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                    e.printStackTrace(); // 디버깅용
                });
            }
        });
    }

    private void parseSearchResult(String jsonResponse, String originalQuery) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String status = jsonObject.getString("status");

            if ("OK".equals(status)) {
                JSONArray results = jsonObject.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject firstResult = results.getJSONObject(0);
                    JSONObject location = firstResult.getJSONObject("geometry").getJSONObject("location");

                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");
                    String address = firstResult.getString("formatted_address");

                    searchedLocation = new LatLng(lat, lng);

                    // 기존 검색 마커 제거
                    if (searchMarker != null) {
                        searchMarker.remove();
                    }

                    // 새 검색 마커 추가
                    searchMarker = googleMap.addMarker(new MarkerOptions()
                            .position(searchedLocation)
                            .title("🔍 " + originalQuery)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                    // 카메라 이동
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchedLocation, 16));

                    // 현재 위치가 있으면 자동으로 경로 표시
                    if (currentLocation != null) {
                        drawRoute(currentLocation, searchedLocation);
                    } else {
                        Toast.makeText(getContext(), "현재 위치를 가져오는 중입니다...", Toast.LENGTH_SHORT).show();
                        getCurrentLocation();
                    }

                } else {
                    Toast.makeText(getContext(), "'" + originalQuery + "' 검색 결과를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            } else if ("ZERO_RESULTS".equals(status)) {
                Toast.makeText(getContext(), "'" + originalQuery + "' 검색 결과가 없습니다. 다른 키워드를 시도해보세요", Toast.LENGTH_SHORT).show();
            } else if ("OVER_QUERY_LIMIT".equals(status)) {
                Toast.makeText(getContext(), "API 사용량 초과. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show();
            } else if ("REQUEST_DENIED".equals(status)) {
                Toast.makeText(getContext(), "API 키를 확인해주세요", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "검색 오류: " + status, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "검색 결과 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
            e.printStackTrace(); // 디버깅용
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        executorService.execute(() -> {
            try {
                String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin.latitude + "," + origin.longitude +
                        "&destination=" + destination.latitude + "," + destination.longitude +
                        "&key=" + GOOGLE_API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                getActivity().runOnUiThread(() -> parseDirectionsResult(response.toString()));

            } catch (Exception e) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "경로 검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void parseDirectionsResult(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray routes = jsonObject.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                List<LatLng> routePoints = decodePolyline(encodedPolyline);

                // 기존 경로 제거
                clearRoute();

                // 새 경로 그리기
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(routePoints)
                        .width(8)
                        .color(Color.BLUE)
                        .geodesic(true);

                Polyline polyline = googleMap.addPolyline(polylineOptions);
                routePolylines.add(polyline);

                Toast.makeText(getContext(), "경로가 표시되었습니다", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getContext(), "경로를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "경로 처리 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng position = new LatLng(lat / 1E5, lng / 1E5);
            polyline.add(position);
        }
        return polyline;
    }

    private void clearRoute() {
        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();

        if (searchMarker != null) {
            searchMarker.remove();
            searchMarker = null;
        }

        searchEditText.setText("");
        Toast.makeText(getContext(), "경로가 삭제되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void loadTravelMarkers() {
        databaseService.getAllTravelsWithLocation(new TravelDatabaseService.OnDataLoadedListener<List<Travel>>() {
            @Override
            public void onSuccess(List<Travel> travels) {
                addTravelMarkers(travels);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "여행지 로드 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTravelMarkers(List<Travel> travels) {
        for (Travel travel : travels) {
            if (travel.hasValidLocation()) {
                LatLng position = new LatLng(travel.getLatitude(), travel.getLongitude());

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title("✈️ " + travel.getTitle())
                        .snippet(travel.getDestination())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                Marker marker = googleMap.addMarker(markerOptions);
                markerTravelMap.put(marker, travel);
            }
        }
    }

    private void showTravelInfo(Travel travel) {
        String message = "✈️ " + travel.getTitle() + "\n" +
                "📍 " + travel.getDestination();

        if (travel.getStartDate() != null && travel.getEndDate() != null) {
            message += "\n📅 " + dateFormat.format(travel.getStartDate()) +
                    " ~ " + dateFormat.format(travel.getEndDate());
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}