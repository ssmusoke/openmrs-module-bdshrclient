package org.openmrs.module.shrclient.handlers;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IdentityToken {
    IdentityToken() {
    }

    @JsonProperty("token")
    private String token;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
