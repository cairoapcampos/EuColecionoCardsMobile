package com.eucolecionocards.data.api;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente HTTP centralizado com SSL Pinning (certificate pinning).
 *
 * O CertificatePinner garante que o app só aceite conexões HTTPS cujo
 * certificado corresponda aos hashes SHA-256 esperados. Mesmo que um
 * atacante instale um certificado CA falso no dispositivo, a conexão
 * será rejeitada porque o pin não corresponde.
 */
public final class ApiClient {
    private static Retrofit retrofit;

    private ApiClient() {}

    /**
     * Pins SHA-256 do certificado leaf e da CA intermediária do Supabase.
     *
     * - Pin 1 (leaf):         certificado do servidor supabase.co
     * - Pin 2 (intermediária): Google Trust Services WE1
     *
     * Para atualizar os pins, execute:
     *   openssl s_client -servername SEU_PROJETO.supabase.co \
     *     -connect SEU_PROJETO.supabase.co:443 -showcerts </dev/null 2>/dev/null \
     *     | openssl x509 -pubkey -noout \
     *     | openssl pkey -pubin -outform der \
     *     | openssl dgst -sha256 -binary \
     *     | openssl enc -base64
     */
    private static final String PIN_LEAF = "sha256/GU2W4j1P24T3sqlI+o6YTnidzz0PI8fB/Gvd2ITfSZE=";
    private static final String PIN_INTERMEDIATE = "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=";

    public static SupabaseService getSupabaseService() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            // Extrai o hostname do Supabase a partir da URL configurada
            String hostname = extractHostname(SupabaseConfig.getBaseUrl());

            // Certificate pinning: rejeita conexões com certificados não esperados
            CertificatePinner certificatePinner = new CertificatePinner.Builder()
                    .add(hostname, PIN_LEAF)
                    .add(hostname, PIN_INTERMEDIATE)
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .certificatePinner(certificatePinner)
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.getBaseUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SupabaseService.class);
    }

    /**
     * Extrai o hostname de uma URL (ex.: "https://xyz.supabase.co/" → "*.supabase.co").
     * Usa wildcard para cobrir qualquer subdomínio do Supabase.
     */
    private static String extractHostname(String baseUrl) {
        try {
            String host = baseUrl.replaceFirst("https?://", "").split("[:/]")[0];
            // Se for um subdomínio do supabase.co, usa wildcard
            if (host.endsWith(".supabase.co")) {
                return "*.supabase.co";
            }
            return host;
        } catch (Exception e) {
            return "*.supabase.co";
        }
    }
}

