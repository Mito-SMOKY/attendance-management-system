package com.example.attendancemanagementsystem.attendance.display.dto;

public class SubjectListDto {
    private Integer subjectId;
    private String courseName;      // コース名
    private String subjectName;     // 教科名
    private String teacherName;     // 担当教師名
    
    private int attendedClasses;    // 出席数
    private int totalClasses;       // 授業総数
    private double attendanceRate;  // 出席率 (0.0 ~ 1.0)

    // コンストラクタ
    public SubjectListDto(Integer subjectId, String courseName, String subjectName, String teacherName) {
        this.subjectId = subjectId;
        this.courseName = courseName;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.attendedClasses = 0;
        this.totalClasses = 0;
        this.attendanceRate = 0.0;
    }

    // 出席率計算
    public void calculateRate() {
        if (this.totalClasses > 0) {
            this.attendanceRate = (double) this.attendedClasses / this.totalClasses;
        } else {
            this.attendanceRate = 0.0;
        }
    }

    // 警告判定 (出席率80%未満)
    public boolean isAttendanceRisk() {
        return this.totalClasses > 0 && this.attendanceRate < 0.8; 
    }

    // --- Getter / Setter ---
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public int getAttendedClasses() { return attendedClasses; }
    public void setAttendedClasses(int attendedClasses) { this.attendedClasses = attendedClasses; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    public double getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }
}