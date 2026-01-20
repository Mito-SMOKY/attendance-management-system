package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "classsession")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Integer sessionId;

    @Column(name = "UserId")
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "TimeTableID")
    private TimetableEntity timeTable;

    @ManyToOne
    @JoinColumn(name = "SlotID")
    private TimeSlotEntity timeSlot;

    @ManyToOne
    @JoinColumn(name = "ClassroomID")
    private ClassroomEntity classroom;

    @ManyToOne
    @JoinColumn(name = " DepartmentID")
    private DepartmentEntity department;

    @Column(name = "Grade")
    private Integer targetGrade;

    @ManyToOne
    @JoinColumn(name = "SubjectID")
    private SubjectEntity subject;

    @Column(name = "SessionDate")
    private LocalDate sessionDate;

    @Column(name = "StartTime")
    private LocalDateTime startTime;

    @Column(name = "EndTime")
    private LocalDateTime endTime;

    @Column(name = "SessionFlag", nullable = false)
    private boolean sessionFlag = false;    

    // Getter / Setter

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public TimetableEntity getTimeTable() {
        return timeTable;
    }

    public void setTimeTable(TimetableEntity timeTable) {
        this.timeTable = timeTable;
    }

    public TimeSlotEntity getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlotEntity timeSlot) {
        this.timeSlot = timeSlot;
    }

    public ClassroomEntity getClassroom() {
        return classroom;
    }

    public void setClassroom(ClassroomEntity classroom) {
        this.classroom = classroom;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }
    public Integer getTargetGrade() {
        return targetGrade;
    }

    public void setTargetGrade(Integer targetGrade) {
        this.targetGrade = targetGrade;
    }

    public SubjectEntity getSubject() {
        return subject;
    }

    public void setSubject(SubjectEntity subject) {
        this.subject = subject;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean getSessionFlag() {
        return sessionFlag;
    }

    public void setSessionFlag(boolean sessionFlag) {
        this.sessionFlag = sessionFlag;
    }
}