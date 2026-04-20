package com.auction.shared.models;

public class Admin extends User {
    private static final long serialVersionUID = 1L;
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
