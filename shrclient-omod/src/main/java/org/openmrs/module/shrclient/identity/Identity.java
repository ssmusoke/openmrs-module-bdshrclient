package org.openmrs.module.shrclient.identity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Identity {
    public Identity() {
    }

    public Identity(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @JsonProperty("user")
    private String user;

    @JsonProperty("password")
    private String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
