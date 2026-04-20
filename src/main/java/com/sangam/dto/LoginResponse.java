package com.sangam.dto;

public class LoginResponse {
    private String token;
    private String role;
    private Long   memberId;
    private String name;

    public LoginResponse() {}

    public LoginResponse(String token, String role, Long memberId, String name) {
        this.token    = token;
        this.role     = role;
        this.memberId = memberId;
        this.name     = name;
    }

    public String getToken()               { return token; }
    public void setToken(String token)     { this.token = token; }
    public String getRole()                { return role; }
    public void setRole(String role)       { this.role = role; }
    public Long getMemberId()              { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }
}
