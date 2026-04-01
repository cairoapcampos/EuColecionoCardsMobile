package com.eucolecionocards.data.repository;

import com.eucolecionocards.data.api.ApiClient;
import com.eucolecionocards.data.api.SupabaseConfig;
import com.eucolecionocards.data.remote.FavoriteDto;
import com.eucolecionocards.data.remote.FavoriteUpsertRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteRepository {

    public interface LoadFavoritesCallback {
        void onSuccess(Set<String> favoriteIds);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public void loadFavorites(String userId, String accessToken, LoadFavoritesCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado");
            return;
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Sessao expirada. Faca login novamente.");
            return;
        }

        String auth = buildAuthorization(accessToken);
        ApiClient.getSupabaseService()
                .getFavorites(SupabaseConfig.getAnonKey(), auth, "card_id", "eq." + userId)
                .enqueue(new Callback<List<FavoriteDto>>() {
                    @Override
                    public void onResponse(Call<List<FavoriteDto>> call, Response<List<FavoriteDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Falha ao carregar favoritos: HTTP " + response.code() + formatErrorBody(response.errorBody()));
                            return;
                        }
                        HashSet<String> ids = new HashSet<>();
                        for (FavoriteDto dto : response.body()) {
                            if (dto != null && dto.cardId != null && !dto.cardId.trim().isEmpty()) {
                                ids.add(dto.cardId.trim());
                            }
                        }
                        callback.onSuccess(ids);
                    }

                    @Override
                    public void onFailure(Call<List<FavoriteDto>> call, Throwable t) {
                        callback.onError("Erro de rede ao carregar favoritos");
                    }
                });
    }

    public void addFavorite(String userId, String cardId, String accessToken, SimpleCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado");
            return;
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Sessao expirada. Faca login novamente.");
            return;
        }

        String auth = buildAuthorization(accessToken);
        List<FavoriteUpsertRequest> body = new ArrayList<>();
        body.add(new FavoriteUpsertRequest(userId, cardId));

        ApiClient.getSupabaseService()
                .upsertFavorite(SupabaseConfig.getAnonKey(), auth, body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful()) {
                            String rawError = readErrorBody(response.errorBody());
                            callback.onError(buildFavoriteErrorMessage("salvar", response.code(), rawError));
                            return;
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        callback.onError("Erro de rede ao salvar favorito");
                    }
                });
    }

    public void removeFavorite(String userId, String cardId, String accessToken, SimpleCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado");
            return;
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Sessao expirada. Faca login novamente.");
            return;
        }

        String auth = buildAuthorization(accessToken);
        ApiClient.getSupabaseService()
                .deleteFavorite(SupabaseConfig.getAnonKey(), auth, "eq." + userId, "eq." + cardId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful()) {
                            String rawError = readErrorBody(response.errorBody());
                            callback.onError(buildFavoriteErrorMessage("remover", response.code(), rawError));
                            return;
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        callback.onError("Erro de rede ao remover favorito");
                    }
                });
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

    private String readErrorBody(ResponseBody errorBody) {
        if (errorBody == null) {
            return "";
        }
        try {
            String body = errorBody.string();
            return body == null ? "" : body;
        } catch (IOException e) {
            return "";
        }
    }

    private String buildFavoriteErrorMessage(String action, int httpCode, String rawError) {
        String lower = rawError == null ? "" : rawError.toLowerCase();
        if (httpCode == 401 || lower.contains("pgrst303")) {
            return "Sessao expirada ou invalida. Faca login novamente.";
        }
        if (httpCode == 403 || lower.contains("42501")) {
            return "Sem permissao para " + action + " favorito. Verifique as policies RLS da tabela favorites/profiles no Supabase.";
        }
        String details = (rawError == null || rawError.trim().isEmpty()) ? "" : " - " + rawError;
        return "Falha ao " + action + " favorito: HTTP " + httpCode + details;
    }

    private String buildAuthorization(String accessToken) {
        return "Bearer " + accessToken.trim();
    }
}

