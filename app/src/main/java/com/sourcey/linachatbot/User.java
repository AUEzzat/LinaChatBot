package com.sourcey.linachatbot;

/**
 * Created by amrezzat on 4/28/2017.
 */

public class User {
    private String userName;
    private String email;
    private String password;
    private String token;
    private long tokenTime;




    public User() {

    }

    public User(String userName, String email, String password, String token) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.token = token;
        this.tokenTime = System.nanoTime();
    }

    public String getToken() {
        return token;
    }

    public long getTokenTime() {
        return tokenTime;
    }

    public void setToken(String token) {
        this.token = token;
        this.tokenTime = System.nanoTime();
    }

    public String getUserName() {

        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }


}
