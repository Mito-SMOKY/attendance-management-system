package com.example.attendancemanagementsystem.common.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SubjectFacultyKey implements Serializable {
    @Column(name = "SubjectID")
    private Integer subjectId;

    @Column(name = "UserID")
    private Integer userId;

    public SubjectFacultyKey() {}
    public SubjectFacultyKey(Integer subjectId, Integer userId) {
        this.subjectId = subjectId;
        this.userId = userId;
    }
    // Getter/Setter, hashCode, equals
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubjectFacultyKey that = (SubjectFacultyKey) o;
        return Objects.equals(subjectId, that.subjectId) && Objects.equals(userId, that.userId);
    }
    @Override
    public int hashCode() { return Objects.hash(subjectId, userId); }
}