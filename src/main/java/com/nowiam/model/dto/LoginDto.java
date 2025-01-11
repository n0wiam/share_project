package com.nowiam.model.dto;

public class LoginDto {
    private String name;
    private String password;

    //get&set
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
