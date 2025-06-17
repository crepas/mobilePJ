// 파일 경로: app/src/main/java/com/inhatc/mytripplanner/model/Place.java
package com.inhatc.mytripplanner.model;

import java.io.Serializable;
import java.util.Date;

public class Place implements Serializable {
    private String id;
    private String travelId;
    private Date scheduleDate;
    private int dayNumber;
    private String name;
    private String address;
    private String notes;
    private double latitude;
    private double longitude;
    private int orderIndex; // 방문 순서
    private boolean isVisited; // 방문 완료 여부
    private Date createdAt;
    private Date updatedAt;

    // Firebase용 기본 생성자
    public Place() {
    }

    // 전체 생성자
    public Place(String id, String travelId, Date scheduleDate, int dayNumber,
                 String name, String address, String notes, double latitude,
                 double longitude, int orderIndex) {
        this.id = id;
        this.travelId = travelId;
        this.scheduleDate = scheduleDate;
        this.dayNumber = dayNumber;
        this.name = name;
        this.address = address;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.orderIndex = orderIndex;
        this.isVisited = false;
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

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
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

    public String getDisplayName() {
        return orderIndex + ". " + name;
    }
}