package com.auction.demo.common.model;

public class Admin extends User {
    private String employeeId;

    public Admin(String username, String password, String employeeId) {
        super(username, password);
        this.employeeId = employeeId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
}
