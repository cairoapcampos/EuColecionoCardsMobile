package com.eucolecionocards.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eucolecionocards.R;
import com.eucolecionocards.model.Carta;
import java.util.ArrayList;
import java.util.List;

public class CartasRecyclerAdapter extends RecyclerView.Adapter<CartasRecyclerAdapter.CartaViewHolder> {
    private final Context context;
    private final boolean isGrid;
    private final List<Carta> cartas = new ArrayList<>();
    private OnCartaClickListener listener;
    private FavoriteHandler favoriteHandler;

    public interface OnCartaClickListener {
        void onCartaClick(Carta carta);
    }

    public interface FavoriteHandler {
        boolean isFavorite(String cardId);
        void onFavoriteToggle(Carta carta, boolean shouldFavorite);
    }

    public CartasRecyclerAdapter(Context context, List<Carta> cartas, boolean isGrid) {
        this.context = context;
        this.isGrid = isGrid;
        setCartas(cartas);
    }

    public void setOnCartaClickListener(OnCartaClickListener listener) {
        this.listener = listener;
    }

    public void setFavoriteHandler(FavoriteHandler favoriteHandler) {
        this.favoriteHandler = favoriteHandler;
    }

    public void setCartas(List<Carta> novasCartas) {
        cartas.clear();
        if (novasCartas != null) {
            cartas.addAll(novasCartas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isGrid ? R.layout.item_carta_grid : R.layout.item_carta;
        View v = LayoutInflater.from(context).inflate(layout, parent, false);
        return new CartaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartaViewHolder holder, int position) {
        Carta carta = cartas.get(position);
        if (isGrid) {
            ImageView ivImagem = holder.itemView.findViewById(R.id.ivImagemGrid);
            TextView tvNome = holder.itemView.findViewById(R.id.tvNomeGrid);
            TextView tvPreco = holder.itemView.findViewById(R.id.tvPrecoGrid);
            TextView tvEstoque = holder.itemView.findViewById(R.id.tvEstoqueGrid);
            ImageView ivFavorito = holder.itemView.findViewById(R.id.ivFavoritoGrid);
            TextView tvQuantidade = holder.itemView.findViewById(R.id.tvQuantidadeGrid);
            View btnMais = holder.itemView.findViewById(R.id.btnAdicionarGrid);
            View btnMenos = holder.itemView.findViewById(R.id.btnRemoverGrid);

            carregarImagem(carta, ivImagem);
            tvNome.setText(carta.getNome());
            tvPreco.setText("R$ " + String.format("%.2f", carta.getPreco()));
            tvEstoque.setText(carta.getEstoque() > 0 ? "Estoque: " + carta.getEstoque() : "Esgotado");

            int quantidade = com.eucolecionocards.CarrinhoManager.getQuantidade(carta);
            tvQuantidade.setText(String.valueOf(quantidade));
            btnMais.setEnabled(com.eucolecionocards.CarrinhoManager.podeAdicionarMais(carta));

            btnMais.setOnClickListener(v -> {
                if (!com.eucolecionocards.CarrinhoManager.adicionarCarta(carta)) {
                    Toast.makeText(context, "Estoque maximo atingido para esta carta.", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    return;
                }
                android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(context, com.eucolecionocards.R.anim.scale_in);
                holder.itemView.startAnimation(anim);
                notifyDataSetChanged();
            });

            btnMenos.setOnClickListener(v -> {
                if (com.eucolecionocards.CarrinhoManager.getQuantidade(carta) > 0) {
                    com.eucolecionocards.CarrinhoManager.removerCarta(carta);
                    android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(context, com.eucolecionocards.R.anim.fade_out);
                    holder.itemView.startAnimation(anim);
                    notifyDataSetChanged();
                }
            });

            boolean favorito = favoriteHandler != null && favoriteHandler.isFavorite(carta.getId());
            ivFavorito.setImageResource(favorito ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            ivFavorito.setOnClickListener(v -> {
                boolean atual = favoriteHandler != null && favoriteHandler.isFavorite(carta.getId());
                if (favoriteHandler != null) {
                    favoriteHandler.onFavoriteToggle(carta, !atual);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCartaClick(carta);
            });
        } else {
            TextView tvNome = holder.itemView.findViewById(R.id.tvNome);
            TextView tvTipo = holder.itemView.findViewById(R.id.tvTipo);
            TextView tvDescricao = holder.itemView.findViewById(R.id.tvDescricao);
            TextView tvPreco = holder.itemView.findViewById(R.id.tvPreco);
            TextView tvQuantidade = holder.itemView.findViewById(R.id.tvQuantidade);
            ImageView ivImagem = holder.itemView.findViewById(R.id.ivImagem);
            ImageView ivFavorito = holder.itemView.findViewById(R.id.ivFavorito);
            TextView tvRaridade = holder.itemView.findViewById(R.id.tvRaridade);
            TextView tvColecao = holder.itemView.findViewById(R.id.tvColecao);
            TextView tvAno = holder.itemView.findViewById(R.id.tvAno);
            TextView tvQualidade = holder.itemView.findViewById(R.id.tvQualidade);
            TextView tvEstoque = holder.itemView.findViewById(R.id.tvEstoque);
            View btnMais = holder.itemView.findViewById(R.id.btnAdicionar);
            View btnMenos = holder.itemView.findViewById(R.id.btnRemover);

            tvNome.setText(carta.getNome());
            tvTipo.setText(carta.getTipo());
            tvDescricao.setText(carta.getDescricao());
            tvPreco.setText("R$ " + String.format("%.2f", carta.getPreco()));
            tvRaridade.setText(carta.getRaridade());
            tvColecao.setText(carta.getColecao());
            tvAno.setText(String.valueOf(carta.getAno()));
            tvQualidade.setText(carta.getQualidade());
            tvEstoque.setText(carta.getEstoque() > 0 ? "Estoque: " + carta.getEstoque() : "Esgotado");
            carregarImagem(carta, ivImagem);

            int quantidade = com.eucolecionocards.CarrinhoManager.getQuantidade(carta);
            tvQuantidade.setText(String.valueOf(quantidade));
            btnMais.setEnabled(com.eucolecionocards.CarrinhoManager.podeAdicionarMais(carta));

            btnMais.setOnClickListener(v -> {
                if (!com.eucolecionocards.CarrinhoManager.adicionarCarta(carta)) {
                    Toast.makeText(context, "Estoque maximo atingido para esta carta.", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    return;
                }
                android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(context, com.eucolecionocards.R.anim.scale_in);
                holder.itemView.startAnimation(anim);
                notifyDataSetChanged();
            });

            btnMenos.setOnClickListener(v -> {
                if (com.eucolecionocards.CarrinhoManager.getQuantidade(carta) > 0) {
                    com.eucolecionocards.CarrinhoManager.removerCarta(carta);
                    android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(context, com.eucolecionocards.R.anim.fade_out);
                    holder.itemView.startAnimation(anim);
                    notifyDataSetChanged();
                }
            });

            boolean favorito = favoriteHandler != null && favoriteHandler.isFavorite(carta.getId());
            ivFavorito.setImageResource(favorito ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            ivFavorito.setOnClickListener(v -> {
                boolean atual = favoriteHandler != null && favoriteHandler.isFavorite(carta.getId());
                if (favoriteHandler != null) {
                    favoriteHandler.onFavoriteToggle(carta, !atual);
                }
            });
            holder.itemView.setOnClickListener(null);
        }
    }

    private void carregarImagem(Carta carta, ImageView imageView) {
        String imagem = carta.getImagem();
        if (imagem != null && (imagem.startsWith("http://") || imagem.startsWith("https://"))) {
            Glide.with(context)
                    .load(imagem)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(imageView);
        } else {
            int resId = context.getResources().getIdentifier(carta.getImagem(), "drawable", context.getPackageName());
            if (resId != 0) {
                imageView.setImageResource(resId);
            } else {
                imageView.setImageResource(R.drawable.logo);
            }
        }
    }

    @Override
    public int getItemCount() {
        return cartas.size();
    }

    static class CartaViewHolder extends RecyclerView.ViewHolder {
        CartaViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
