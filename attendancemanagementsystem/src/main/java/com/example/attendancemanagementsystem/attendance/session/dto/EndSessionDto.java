package com.example.attendancemanagementsystem.attendance.session.dto;

import java.util.Map;

public class EndSessionDto {
    private Integer sessionId;
    private Map<Integer, Integer> changes;

    // Getter/Setter

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Map<Integer, Integer> getChanges() {
        return changes;
    }

    public void setChanges(Map<Integer, Integer> changes) {
        this.changes = changes;
    }
}
