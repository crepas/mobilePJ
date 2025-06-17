// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/model/Activity.java
package com.inhatc.mytripplanner.model;

import java.io.Serializable;
import java.util.Date;

public class Activity implements Serializable {
    private String id;
    private String dayScheduleId;
    private String title;
    private String description;
    private String location;
    private double latitude;
    private double longitude;
    private Date startTime;
    private Date endTime;
    private String category; // 관광, 식사, 쇼핑, 휴식 등
    private String notes;
    private boolean isCompleted;
    private Date createdAt;
    private Date updatedAt;

    // Firebase용 기본 생성자
    public Activity() {
    }

    // 전체 생성자
    public Activity(String id, String dayScheduleId, String title, String description,
                    String location, double latitude, double longitude, Date startTime,
                    Date endTime, String category, String notes) {
        this.id = id;
        this.dayScheduleId = dayScheduleId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.notes = notes;
        this.isCompleted = false;
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

    public String getDayScheduleId() {
        return dayScheduleId;
    }

    public void setDayScheduleId(String dayScheduleId) {
        this.dayScheduleId = dayScheduleId;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
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
    public boolean hasValidLocation() {
        return latitude != 0.0 && longitude != 0.0;
    }

    public String getCategoryEmoji() {
        switch (category != null ? category : "") {
            case "관광": return "🏛️";
            case "식사": return "🍽️";
            case "쇼핑": return "🛍️";
            case "휴식": return "☕";
            case "교통": return "🚗";
            case "숙박": return "🏨";
            default: return "📍";
        }
    }

    public long getDurationInMinutes() {
        if (startTime != null && endTime != null) {
            return (endTime.getTime() - startTime.getTime()) / (1000 * 60);
        }
        return 0;
    }
}