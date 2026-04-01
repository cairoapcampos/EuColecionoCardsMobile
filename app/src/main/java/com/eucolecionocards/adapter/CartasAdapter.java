package com.eucolecionocards.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.eucolecionocards.model.Carta;
import com.eucolecionocards.R;
import com.eucolecionocards.CarrinhoManager;
import java.util.List;

public class CartasAdapter extends BaseAdapter {
    private Context context;
    private List<Carta> cartas;
    private OnCarrinhoChangeListener carrinhoChangeListener;
    private FavoriteHandler favoriteHandler;

    public interface OnCarrinhoChangeListener {
        void onCarrinhoChanged();
    }

    public interface FavoriteHandler {
        boolean isFavorite(String cardId);
        void onFavoriteToggle(Carta carta, boolean shouldFavorite);
    }

    public CartasAdapter(Context context, List<Carta> cartas) {
        this.context = context;
        this.cartas = cartas;
    }

    public void setCartas(List<Carta> cartas) {
        this.cartas = cartas;
        notifyDataSetChanged();
    }

    public void setOnCarrinhoChangeListener(OnCarrinhoChangeListener listener) {
        this.carrinhoChangeListener = listener;
    }

    public void setFavoriteHandler(FavoriteHandler favoriteHandler) {
        this.favoriteHandler = favoriteHandler;
    }

    @Override
    public int getCount() { return cartas.size(); }
    @Override
    public Object getItem(int i) { return cartas.get(i); }
    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_carta, parent, false);
        }
        final View itemView = convertView;
        Carta carta = cartas.get(i);
        TextView tvNome = convertView.findViewById(R.id.tvNome);
        TextView tvTipo = convertView.findViewById(R.id.tvTipo);
        TextView tvDescricao = convertView.findViewById(R.id.tvDescricao);
        TextView tvPreco = convertView.findViewById(R.id.tvPreco);
        TextView tvQuantidade = convertView.findViewById(R.id.tvQuantidade);
        ImageView ivImagem = convertView.findViewById(R.id.ivImagem);
        Button btnMais = convertView.findViewById(R.id.btnAdicionar);
        Button btnMenos = convertView.findViewById(R.id.btnRemover);
        TextView tvRaridade = convertView.findViewById(R.id.tvRaridade);
        TextView tvColecao = convertView.findViewById(R.id.tvColecao);
        TextView tvAno = convertView.findViewById(R.id.tvAno);
        TextView tvQualidade = convertView.findViewById(R.id.tvQualidade);
        TextView tvEstoque = convertView.findViewById(R.id.tvEstoque);
        ImageView ivFavorito = convertView.findViewById(R.id.ivFavorito);
        tvNome.setText(carta.getNome());
        tvTipo.setText(carta.getTipo());
        tvDescricao.setText(carta.getDescricao());
        tvPreco.setText("R$ " + String.format("%.2f", carta.getPreco()));
        tvRaridade.setText(carta.getRaridade());
        tvColecao.setText(carta.getColecao());
        tvAno.setText(String.valueOf(carta.getAno()));
        tvQualidade.setText(carta.getQualidade());
        tvEstoque.setText(carta.getEstoque() > 0 ? "Estoque: " + carta.getEstoque() : "Esgotado");

        String imagem = carta.getImagem();
        if (imagem != null && (imagem.startsWith("http://") || imagem.startsWith("https://"))) {
            Glide.with(context)
                    .load(imagem)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(ivImagem);
        } else {
            int resId = context.getResources().getIdentifier(carta.getImagem(), "drawable", context.getPackageName());
            if (resId != 0) {
                ivImagem.setImageResource(resId);
            } else {
                ivImagem.setImageResource(R.drawable.logo);
            }
        }

        int quantidade = CarrinhoManager.getQuantidade(carta);
        tvQuantidade.setText(String.valueOf(quantidade));
        btnMais.setEnabled(CarrinhoManager.podeAdicionarMais(carta));

        btnMais.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CarrinhoManager.adicionarCarta(carta)) {
                    Toast.makeText(context, "Estoque maximo atingido para esta carta.", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    return;
                }
                Animation anim = AnimationUtils.loadAnimation(context, R.anim.scale_in);
                itemView.startAnimation(anim);
                notifyDataSetChanged();
                if (carrinhoChangeListener != null) {
                    carrinhoChangeListener.onCarrinhoChanged();
                }
            }
        });

        btnMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CarrinhoManager.getQuantidade(carta) > 0) {
                    CarrinhoManager.removerCarta(carta);
                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                    itemView.startAnimation(anim);
                    notifyDataSetChanged();
                    if (carrinhoChangeListener != null) {
                        carrinhoChangeListener.onCarrinhoChanged();
                    }
                }
            }
        });

        boolean favorito = favoriteHandler != null && favoriteHandler.isFavorite(carta.getId());
        ivFavorito.setImageResource(favorito ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        ivFavorito.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean atual = favoriteHandler != null && favoriteHandler.isFavorite(carta.getId());
                if (favoriteHandler != null) {
                    favoriteHandler.onFavoriteToggle(carta, !atual);
                }
            }
        });

        convertView.setOnClickListener(null);
        return convertView;
    }
}
