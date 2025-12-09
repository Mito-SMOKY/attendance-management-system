package com.example.attendancemanagementsystem.classroom.timetable.dto;

import java.util.List;
import java.util.Map;

public class StudentTimetableDto {

    private String weekStart;   // 週の開始日 (月曜日) "YYYY-MM-DD"
    private int weekNumber;     // 月の第何週か
    private List<String> dates; // その週の日付リスト (月～金)
    private List<String> timeSlots; // 時間割の枠
    
    // スケジュールデータ
    // Map<日付文字列, Map<時限インデックス(0始まり), 授業詳細>>
    private Map<String, Map<Integer, ClassDetail>> schedule;

    // --- 内部クラス: 授業詳細 ---
    public static class ClassDetail {
        private String subject;
        private String classroom;

        public ClassDetail(String subject, String classroom) {
            this.subject = subject;
            this.classroom = classroom;
        }

        // --- Getter/Setter ---
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getClassroom() { return classroom; }
        public void setClassroom(String classroom) { this.classroom = classroom; }
    }

    // --- Getter/Setter ---
    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public List<String> getDates() { return dates; }
    public void setDates(List<String> dates) { this.dates = dates; }

    public List<String> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<String> timeSlots) { this.timeSlots = timeSlots; }

    public Map<String, Map<Integer, ClassDetail>> getSchedule() { return schedule; }
    public void setSchedule(Map<String, Map<Integer, ClassDetail>> schedule) { this.schedule = schedule; }
}