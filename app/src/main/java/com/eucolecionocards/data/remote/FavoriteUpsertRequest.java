package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class FavoriteUpsertRequest {
    @SerializedName("user_id")
    public String userId;

    @SerializedName("card_id")
    public String cardId;

    public FavoriteUpsertRequest(String userId, String cardId) {
        this.userId = userId;
        this.cardId = cardId;
    }
}

