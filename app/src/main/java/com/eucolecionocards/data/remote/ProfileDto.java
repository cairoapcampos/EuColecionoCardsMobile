package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class ProfileDto {
    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("bio")
    public String bio;

    @SerializedName("avatar_path")
    public String avatarUrl;
}

