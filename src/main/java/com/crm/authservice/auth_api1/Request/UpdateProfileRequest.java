package com.crm.authservice.auth_api1.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;


public class UpdateProfileRequest {

    @NotNull
    private Integer userId;

    @NotNull
    private String firstname;

    @NotNull
    private String lastname;

    @Email
    private String email;

    private String password;

    // Getters and setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
