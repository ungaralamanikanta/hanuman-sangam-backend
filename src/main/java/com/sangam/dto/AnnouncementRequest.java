package com.sangam.dto;

public class AnnouncementRequest {
    private String title;
    private String message;

    public String getTitle()           { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage()         { return message; }
    public void setMessage(String msg) { this.message = msg; }
}
