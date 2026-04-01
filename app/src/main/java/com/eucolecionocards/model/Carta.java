package com.eucolecionocards.model;

public class Carta {
    private String id;
    private String code;
    private String nome;
    private String descricao;
    private String imagem;
    private double preco;
    private String tipo;
    private String raridade;
    private String colecao;
    private int ano;
    private String qualidade;
    private int estoque;

    public Carta(String id, String code, String nome, String descricao, String imagem, double preco, String tipo, String raridade, String colecao, int ano, String qualidade, int estoque) {
        this.id = id;
        this.code = code;
        this.nome = nome;
        this.descricao = descricao;
        this.imagem = imagem;
        this.preco = preco;
        this.tipo = tipo;
        this.raridade = raridade;
        this.colecao = colecao;
        this.ano = ano;
        this.qualidade = qualidade;
        this.estoque = Math.max(0, estoque);
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getImagem() { return imagem; }
    public double getPreco() { return preco; }
    public String getTipo() { return tipo; }
    public String getRaridade() { return raridade; }
    public String getColecao() { return colecao; }
    public int getAno() { return ano; }
    public String getQualidade() { return qualidade; }
    public int getEstoque() { return estoque; }
}