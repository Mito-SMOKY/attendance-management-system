package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subjectfaculty")
@IdClass(SubjectFacultyId.class)
public class SubjectFaculty {

    @Id
    @Column(name = "SubjectID")
    private Integer subjectId;
    
    @Id
    @Column(name = "UserID")
    private Integer userId;

    // --- リレーション定義 ---
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectId")
    @JoinColumn(name = "SubjectID", insertable = false, updatable = false)
    private SubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private UsersEntity teacher;

    // --- コンストラクタ ---

    public SubjectFaculty() {}

    public SubjectFaculty(SubjectEntity subject, UsersEntity teacher) {
        this.subject = subject;
        this.teacher = teacher;
        // IDは自動的に同期されますが、明示的にセットする場合
        if (subject != null) this.subjectId = subject.getSubjectId();
        if (teacher != null) this.userId = teacher.getUserId();
    }

    // --- Getter / Setter ---

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public SubjectEntity getSubject() { return subject; }
    public void setSubject(SubjectEntity subject) { this.subject = subject; }

    public UsersEntity getTeacher() { return teacher; }
    public void setTeacher(UsersEntity teacher) { this.teacher = teacher; }
}