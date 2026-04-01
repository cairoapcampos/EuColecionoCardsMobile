package com.eucolecionocards.data.remote;

import com.google.gson.annotations.SerializedName;

public class CardDto {
    @SerializedName("id")
    public String id;

    @SerializedName("code")
    public String code;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;

    @SerializedName("image_path")
    public String imagePath;

    @SerializedName("type")
    public String type;

    @SerializedName("rarity")
    public String rarity;

    @SerializedName("collection")
    public String collection;

    @SerializedName("year")
    public Integer year;

    @SerializedName("quality")
    public String quality;

    @SerializedName("price")
    public Double price;

    @SerializedName("stock_quantity")
    public Integer stockQuantity;
}

