package com.eucolecionocards.data.repository;

import com.eucolecionocards.data.api.ApiClient;
import com.eucolecionocards.data.api.SupabaseConfig;
import com.eucolecionocards.data.remote.AuthSessionDto;
import com.eucolecionocards.data.remote.AuthSignInRequest;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(AuthSessionDto session);
        void onError(String message);
    }

    public void signIn(String email, String password, AuthCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado no gradle.properties");
            return;
        }

        ApiClient.getSupabaseService()
                .signInWithPassword(SupabaseConfig.getAnonKey(), "password", new AuthSignInRequest(email, password))
                .enqueue(new Callback<AuthSessionDto>() {
                    @Override
                    public void onResponse(Call<AuthSessionDto> call, Response<AuthSessionDto> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Falha no login: HTTP " + response.code() + formatErrorBody(response.errorBody()));
                            return;
                        }
                        AuthSessionDto session = response.body();
                        if (session.user == null || session.user.id == null || session.user.id.trim().isEmpty()) {
                            callback.onError("Login retornou usuario invalido");
                            return;
                        }
                        if (session.accessToken == null || session.accessToken.trim().isEmpty()) {
                            callback.onError("Login sem token de acesso");
                            return;
                        }
                        callback.onSuccess(session);
                    }

                    @Override
                    public void onFailure(Call<AuthSessionDto> call, Throwable t) {
                        callback.onError("Erro de rede no login: " + (t.getMessage() == null ? "desconhecido" : t.getMessage()));
                    }
                });
    }

    public void signUp(String email, String password, AuthCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado no gradle.properties");
            return;
        }

        ApiClient.getSupabaseService()
                .signUpWithEmail(SupabaseConfig.getAnonKey(), new AuthSignInRequest(email, password))
                .enqueue(new Callback<AuthSessionDto>() {
                    @Override
                    public void onResponse(Call<AuthSessionDto> call, Response<AuthSessionDto> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            String rawError = readErrorBody(response.errorBody());
                            callback.onError(buildSignUpErrorMessage(response.code(), rawError));
                            return;
                        }
                        AuthSessionDto session = response.body();
                        if (session.user == null || session.user.id == null || session.user.id.trim().isEmpty()) {
                            callback.onError("Cadastro retornou usuario invalido");
                            return;
                        }
                        callback.onSuccess(session);
                    }

                    @Override
                    public void onFailure(Call<AuthSessionDto> call, Throwable t) {
                        callback.onError("Erro de rede no cadastro: " + (t.getMessage() == null ? "desconhecido" : t.getMessage()));
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

    private String buildSignUpErrorMessage(int httpCode, String rawError) {
        String lower = rawError == null ? "" : rawError.toLowerCase();
        if (httpCode == 429 || lower.contains("over_email_send_rate_limit")) {
            return "Muitas tentativas de cadastro em pouco tempo. Aguarde alguns minutos e tente novamente.";
        }
        if (lower.contains("user_already_exists")) {
            return "Este e-mail ja esta cadastrado. Tente entrar com sua conta.";
        }
        String details = (rawError == null || rawError.trim().isEmpty()) ? "" : " - " + rawError;
        return "Falha no cadastro: HTTP " + httpCode + details;
    }
}

