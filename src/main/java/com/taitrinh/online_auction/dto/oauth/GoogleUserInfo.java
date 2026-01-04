package com.taitrinh.online_auction.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GoogleUserInfo {
    private String sub; // Google's unique user ID

    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    private String name;

    private String picture; // Avatar URL

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;
}
