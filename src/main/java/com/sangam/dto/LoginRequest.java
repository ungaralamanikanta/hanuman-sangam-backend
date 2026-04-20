package com.sangam.dto;

public class LoginRequest {
    private String phoneNumber;
    private String password;

    public String getPhoneNumber()           { return phoneNumber; }
    public void setPhoneNumber(String p)     { this.phoneNumber = p; }
    public String getPassword()              { return password; }
    public void setPassword(String password) { this.password = password; }
}
