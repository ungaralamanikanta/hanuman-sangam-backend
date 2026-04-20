package com.sangam.dto;

public class AdminUpdateMemberRequest {
    private String name;
    private String email;
    private String phoneNumber;
    private String address;

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }
    public String getEmail()               { return email; }
    public void setEmail(String email)     { this.email = email; }
    public String getPhoneNumber()         { return phoneNumber; }
    public void setPhoneNumber(String p)   { this.phoneNumber = p; }
    public String getAddress()             { return address; }
    public void setAddress(String address) { this.address = address; }
}
