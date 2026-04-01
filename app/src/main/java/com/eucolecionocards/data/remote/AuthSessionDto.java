package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class AuthSessionDto {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;

    @SerializedName("user")
    public AuthUserDto user;
}

