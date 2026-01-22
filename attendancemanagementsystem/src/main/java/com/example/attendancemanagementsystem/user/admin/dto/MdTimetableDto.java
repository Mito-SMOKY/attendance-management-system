package com.example.attendancemanagementsystem.user.admin.dto;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MdTimetableDto {

    // 年度・学期選択用
    private Integer year; 
    private Integer term; 

    // 指定期間フィールド
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 画面から "2025-04-29, 2025-05-03" のような形式で渡されます
    private String excludedDates;

    // 検索・特定用フィールド
    private Integer courseId;
    private Integer targetGrade; 
    private String className; 
    private Integer departmentId;
    private String majorName;
    private String courseName;

    // スケジュールデータ (get時にnullなら空Mapを生成して返す仕組み)
    private Map<Integer, Map<String, Cell>> scheduleMap = new HashMap<Integer, Map<String, Cell>>() {
        @Override
        public Map<String, Cell> get(Object key) {
            if (!super.containsKey(key) && key instanceof Integer) {
                super.put((Integer) key, new HashMap<>());
            }
            return super.get(key);
        }
    };

    public MdTimetableDto() {}

    // --- 内部クラス ---
    public static class Cell {
        private Integer subjectId;
        private Integer classroomId;
        private Integer userId; 
        private String subjectName;
        private String classroomName;
        private String teacherName;
        
        // Getter/Setter
        public Integer getSubjectId() { return subjectId; }
        public void setSubjectId(Integer s) { this.subjectId = s; }

        public Integer getClassroomId() { return classroomId; }
        public void setClassroomId(Integer c) { this.classroomId = c; }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer u) { this.userId = u; }

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String s) { this.subjectName = s; }

        public String getClassroomName() { return classroomName; }
        public void setClassroomName(String c) { this.classroomName = c; }

        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String t) { this.teacherName = t; }
    }

    // --- Getter/Setter ---
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getTerm() { return term; }
    public void setTerm(Integer term) { this.term = term; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate s) { this.startDate = s; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate e) { this.endDate = e; }
    
    // ★追加分のGetter/Setter
    public String getExcludedDates() { return excludedDates; }
    public void setExcludedDates(String excludedDates) { this.excludedDates = excludedDates; }
    
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer c) { this.courseId = c; }

    public Integer getTargetGrade() { return targetGrade; }
    public void setTargetGrade(Integer g) { this.targetGrade = g; }

    public String getClassName() { return className; }
    public void setClassName(String c) { this.className = c; }
    
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer d) { this.departmentId = d; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public Map<Integer, Map<String, Cell>> getScheduleMap() { return scheduleMap; }
    public void setScheduleMap(Map<Integer, Map<String, Cell>> m) { this.scheduleMap = m; }
}