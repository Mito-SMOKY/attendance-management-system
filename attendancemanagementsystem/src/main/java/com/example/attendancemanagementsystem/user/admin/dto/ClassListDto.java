package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.Map;

public class ClassListDto {

    private String searchDate;      
    private String headerDate;      
    private String searchWord;
    private Map<Integer, PeriodDetail> periods;

    // --- Getter / Setter ---
    public String getSearchDate() {
        return searchDate;
    }

    public void setSearchDate(String searchDate) {
        this.searchDate = searchDate;
    }

    public String getHeaderDate() {
        return headerDate;
    }

    public void setHeaderDate(String headerDate) {
        this.headerDate = headerDate;
    }

    public String getSearchWord() {
        return searchWord;
    }

    public void setSearchWord(String searchWord) {
        this.searchWord = searchWord;
    }

    public Map<Integer, PeriodDetail> getPeriods() {
        return periods;
    }

    public void setPeriods(Map<Integer, PeriodDetail> periods) {
        this.periods = periods;
    }

    // 内部クラス: 各時限の詳細
    public static class PeriodDetail {
        private boolean hasClass;      
        private String subjectName;    
        private String classroomName;  
        private Integer subjectId; 
            
        // --- Getter / Setter ---
        public boolean isHasClass() {
            return hasClass;
        }

        public void setHasClass(boolean hasClass) {
            this.hasClass = hasClass;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getClassroomName() {
            return classroomName;
        }

        public void setClassroomName(String classroomName) {
            this.classroomName = classroomName;
        }

        public Integer getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(Integer subjectId) {
            this.subjectId = subjectId;
        }
    }
}