// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/adapter/TravelDetailPagerAdapter.java
package com.inhatc.mytripplanner.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.inhatc.mytripplanner.model.Travel;
import com.inhatc.mytripplanner.DayScheduleFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TravelDetailPagerAdapter extends FragmentStateAdapter {

    private List<Date> dateList;
    private Travel travel;

    public TravelDetailPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
        this.dateList = new ArrayList<>();
    }

    public void setTravelDates(Travel travel, Date startDate, Date endDate) {
        this.travel = travel;
        this.dateList.clear();

        if (startDate == null || endDate == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);

        // 시작일부터 종료일까지 모든 날짜 추가
        while (!calendar.after(endCalendar)) {
            dateList.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Date scheduleDate = dateList.get(position);
        return DayScheduleFragment.newInstance(travel.getId(), scheduleDate, position + 1);
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public Date getDateAt(int position) {
        if (position >= 0 && position < dateList.size()) {
            return dateList.get(position);
        }
        return null;
    }
}