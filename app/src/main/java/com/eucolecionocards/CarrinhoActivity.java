package com.eucolecionocards;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.eucolecionocards.adapter.CarrinhoAdapter;
import com.eucolecionocards.model.CarrinhoItem;
import java.util.List;

public class CarrinhoActivity extends AppCompatActivity {
    private CarrinhoAdapter adapter;
    private TextView tvTotalCarrinho;
    private LinearLayout layoutCarrinhoVazio;
    private RecyclerView rvCarrinho;
    private Button btnPagar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrinho);

        rvCarrinho = findViewById(R.id.rvCarrinho);
        tvTotalCarrinho = findViewById(R.id.tvTotalCarrinho);
        layoutCarrinhoVazio = findViewById(R.id.layoutCarrinhoVazio);
        btnPagar = findViewById(R.id.btnPagar);
        Button btnVoltarCarrinho = findViewById(R.id.btnVoltarCarrinho);

        List<CarrinhoItem> itens = CarrinhoManager.getCarrinho();
        adapter = new CarrinhoAdapter(this, itens);
        adapter.setOnCarrinhoChangeListener(() -> {
            List<CarrinhoItem> atual = CarrinhoManager.getCarrinho();
            atualizarEstadoCarrinho(atual);
        });
        rvCarrinho.setLayoutManager(new LinearLayoutManager(this));
        rvCarrinho.setAdapter(adapter);

        atualizarEstadoCarrinho(itens);

        btnPagar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CarrinhoActivity.this, PagamentoActivity.class);
                startActivity(intent);
            }
        });

        btnVoltarCarrinho.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void atualizarEstadoCarrinho(List<CarrinhoItem> itens) {
        boolean vazio = itens == null || itens.isEmpty();
        if (vazio) {
            layoutCarrinhoVazio.setVisibility(View.VISIBLE);
            rvCarrinho.setVisibility(View.GONE);
            tvTotalCarrinho.setVisibility(View.GONE);
            btnPagar.setVisibility(View.GONE);
        } else {
            layoutCarrinhoVazio.setVisibility(View.GONE);
            rvCarrinho.setVisibility(View.VISIBLE);
            tvTotalCarrinho.setVisibility(View.VISIBLE);
            btnPagar.setVisibility(View.VISIBLE);
            double total = 0.0;
            for (CarrinhoItem item : itens) {
                total += item.getCarta().getPreco() * item.getQuantidade();
            }
            tvTotalCarrinho.setText(getString(R.string.carrinho_total_formato, total));
        }
    }

    @Override
    public void finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_right, R.anim.slide_out_left);
        }
        super.finish();
    }
}
