package com.eucolecionocards;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrinho);

        RecyclerView rvCarrinho = findViewById(R.id.rvCarrinho);
        tvTotalCarrinho = findViewById(R.id.tvTotalCarrinho);
        Button btnPagar = findViewById(R.id.btnPagar);
        Button btnVoltarCarrinho = findViewById(R.id.btnVoltarCarrinho);

        List<CarrinhoItem> itens = CarrinhoManager.getCarrinho();
        adapter = new CarrinhoAdapter(this, itens);
        rvCarrinho.setLayoutManager(new LinearLayoutManager(this));
        rvCarrinho.setAdapter(adapter);

        atualizarTotal(itens);

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

    private void atualizarTotal(List<CarrinhoItem> itens) {
        double total = 0.0;
        for (CarrinhoItem item : itens) {
            total += item.getCarta().getPreco() * item.getQuantidade();
        }
        tvTotalCarrinho.setText(String.format("Total: R$ %.2f", total));
    }

    @Override
    public void finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_right, R.anim.slide_out_left);
        }
        super.finish();
    }
}
