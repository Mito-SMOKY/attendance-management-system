package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public class DeleteRequestDto {
    private List<Integer> targetIds;
    private String remarks;
    private Integer approverId;

    public List<Integer> getTargetIds() { return targetIds; }
    public void setTargetIds(List<Integer> targetIds) { this.targetIds = targetIds; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }
}