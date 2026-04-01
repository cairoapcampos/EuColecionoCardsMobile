package com.eucolecionocards.data.api;

import com.eucolecionocards.data.remote.AuthSessionDto;
import com.eucolecionocards.data.remote.AuthSignInRequest;
import com.eucolecionocards.data.remote.CardDto;
import com.eucolecionocards.data.remote.FavoriteDto;
import com.eucolecionocards.data.remote.FavoriteUpsertRequest;
import com.eucolecionocards.data.remote.ProfileDto;
import com.eucolecionocards.data.remote.ProfileUpsertRequest;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseService {
    @POST("auth/v1/signup")
    Call<AuthSessionDto> signUpWithEmail(
            @Header("apikey") String apikey,
            @Body AuthSignInRequest body
    );

    @POST("auth/v1/token")
    Call<AuthSessionDto> signInWithPassword(
            @Header("apikey") String apikey,
            @Query("grant_type") String grantType,
            @Body AuthSignInRequest body
    );

    @GET("rest/v1/cards")
    Call<List<CardDto>> getCards(
            @Header("apikey") String apikey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/favorites")
    Call<List<FavoriteDto>> getFavorites(
            @Header("apikey") String apikey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("user_id") String userIdFilter
    );

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/favorites")
    Call<ResponseBody> upsertFavorite(
            @Header("apikey") String apikey,
            @Header("Authorization") String authorization,
            @Body List<FavoriteUpsertRequest> body
    );

    @DELETE("rest/v1/favorites")
    Call<ResponseBody> deleteFavorite(
            @Header("apikey") String apikey,
            @Header("Authorization") String authorization,
            @Query("user_id") String userIdFilter,
            @Query("card_id") String cardIdFilter
    );

    @GET("rest/v1/profiles")
    Call<List<ProfileDto>> getProfile(
            @Header("apikey") String apikey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("id") String idFilter
    );

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/profiles")
    Call<ResponseBody> upsertProfile(
            @Header("apikey") String apikey,
            @Header("Authorization") String authorization,
            @Body List<ProfileUpsertRequest> body
    );
}
