// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/adapter/PlaceAdapter.java
package com.inhatc.mytripplanner.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inhatc.mytripplanner.R;
import com.inhatc.mytripplanner.model.Place;

import java.util.ArrayList;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    private List<Place> places;
    private Context context;
    private OnPlaceClickListener onPlaceClickListener;

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
        void onPlaceLongClick(Place place);
        void onPlaceOrderChange(Place place, int newOrder);
    }

    public PlaceAdapter(Context context) {
        this.context = context;
        this.places = new ArrayList<>();
    }

    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.onPlaceClickListener = listener;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
        notifyDataSetChanged();
    }

    public void addPlace(Place place) {
        places.add(place);
        notifyItemInserted(places.size() - 1);
    }

    public void removePlace(Place place) {
        int position = places.indexOf(place);
        if (position != -1) {
            places.remove(position);
            notifyItemRemoved(position);
            // 순서 재정렬
            updateOrderIndexes();
        }
    }

    public void updatePlace(Place updatedPlace) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId() != null &&
                    places.get(i).getId().equals(updatedPlace.getId())) {
                places.set(i, updatedPlace);
                notifyItemChanged(i);
                break;
            }
        }
    }

    private void updateOrderIndexes() {
        for (int i = 0; i < places.size(); i++) {
            places.get(i).setOrderIndex(i + 1);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewOrderIndex;
        private TextView textViewPlaceName;
        private TextView textViewPlaceAddress;
        private TextView textViewPlaceNotes;
        private CheckBox checkBoxVisited;
        private TextView textViewEditIcon;
        private TextView textViewDeleteIcon;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewOrderIndex = itemView.findViewById(R.id.textViewOrderIndex);
            textViewPlaceName = itemView.findViewById(R.id.textViewPlaceName);
            textViewPlaceAddress = itemView.findViewById(R.id.textViewPlaceAddress);
            textViewPlaceNotes = itemView.findViewById(R.id.textViewPlaceNotes);
            checkBoxVisited = itemView.findViewById(R.id.checkBoxVisited);
            textViewEditIcon = itemView.findViewById(R.id.textViewEditIcon);
            textViewDeleteIcon = itemView.findViewById(R.id.textViewDeleteIcon);

            setupClickListeners();
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                if (onPlaceClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaceClickListener.onPlaceClick(places.get(position));
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (onPlaceClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaceClickListener.onPlaceLongClick(places.get(position));
                    }
                }
                return true;
            });

            textViewEditIcon.setOnClickListener(v -> {
                if (onPlaceClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaceClickListener.onPlaceLongClick(places.get(position));
                    }
                }
            });

            textViewDeleteIcon.setOnClickListener(v -> {
                if (onPlaceClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaceClickListener.onPlaceLongClick(places.get(position));
                    }
                }
            });

            checkBoxVisited.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    places.get(position).setVisited(isChecked);
                    updateVisitedStyle(isChecked);
                }
            });
        }

        public void bind(Place place) {
            // 순서 번호
            textViewOrderIndex.setText(String.valueOf(place.getOrderIndex()));

            // 장소명
            textViewPlaceName.setText(place.getName());

            // 주소
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                textViewPlaceAddress.setVisibility(View.VISIBLE);
                textViewPlaceAddress.setText(place.getAddress());
            } else {
                textViewPlaceAddress.setVisibility(View.GONE);
            }

            // 메모
            if (place.getNotes() != null && !place.getNotes().isEmpty()) {
                textViewPlaceNotes.setVisibility(View.VISIBLE);
                textViewPlaceNotes.setText(place.getNotes());
            } else {
                textViewPlaceNotes.setVisibility(View.GONE);
            }

            // 방문 체크
            checkBoxVisited.setChecked(place.isVisited());
            updateVisitedStyle(place.isVisited());
        }

        private void updateVisitedStyle(boolean isVisited) {
            if (isVisited) {
                itemView.setAlpha(0.6f);
                textViewPlaceName.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                textViewOrderIndex.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
            } else {
                itemView.setAlpha(1.0f);
                textViewPlaceName.setTextColor(context.getResources().getColor(android.R.color.black));
                textViewOrderIndex.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_bright));
            }
        }
    }
}