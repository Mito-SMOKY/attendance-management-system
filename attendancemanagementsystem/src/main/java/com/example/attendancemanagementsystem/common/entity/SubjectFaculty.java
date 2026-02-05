package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjectfaculty")
public class SubjectFaculty {

    @EmbeddedId
    private SubjectFacultyId id;

    // コンストラクタ
    public SubjectFaculty() {}

    public SubjectFaculty(SubjectFacultyId id) {
        this.id = id;
    }

    // Getter / Setter
    public SubjectFacultyId getId() {
        return id;
    }

    public void setId(SubjectFacultyId id) {
        this.id = id;
    }
}