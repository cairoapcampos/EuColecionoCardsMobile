package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class AuthSignInRequest {
    @SerializedName("email")
    public String email;

    @SerializedName("password")
    public String password;

    public AuthSignInRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}

