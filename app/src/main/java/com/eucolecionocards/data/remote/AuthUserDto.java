package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class AuthUserDto {
    @SerializedName("id")
    public String id;

    @SerializedName("email")
    public String email;
}

