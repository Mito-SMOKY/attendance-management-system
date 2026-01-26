package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public class DepartmentRequestDto {
    private List<Integer> targetIds;
    private Integer targetDepartmentId;
    private String remarks;
    private Integer approverId;

    public List<Integer> getTargetIds() { return targetIds; }
    public void setTargetIds(List<Integer> targetIds) { this.targetIds = targetIds; }
    
    public Integer getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(Integer targetDepartmentId) { this.targetDepartmentId = targetDepartmentId; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }
}