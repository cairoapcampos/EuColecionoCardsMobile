package com.eucolecionocards.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eucolecionocards.CarrinhoManager;
import com.eucolecionocards.R;
import com.eucolecionocards.model.CarrinhoItem;
import com.eucolecionocards.model.Carta;
import java.util.List;
import java.util.Locale;

public class CarrinhoAdapter extends RecyclerView.Adapter<CarrinhoAdapter.CarrinhoViewHolder> {
    private final List<CarrinhoItem> itens;
    private final Context context;
    private OnCarrinhoChangeListener changeListener;

    public interface OnCarrinhoChangeListener {
        void onCarrinhoChanged();
    }

    public CarrinhoAdapter(Context context, List<CarrinhoItem> itens) {
        this.context = context;
        this.itens = itens;
    }

    public void setOnCarrinhoChangeListener(OnCarrinhoChangeListener listener) {
        this.changeListener = listener;
    }

    @NonNull
    @Override
    public CarrinhoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_carrinho, parent, false);
        return new CarrinhoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CarrinhoViewHolder holder, int position) {
        CarrinhoItem item = itens.get(position);
        Carta carta = item.getCarta();

        holder.tvNomeCarta.setText(carta.getNome());
        holder.tvQuantidade.setText(String.valueOf(item.getQuantidade()));
        holder.tvEstoque.setText(context.getString(R.string.carrinho_item_estoque_formato, carta.getEstoque()));

        double subtotal = carta.getPreco() * item.getQuantidade();
        holder.tvSubtotal.setText(String.format(Locale.getDefault(), "R$ %.2f", subtotal));

        carregarImagem(carta, holder.imgCarta);

        // Desabilitar botão + se atingiu o estoque
        boolean podeAdicionar = CarrinhoManager.podeAdicionarMais(carta);
        holder.btnAdicionar.setEnabled(podeAdicionar);
        holder.btnAdicionar.setAlpha(podeAdicionar ? 1.0f : 0.4f);

        holder.btnAdicionar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            CarrinhoManager.adicionarCarta(carta);
            notifyItemChanged(pos);
            notificarMudanca();
        });

        holder.btnRemover.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            CarrinhoManager.removerCarta(carta);
            int novaQtd = CarrinhoManager.getQuantidade(carta);
            if (novaQtd <= 0) {
                itens.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, itens.size());
            } else {
                notifyItemChanged(pos);
            }
            notificarMudanca();
        });
    }

    private void notificarMudanca() {
        if (changeListener != null) {
            changeListener.onCarrinhoChanged();
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    private void carregarImagem(Carta carta, ImageView imageView) {
        String imagem = carta.getImagem();
        if (imagem != null && (imagem.startsWith("http://") || imagem.startsWith("https://"))) {
            Glide.with(context)
                    .load(imagem)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(imageView);
            return;
        }

        int resId = context.getResources().getIdentifier(imagem, "drawable", context.getPackageName());
        if (resId != 0) {
            imageView.setImageResource(resId);
        } else {
            imageView.setImageResource(R.drawable.logo);
        }
    }

    public static class CarrinhoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCarta;
        TextView tvNomeCarta, tvQuantidade, tvSubtotal, tvEstoque;
        Button btnRemover, btnAdicionar;
        CarrinhoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCarta = itemView.findViewById(R.id.imgCarta);
            tvNomeCarta = itemView.findViewById(R.id.tvNomeCarta);
            tvQuantidade = itemView.findViewById(R.id.tvQuantidade);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            tvEstoque = itemView.findViewById(R.id.tvEstoque);
            btnRemover = itemView.findViewById(R.id.btnRemover);
            btnAdicionar = itemView.findViewById(R.id.btnAdicionar);
        }
    }
}
