package com.example.attendancemanagementsystem.attendance.display.dto;

public class DailyAttendanceDto {
    private Integer subjectId;  // 教科ID
    private Integer slotId;       // 時限 (1, 2...)
    private String subjectName;   // 科目名
    private String classroomName; // 教室名
    private String status;        // 出席状況 (〇, ×, △, 空白)

    // コンストラクタ
    public DailyAttendanceDto(Integer subjectId, Integer slotId, String subjectName, String classroomName, String status) {
        this.subjectId = subjectId;
        this.slotId = slotId;
        this.subjectName = subjectName;
        this.classroomName = classroomName;
        this.status = status;
    }

    // --- Getter / Setter ---
    public Integer getSubjectId() {
        return subjectId;
    }
    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }
    
    public Integer getSlotId() {
        return slotId;
    }
    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}