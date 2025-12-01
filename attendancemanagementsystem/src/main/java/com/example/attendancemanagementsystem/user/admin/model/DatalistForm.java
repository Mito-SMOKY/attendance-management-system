package com.example.attendancemanagementsystem.user.admin.model;


import java.util.List;

public class DatalistForm {
    private String datalistName;
    private List<TempAccountData> tempAccounts;

    public String getDatalistName() {
        return datalistName;
    }

    public void setDatalistName(String datalistName) {
        this.datalistName = datalistName;
    }

    public List<TempAccountData> getTempAccounts() {
        return tempAccounts;
    }

    public void setTempAccounts(List<TempAccountData> tempAccounts) {
        this.tempAccounts = tempAccounts;
    }
}
