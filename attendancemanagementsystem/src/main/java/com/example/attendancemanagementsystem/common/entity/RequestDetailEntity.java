package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "requestdetail")
public class RequestDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetailID")
    private Integer detailID;

    @ManyToOne
    @JoinColumn(name = "RequestID", nullable = false)
    private RequestEntity request;

    @Column(name = "TargetDate", nullable = false)
    private LocalDate targetDate;

    // TimeSlotテーブルへの外部キーですが、検索ロジックの簡略化のため
    // 今回はIntegerとしてIDを保持します。
    @Column(name = "SlotID", nullable = false)
    private Integer slotId;

    // --- Constructor ---
    public RequestDetailEntity() {
    }

    // --- Getters and Setters ---

    public Integer getDetailID() {
        return detailID;
    }

    public void setDetailID(Integer detailID) {
        this.detailID = detailID;
    }

    public RequestEntity getRequest() {
        return request;
    }

    public void setRequest(RequestEntity request) {
        this.request = request;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }
}