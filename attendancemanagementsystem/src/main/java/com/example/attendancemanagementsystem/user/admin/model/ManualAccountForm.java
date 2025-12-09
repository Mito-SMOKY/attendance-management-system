package com.example.attendancemanagementsystem.user.admin.model;


import java.util.List;

public class ManualAccountForm {
    private String datalistName;
    private List<ManualAccountData> accounts;

    public String getDatalistName() {
        return datalistName;
    }

    public void setDatalistName(String datalistName) {
        this.datalistName = datalistName;
    }

    public List<ManualAccountData> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<ManualAccountData> accounts) {
        this.accounts = accounts;
    }
}