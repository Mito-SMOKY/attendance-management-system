package com.example.attendancemanagementsystem.common.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DepartmentSubjectKey implements Serializable {
    @Column(name = "DepartmentID")
    private Integer departmentId;

    @Column(name = "SubjectID")
    private Integer subjectId;
    
    // コンストラクタやGetter/Setter/hashCode/equalsは上記と同様に作成
    // (省略しますが、IDEの自動生成機能を使うと早いです)
    public DepartmentSubjectKey() {}
    public DepartmentSubjectKey(Integer departmentId, Integer subjectId) {
        this.departmentId = departmentId;
        this.subjectId = subjectId;
    }
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentSubjectKey that = (DepartmentSubjectKey) o;
        return Objects.equals(departmentId, that.departmentId) && Objects.equals(subjectId, that.subjectId);
    }
    @Override
    public int hashCode() { return Objects.hash(departmentId, subjectId); }
}