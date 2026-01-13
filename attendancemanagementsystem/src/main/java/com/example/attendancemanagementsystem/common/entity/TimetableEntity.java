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
    @Column(name = "TimetableID")
    private Integer timetableId;

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

    //科目情報の取得用 (読み取り専用)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubjectID", insertable = false, updatable = false)
    private SubjectEntity subject;

    // 教室情報の取得用 (読み取り専用)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ClassroomID", insertable = false, updatable = false)
    private ClassroomEntity classroom;

    @ManyToOne
    @JoinColumn(name = "SlotID", insertable = false, updatable = false)
    private TimeSlotEntity timeSlot;

    // Timetable(1) 対 Attendance(多)
    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttendanceEntity> attendances;

    // --- constructor ---
    public TimetableEntity() {
    }

    // --- Getter/Setter ---
    public Integer getTimetableId() {
        return timetableId;
    }

    public void setTimetableId(Integer timetableId) {
        this.timetableId = timetableId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public TimeSlotEntity getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlotEntity timeSlot) {
        this.timeSlot = timeSlot;
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

    // ★追加: SubjectEntityのGetter/Setter
    public SubjectEntity getSubject() {
        return subject;
    }

    public void setSubject(SubjectEntity subject) {
        this.subject = subject;
    }

    // ★追加: ClassroomEntityのGetter/Setter
    public ClassroomEntity getClassroom() {
        return classroom;
    }

    public void setClassroom(ClassroomEntity classroom) {
        this.classroom = classroom;
    }

    public List<AttendanceEntity> getAttendances() {
        return attendances;
    }

    public void setAttendances(List<AttendanceEntity> attendances) {
        this.attendances = attendances;
    }
}