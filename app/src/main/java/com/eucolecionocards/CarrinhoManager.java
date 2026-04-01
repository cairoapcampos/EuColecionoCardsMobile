package com.eucolecionocards;

import com.eucolecionocards.model.Carta;
import com.eucolecionocards.model.CarrinhoItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarrinhoManager {
    private static final Map<String, CarrinhoItem> carrinho = new HashMap<>();

    public static boolean adicionarCarta(Carta carta) {
        if (carta == null || carta.getEstoque() <= 0) {
            return false;
        }

        CarrinhoItem item = carrinho.get(carta.getId());
        if (item == null) {
            if (carta.getEstoque() < 1) {
                return false;
            }
            carrinho.put(carta.getId(), new CarrinhoItem(carta));
            return true;
        }

        if (item.getQuantidade() >= carta.getEstoque()) {
            return false;
        } else {
            item.incrementar();
            return true;
        }
    }

    public static List<CarrinhoItem> getCarrinho() {
        return new ArrayList<>(carrinho.values());
    }

    public static void limparCarrinho() {
        carrinho.clear();
    }

    public static void removerCarta(Carta carta) {
        CarrinhoItem item = carrinho.get(carta.getId());
        if (item != null) {
            item.decrementar();
            if (item.getQuantidade() <= 0) {
                carrinho.remove(carta.getId());
            }
        }
    }

    public static boolean contemCarta(Carta carta) {
        return carrinho.containsKey(carta.getId());
    }

    public static int getQuantidade(Carta carta) {
        CarrinhoItem item = carrinho.get(carta.getId());
        return item != null ? item.getQuantidade() : 0;
    }

    public static boolean podeAdicionarMais(Carta carta) {
        return carta != null && carta.getEstoque() > getQuantidade(carta);
    }
}
