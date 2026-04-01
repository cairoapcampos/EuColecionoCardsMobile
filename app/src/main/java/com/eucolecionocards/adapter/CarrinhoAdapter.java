package com.eucolecionocards.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eucolecionocards.R;
import com.eucolecionocards.model.CarrinhoItem;
import com.eucolecionocards.model.Carta;
import java.util.List;

public class CarrinhoAdapter extends RecyclerView.Adapter<CarrinhoAdapter.CarrinhoViewHolder> {
    private final List<CarrinhoItem> itens;
    private final Context context;

    public CarrinhoAdapter(Context context, List<CarrinhoItem> itens) {
        this.context = context;
        this.itens = itens;
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
        holder.tvQuantidade.setText("Qtd: " + item.getQuantidade());
        double subtotal = carta.getPreco() * item.getQuantidade();
        holder.tvSubtotal.setText(String.format("R$ %.2f", subtotal));
        carregarImagem(carta, holder.imgCarta);
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

    static class CarrinhoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCarta;
        TextView tvNomeCarta, tvQuantidade, tvSubtotal;
        CarrinhoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCarta = itemView.findViewById(R.id.imgCarta);
            tvNomeCarta = itemView.findViewById(R.id.tvNomeCarta);
            tvQuantidade = itemView.findViewById(R.id.tvQuantidade);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
        }
    }
}
