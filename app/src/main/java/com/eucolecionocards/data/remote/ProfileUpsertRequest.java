package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class ProfileUpsertRequest {
    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("bio")
    public String bio;

    @SerializedName("avatar_path")
    public String avatarUrl;

    public ProfileUpsertRequest(String id, String displayName, String bio, String avatarUrl) {
        this.id = id;
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }
}

