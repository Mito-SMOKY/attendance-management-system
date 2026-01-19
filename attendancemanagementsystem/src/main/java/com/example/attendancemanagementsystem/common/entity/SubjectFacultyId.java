package com.example.attendancemanagementsystem.common.entity;

import java.io.Serializable;
import java.util.Objects;

public class SubjectFacultyId implements Serializable {
    private Integer subjectId;
    private Integer userId;

    public SubjectFacultyId() {}

    public SubjectFacultyId(Integer subjectId, Integer userId) {
        this.subjectId = subjectId;
        this.userId = userId;
    }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubjectFacultyId that = (SubjectFacultyId) o;
        return Objects.equals(subjectId, that.subjectId) &&
            Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId, userId);
    }
}