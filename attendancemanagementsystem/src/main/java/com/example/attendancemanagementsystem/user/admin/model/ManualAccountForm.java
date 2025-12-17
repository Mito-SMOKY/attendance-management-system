package com.example.attendancemanagementsystem.user.admin.model;

import java.util.List;

public class ManualAccountForm {

    // HTMLの name="dataListName" を受け取る
    private String dataListName;

    // HTMLの name="name[]" を受け取る
    private List<String> name;
    
    // HTMLの name="studentId[]" を受け取る
    private List<String> studentId;
    
    // HTMLの name="affiliationId[]" を受け取る
    private List<String> affiliationId;

    // --- Getter / Setter ---

    // 以前のエラー「undefined method getDataListName」を消すためにこの名前が必要です
    public String getDataListName() {
        return dataListName;
    }

    public void setDataListName(String dataListName) {
        this.dataListName = dataListName;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public List<String> getStudentId() {
        return studentId;
    }

    public void setStudentId(List<String> studentId) {
        this.studentId = studentId;
    }

    public List<String> getAffiliationId() {
        return affiliationId;
    }

    public void setAffiliationId(List<String> affiliationId) {
        this.affiliationId = affiliationId;
    }
}