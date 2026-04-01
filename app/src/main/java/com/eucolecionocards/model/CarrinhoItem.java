package com.eucolecionocards.model;

public class CarrinhoItem {
    private Carta carta;
    private int quantidade;

    public CarrinhoItem(Carta carta) {
        this.carta = carta;
        this.quantidade = 1;
    }

    public Carta getCarta() {
        return carta;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void incrementar() {
        quantidade++;
    }

    public void decrementar() {
        if (quantidade > 0) quantidade--;
    }
}

