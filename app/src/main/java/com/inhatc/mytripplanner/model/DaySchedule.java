// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/model/DaySchedule.java
package com.inhatc.mytripplanner.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class DaySchedule implements Serializable {
    private String id;
    private String travelId;
    private Date scheduleDate;
    private String title;
    private String description;
    private List<Activity> activities;
    private Date createdAt;
    private Date updatedAt;

    // Firebase용 기본 생성자
    public DaySchedule() {
    }

    // 전체 생성자
    public DaySchedule(String id, String travelId, Date scheduleDate, String title,
                       String description, List<Activity> activities) {
        this.id = id;
        this.travelId = travelId;
        this.scheduleDate = scheduleDate;
        this.title = title;
        this.description = description;
        this.activities = activities;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getter & Setter 메서드들
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTravelId() {
        return travelId;
    }

    public void setTravelId(String travelId) {
        this.travelId = travelId;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 편의 메서드들
    public int getActivityCount() {
        return activities != null ? activities.size() : 0;
    }

    public boolean hasActivities() {
        return activities != null && !activities.isEmpty();
    }
}