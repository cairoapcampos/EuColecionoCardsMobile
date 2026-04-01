package com.eucolecionocards.data.repository;

import com.eucolecionocards.data.api.ApiClient;
import com.eucolecionocards.data.api.SupabaseConfig;
import com.eucolecionocards.data.remote.ProfileDto;
import com.eucolecionocards.data.remote.ProfileUpsertRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    public interface LoadProfileCallback {
        void onSuccess(ProfileDto profile);
        void onEmpty();
        void onError(String message);
    }

    public interface SaveProfileCallback {
        void onSuccess();
        void onError(String message);
    }

    public void loadProfile(String userId, String accessToken, LoadProfileCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado");
            return;
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Sessao expirada. Faca login novamente.");
            return;
        }

        ApiClient.getSupabaseService()
                .getProfile(SupabaseConfig.getAnonKey(), buildAuthorization(accessToken), "id,display_name,bio,avatar_path", "eq." + userId)
                .enqueue(new Callback<List<ProfileDto>>() {
                    @Override
                    public void onResponse(Call<List<ProfileDto>> call, Response<List<ProfileDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Falha ao carregar perfil: HTTP " + response.code() + formatErrorBody(response.errorBody()));
                            return;
                        }
                        List<ProfileDto> rows = response.body();
                        if (rows.isEmpty()) {
                            callback.onEmpty();
                            return;
                        }
                        callback.onSuccess(rows.get(0));
                    }

                    @Override
                    public void onFailure(Call<List<ProfileDto>> call, Throwable t) {
                        callback.onError("Erro de rede ao carregar perfil");
                    }
                });
    }

    public void saveProfile(String userId, String displayName, String bio, String avatarUrl, String accessToken, SaveProfileCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado");
            return;
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Sessao expirada. Faca login novamente.");
            return;
        }

        List<ProfileUpsertRequest> body = new ArrayList<>();
        body.add(new ProfileUpsertRequest(userId, valueOrNull(displayName), valueOrNull(bio), valueOrNull(avatarUrl)));

        ApiClient.getSupabaseService()
                .upsertProfile(SupabaseConfig.getAnonKey(), buildAuthorization(accessToken), body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful()) {
                            callback.onError("Falha ao salvar perfil: HTTP " + response.code() + formatErrorBody(response.errorBody()));
                            return;
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        callback.onError("Erro de rede ao salvar perfil");
                    }
                });
    }

    private String valueOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String buildAuthorization(String accessToken) {
        return "Bearer " + accessToken.trim();
    }

    private String formatErrorBody(ResponseBody errorBody) {
        if (errorBody == null) {
            return "";
        }
        try {
            String body = errorBody.string();
            if (body == null || body.trim().isEmpty()) {
                return "";
            }
            return " - " + body;
        } catch (IOException e) {
            return "";
        }
    }
}

