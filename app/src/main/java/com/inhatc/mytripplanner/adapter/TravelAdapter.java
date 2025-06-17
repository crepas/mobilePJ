// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/adapter/TravelAdapter.java
package com.inhatc.mytripplanner.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inhatc.mytripplanner.R;
import com.inhatc.mytripplanner.model.Travel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TravelAdapter extends RecyclerView.Adapter<TravelAdapter.TravelViewHolder> {

    private List<Travel> travels;
    private Context context;
    private OnTravelClickListener onTravelClickListener;
    private SimpleDateFormat dateFormat;

    public interface OnTravelClickListener {
        void onTravelClick(Travel travel);
        void onTravelLongClick(Travel travel);
        void onEditClick(Travel travel);
        void onDeleteClick(Travel travel);
    }

    public TravelAdapter(Context context) {
        this.context = context;
        this.travels = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
    }

    public void setOnTravelClickListener(OnTravelClickListener listener) {
        this.onTravelClickListener = listener;
    }

    public void setTravels(List<Travel> travels) {
        this.travels = travels;
        notifyDataSetChanged();
    }

    public void addTravel(Travel travel) {
        travels.add(0, travel); // 최신 항목을 맨 위에 추가
        notifyItemInserted(0);
    }

    public void updateTravel(Travel updatedTravel) {
        for (int i = 0; i < travels.size(); i++) {
            if (travels.get(i).getId().equals(updatedTravel.getId())) {
                travels.set(i, updatedTravel);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeTravel(String travelId) {
        for (int i = 0; i < travels.size(); i++) {
            if (travels.get(i).getId().equals(travelId)) {
                travels.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public TravelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_travel, parent, false);
        return new TravelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TravelViewHolder holder, int position) {
        Travel travel = travels.get(position);
        holder.bind(travel);
    }

    @Override
    public int getItemCount() {
        return travels.size();
    }

    class TravelViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView destinationTextView;
        private TextView dateTextView;
        private TextView durationTextView;
        private ImageView editImageView;
        private ImageView deleteImageView;
        private View itemView;

        @SuppressLint("WrongViewCast")
        public TravelViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            destinationTextView = itemView.findViewById(R.id.textViewDestination);
            dateTextView = itemView.findViewById(R.id.textViewDate);
            durationTextView = itemView.findViewById(R.id.textViewDuration);
            editImageView = itemView.findViewById(R.id.imageViewEdit);
            deleteImageView = itemView.findViewById(R.id.imageViewDelete);

            setupClickListeners();
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onTravelClickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onTravelClickListener.onTravelClick(travels.get(position));
                        }
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onTravelClickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onTravelClickListener.onTravelLongClick(travels.get(position));
                        }
                    }
                    return true;
                }
            });

            editImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onTravelClickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onTravelClickListener.onEditClick(travels.get(position));
                        }
                    }
                }
            });

            deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onTravelClickListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onTravelClickListener.onDeleteClick(travels.get(position));
                        }
                    }
                }
            });
        }

        public void bind(Travel travel) {
            titleTextView.setText(travel.getTitle());
            destinationTextView.setText(travel.getDestination());

            // 날짜 표시
            if (travel.getStartDate() != null && travel.getEndDate() != null) {
                String startDateStr = dateFormat.format(travel.getStartDate());
                String endDateStr = dateFormat.format(travel.getEndDate());
                dateTextView.setText(startDateStr + " ~ " + endDateStr);

                // 기간 표시
                long duration = travel.getDurationInDays();
                durationTextView.setText(duration + "일");

                // 날짜 상태에 따른 색상 변경
                Date currentDate = new Date();
                if (travel.getEndDate().before(currentDate)) {
                    // 지난 여행
                    itemView.setAlpha(0.7f);
                    dateTextView.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                } else if (travel.getStartDate().before(currentDate) && travel.getEndDate().after(currentDate)) {
                    // 진행 중인 여행
                    dateTextView.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    // 예정된 여행
                    itemView.setAlpha(1.0f);
                    dateTextView.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                }
            } else {
                dateTextView.setText("날짜 미정");
                durationTextView.setText("");
            }
        }
    }
}