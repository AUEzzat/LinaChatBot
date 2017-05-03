package com.sourcey.linachatbot;

/**
 * Created by amrezzat on 4/28/2017.
 */

public class User {
    private String usernameOrEmail;
    private String password;
    private String token;
    private long tokenTime;

    public User() {
        this.tokenTime = System.nanoTime();

    }

    public User(String usernameOrEmail, String password, String token) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
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

    public String getuserNameOrEmail() {

        return usernameOrEmail;
    }

    public void setuserNameOrEmail(String userNameOrEmail) {
        this.usernameOrEmail = userNameOrEmail;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getPassword() {
        return password;
    }



}
