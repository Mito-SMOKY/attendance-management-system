package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "timetable")
public class TimetableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TimeTableID")
    private Integer timeTableId;

    @Column(name = "Date")
    private LocalDate date;

    @Column(name = "SlotID")
    private Integer slotId; 

    @Column(name = "UserID")
    private Integer userId; 

    @Column(name = "SubjectID")
    private Integer subjectId; 

    @Column(name = "ClassroomID")
    private Integer classroomId; 

    @Column(name = "AcademicYear")
    private Integer academicYear;

    // --- 関連定義 ---

    // Timetable(多) 対 Department(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID") 
    private DepartmentEntity department;

    // Timetable(1) 対 Attendance(多)
    @OneToMany(mappedBy = "timeTable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttendanceEntity> attendances;

    // --- constructor ---
    public TimetableEntity() {
    }

    // --- Getter/Setter ---
    public Integer getTimeTableId() {
        return timeTableId;
    }

    public void setTimeTableId(Integer timeTableId) {
        this.timeTableId = timeTableId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Integer classroomId) {
        this.classroomId = classroomId;
    }

    public Integer getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(Integer academicYear) {
        this.academicYear = academicYear;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }

    public List<AttendanceEntity> getAttendances() {
        return attendances;
    }

    public void setAttendances(List<AttendanceEntity> attendances) {
        this.attendances = attendances;
    }
}