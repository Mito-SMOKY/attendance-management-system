package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public class StatusRequestDto {
    private List<Integer> targetIds;
    private Integer targetStatusId;
    private String remarks;
    private Integer approverId;

    public List<Integer> getTargetIds() { return targetIds; }
    public void setTargetIds(List<Integer> targetIds) { this.targetIds = targetIds; }
    
    public Integer getTargetStatusId() { return targetStatusId; }
    public void setTargetStatusId(Integer targetStatusId) { this.targetStatusId = targetStatusId; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }
}