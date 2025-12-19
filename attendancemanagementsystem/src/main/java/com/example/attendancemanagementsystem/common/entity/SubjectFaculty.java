package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjectfaculty")
public class SubjectFaculty {
    @EmbeddedId
    private SubjectFacultyKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectId")
    @JoinColumn(name = "SubjectID")
    private SubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "UserID")
    private UsersEntity teacher; // 既存の UsersEntity ではなく Users クラスを使用

    public SubjectFaculty() {}
    public SubjectFaculty(SubjectEntity subject, UsersEntity teacher) {
        this.subject = subject;
        this.teacher = teacher;
        this.id = new SubjectFacultyKey(subject.getSubjectId(), teacher.getUserId());
    }
    // Getter/Setter...
    public SubjectFacultyKey getId() { return id; }
    public void setId(SubjectFacultyKey id) { this.id = id; }
    public SubjectEntity getSubject() { return subject; }
    public void setSubject(SubjectEntity subject) { this.subject = subject; }
    public UsersEntity getTeacher() { return teacher; }
    public void setTeacher(UsersEntity teacher) { this.teacher = teacher; }
}