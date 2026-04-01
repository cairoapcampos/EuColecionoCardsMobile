package com.eucolecionocards.data.repository;

import com.eucolecionocards.data.api.ApiClient;
import com.eucolecionocards.data.api.SupabaseConfig;
import com.eucolecionocards.data.remote.CardDto;
import com.eucolecionocards.model.Carta;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartaRepository {

    public interface LoadCartasCallback {
        void onSuccess(List<Carta> cartas);
        void onError(String message);
    }

    public void buscarCartas(LoadCartasCallback callback) {
        if (!SupabaseConfig.isConfigured()) {
            callback.onError("Supabase nao configurado no gradle.properties");
            return;
        }

        String auth = "Bearer " + SupabaseConfig.getAnonKey();
        String select = "id,code,name,description,image_path,type,rarity,collection,year,quality,price,stock_quantity";

        ApiClient.getSupabaseService()
                .getCards(SupabaseConfig.getAnonKey(), auth, select, "year.asc")
                .enqueue(new Callback<List<CardDto>>() {
                    @Override
                    public void onResponse(Call<List<CardDto>> call, Response<List<CardDto>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Falha ao buscar cartas: HTTP " + response.code());
                            return;
                        }
                        callback.onSuccess(mapper(response.body()));
                    }

                    @Override
                    public void onFailure(Call<List<CardDto>> call, Throwable t) {
                        callback.onError("Erro de rede: " + (t.getMessage() == null ? "desconhecido" : t.getMessage()));
                    }
                });
    }

    private List<Carta> mapper(List<CardDto> dtos) {
        ArrayList<Carta> cartas = new ArrayList<>();
        for (CardDto dto : dtos) {
            String id = valueOrDefault(dto.id, dto.code);
            String code = valueOrDefault(dto.code, dto.name);
            String nome = valueOrDefault(dto.name, "Carta sem nome");
            String descricao = valueOrDefault(dto.description, "");
            String imagem = SupabaseConfig.buildPublicCardImageUrl(dto.imagePath);
            String tipo = valueOrDefault(dto.type, "Desconhecido");
            String raridade = valueOrDefault(dto.rarity, "Nao informado");
            String colecao = valueOrDefault(dto.collection, "Nao informada");
            int ano = dto.year == null ? 0 : dto.year;
            String qualidade = valueOrDefault(dto.quality, "Nao informada");
            double preco = dto.price == null ? 0.0 : dto.price;
            int estoque = dto.stockQuantity == null ? 0 : Math.max(0, dto.stockQuantity);

            cartas.add(new Carta(id, code, nome, descricao, imagem, preco, tipo, raridade, colecao, ano, qualidade, estoque));
        }

        // Garante ordem natural por codigo (card1, card2, ... card24), mesmo com resposta nao deterministica.
        Collections.sort(cartas, Comparator
                .comparingInt((Carta c) -> extractTrailingNumber(c.getCode()))
                .thenComparing(Carta::getCode, String.CASE_INSENSITIVE_ORDER));

        return cartas;
    }

    private int extractTrailingNumber(String code) {
        if (code == null) {
            return Integer.MAX_VALUE;
        }
        String digits = code.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}
