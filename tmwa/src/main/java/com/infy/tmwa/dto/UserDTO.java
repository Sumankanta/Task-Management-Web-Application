package com.infy.tmwa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.infy.tmwa.entity.User;

public class UserDTO {
    private Long   id;
    private String fullName;
    private String email;
    private String role;
    private String avatarColor;
    private String bio;

    @JsonProperty("isActive")
    private boolean isActive;

    public UserDTO(User user) {
        this.id          = user.getId();
        this.fullName    = user.getFullName();
        this.email       = user.getEmail();
        this.role        = user.getRole().name();
        this.isActive    = user.isActive();
        this.avatarColor = user.getAvatarColor();
        this.bio         = user.getBio();
    }

    public Long getId()          { return id; }
    public String getFullName()  { return fullName; }
    public String getEmail()     { return email; }
    public String getRole()      { return role; }
    public String getAvatarColor(){ return avatarColor; }
    public String getBio()       { return bio; }

    @JsonProperty("isActive")
    public boolean isActive()    { return isActive; }
}