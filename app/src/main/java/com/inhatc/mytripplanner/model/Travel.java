package com.inhatc.mytripplanner.model;
import java.io.Serializable;
import java.util.Date;
    public class Travel implements Serializable {
        private String id;
        private String title;
        private String destination;
        private Date startDate;
        private Date endDate;
        private String description;
        private double latitude;
        private double longitude;
        private String userId;
        private Date createdAt;
        private Date updatedAt;

        // Firebase용 기본 생성자
        public Travel() {
        }

        // 전체 생성자
        public Travel(String id, String title, String destination, Date startDate, Date endDate,
                      String description, double latitude, double longitude, String userId) {
            this.id = id;
            this.title = title;
            this.destination = destination;
            this.startDate = startDate;
            this.endDate = endDate;
            this.description = description;
            this.latitude = latitude;
            this.longitude = longitude;
            this.userId = userId;
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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
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

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
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

        public long getDurationInDays() {
            if (startDate != null && endDate != null) {
                long diffInMillis = endDate.getTime() - startDate.getTime();
                return diffInMillis / (1000 * 60 * 60 * 24) + 1; // +1 to include both start and end dates
            }
            return 0;
        }
    }
