package com.eucolecionocards;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.eucolecionocards.adapter.CartasAdapter;
import com.eucolecionocards.adapter.CartasRecyclerAdapter;
import com.eucolecionocards.data.repository.CartaRepository;
import com.eucolecionocards.data.repository.FavoriteRepository;
import com.eucolecionocards.model.Carta;
import com.eucolecionocards.session.UserSession;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartasActivity extends AppCompatActivity {
    private boolean isGrid = false;
    private CartasAdapter listAdapter;
    private CartasRecyclerAdapter recyclerAdapter;
    private RecyclerView recyclerView;
    private ListView listView;

    private final ArrayList<Carta> todasCartas = new ArrayList<>();
    private final ArrayList<Carta> cartasFiltradas = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final FavoriteRepository favoriteRepository = new FavoriteRepository();

    private Spinner spinnerFiltroTipo;
    private Spinner spinnerFiltroRaridade;
    private Spinner spinnerFiltroColecao;
    private Spinner spinnerFiltroQualidade;
    private android.widget.EditText etBusca;
    private String currentUserId;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cartas);

        currentUserId = UserSession.getUserId(this);
        accessToken = UserSession.getAccessToken(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.listViewCartas);
        recyclerView = findViewById(R.id.recyclerViewCartas);

        listAdapter = new CartasAdapter(this, cartasFiltradas);
        listAdapter.setFavoriteHandler(new CartasAdapter.FavoriteHandler() {
            @Override
            public boolean isFavorite(String cardId) {
                return favoriteIds.contains(cardId);
            }

            @Override
            public void onFavoriteToggle(Carta carta, boolean shouldFavorite) {
                toggleFavorite(carta, shouldFavorite);
            }
        });
        listView.setAdapter(listAdapter);

        spinnerFiltroTipo = findViewById(R.id.spinnerFiltroTipo);
        spinnerFiltroRaridade = findViewById(R.id.spinnerFiltroRaridade);
        spinnerFiltroColecao = findViewById(R.id.spinnerFiltroColecao);
        spinnerFiltroQualidade = findViewById(R.id.spinnerFiltroQualidade);
        etBusca = findViewById(R.id.etBusca);

        configurarListenersDeFiltro();
        configurarAcoesDaTela();

        atualizarVisualizacao();
        carregarCartas();
        carregarFavoritos();
    }

    private void configurarAcoesDaTela() {
        FloatingActionButton fabPerfil = findViewById(R.id.fabPerfil);
        fabPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(CartasActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        FloatingActionButton fabCarrinho = findViewById(R.id.fabCarrinho);
        fabCarrinho.setOnClickListener(v -> {
            Intent intent = new Intent(CartasActivity.this, CarrinhoActivity.class);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    CartasActivity.this,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
            );
            startActivity(intent, options.toBundle());
        });

        ImageButton btnToggleView = findViewById(R.id.btnToggleView);
        btnToggleView.setOnClickListener(v -> {
            isGrid = !isGrid;
            atualizarVisualizacao();
            btnToggleView.setImageResource(isGrid ? R.drawable.ic_list_view : R.drawable.ic_grid_view);
        });
        btnToggleView.setImageResource(isGrid ? R.drawable.ic_list_view : R.drawable.ic_grid_view);
    }

    private void carregarCartas() {
        new CartaRepository().buscarCartas(new CartaRepository.LoadCartasCallback() {
            @Override
            public void onSuccess(List<Carta> cartas) {
                if (cartas == null || cartas.isEmpty()) {
                    atualizarDados(new ArrayList<>());
                    Toast.makeText(CartasActivity.this, "Nenhuma carta encontrada no Supabase.", Toast.LENGTH_LONG).show();
                    return;
                }
                atualizarDados(cartas);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CartasActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void carregarFavoritos() {
        favoriteRepository.loadFavorites(currentUserId, accessToken, new FavoriteRepository.LoadFavoritesCallback() {
            @Override
            public void onSuccess(Set<String> ids) {
                favoriteIds.clear();
                favoriteIds.addAll(ids);
                atualizarFavoritosNaTela();
            }

            @Override
            public void onError(String message) {
                if (isSessionError(message)) {
                    handleSessionExpired(message);
                    return;
                }
                Toast.makeText(CartasActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleFavorite(Carta carta, boolean shouldFavorite) {
        String cardId = carta.getId();
        if (cardId == null || cardId.trim().isEmpty()) {
            return;
        }

        if (shouldFavorite) {
            favoriteIds.add(cardId);
        } else {
            favoriteIds.remove(cardId);
        }
        atualizarFavoritosNaTela();

        FavoriteRepository.SimpleCallback callback = new FavoriteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                if (isSessionError(message)) {
                    handleSessionExpired(message);
                    return;
                }
                if (shouldFavorite) {
                    favoriteIds.remove(cardId);
                } else {
                    favoriteIds.add(cardId);
                }
                atualizarFavoritosNaTela();
                Toast.makeText(CartasActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };

        if (shouldFavorite) {
            favoriteRepository.addFavorite(currentUserId, cardId, accessToken, callback);
        } else {
            favoriteRepository.removeFavorite(currentUserId, cardId, accessToken, callback);
        }
    }


    private void atualizarFavoritosNaTela() {
        listAdapter.notifyDataSetChanged();
        if (recyclerAdapter != null) {
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    private void atualizarDados(List<Carta> cartas) {
        todasCartas.clear();
        todasCartas.addAll(cartas);
        preencherSpinners();
        aplicarFiltros();
    }

    private void preencherSpinners() {
        ArrayList<String> tipos = new ArrayList<>();
        ArrayList<String> raridades = new ArrayList<>();
        ArrayList<String> colecoes = new ArrayList<>();
        ArrayList<String> qualidades = new ArrayList<>();

        tipos.add("Todos");
        raridades.add("Todas");
        colecoes.add("Todas");
        qualidades.add("Todas");

        for (Carta c : todasCartas) {
            if (!tipos.contains(c.getTipo())) tipos.add(c.getTipo());
            if (!raridades.contains(c.getRaridade())) raridades.add(c.getRaridade());
            if (!colecoes.contains(c.getColecao())) colecoes.add(c.getColecao());
            if (!qualidades.contains(c.getQualidade())) qualidades.add(c.getQualidade());
        }

        String[] ordemQualidade = {"NM", "SP", "PL", "HP", "DM"};
        ArrayList<String> qualidadesOrdenadas = new ArrayList<>();
        qualidadesOrdenadas.add("Todas");
        for (String q : ordemQualidade) if (qualidades.contains(q)) qualidadesOrdenadas.add(q);
        for (String q : qualidades) if (!qualidadesOrdenadas.contains(q)) qualidadesOrdenadas.add(q);

        spinnerFiltroTipo.setAdapter(criarArrayAdapter(tipos));
        spinnerFiltroRaridade.setAdapter(criarArrayAdapter(raridades));
        spinnerFiltroColecao.setAdapter(criarArrayAdapter(colecoes));
        spinnerFiltroQualidade.setAdapter(criarArrayAdapter(qualidadesOrdenadas));
    }

    private ArrayAdapter<String> criarArrayAdapter(List<String> itens) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itens);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void configurarListenersDeFiltro() {
        android.widget.AdapterView.OnItemSelectedListener listener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltros();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        };

        spinnerFiltroTipo.setOnItemSelectedListener(listener);
        spinnerFiltroRaridade.setOnItemSelectedListener(listener);
        spinnerFiltroColecao.setOnItemSelectedListener(listener);
        spinnerFiltroQualidade.setOnItemSelectedListener(listener);
        etBusca.setOnEditorActionListener((v, actionId, event) -> {
            aplicarFiltros();
            return false;
        });
    }

    private void aplicarFiltros() {
        if (spinnerFiltroTipo.getAdapter() == null) {
            return;
        }

        String tipoSel = spinnerFiltroTipo.getSelectedItem().toString();
        String raridadeSel = spinnerFiltroRaridade.getSelectedItem().toString();
        String colecaoSel = spinnerFiltroColecao.getSelectedItem().toString();
        String qualidadeSel = spinnerFiltroQualidade.getSelectedItem().toString();
        String busca = etBusca.getText().toString().trim().toLowerCase();

        cartasFiltradas.clear();
        for (Carta c : todasCartas) {
            boolean ok = true;
            if (!"Todos".equals(tipoSel) && !c.getTipo().equals(tipoSel)) ok = false;
            if (!"Todas".equals(raridadeSel) && !c.getRaridade().equals(raridadeSel)) ok = false;
            if (!"Todas".equals(colecaoSel) && !c.getColecao().equals(colecaoSel)) ok = false;
            if (!"Todas".equals(qualidadeSel) && !c.getQualidade().equals(qualidadeSel)) ok = false;
            if (!busca.isEmpty() && !(c.getNome().toLowerCase().contains(busca) || c.getDescricao().toLowerCase().contains(busca))) ok = false;
            if (ok) cartasFiltradas.add(c);
        }

        listAdapter.setCartas(cartasFiltradas);
        if (recyclerAdapter != null) {
            recyclerAdapter.setCartas(cartasFiltradas);
        }
    }

    private void atualizarVisualizacao() {
        if (isGrid) {
            listView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            recyclerAdapter = new CartasRecyclerAdapter(this, cartasFiltradas, true);
            recyclerAdapter.setFavoriteHandler(new CartasRecyclerAdapter.FavoriteHandler() {
                @Override
                public boolean isFavorite(String cardId) {
                    return favoriteIds.contains(cardId);
                }

                @Override
                public void onFavoriteToggle(Carta carta, boolean shouldFavorite) {
                    toggleFavorite(carta, shouldFavorite);
                }
            });
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.setOnCartaClickListener(this::mostrarImagemGrande);
        } else {
            recyclerView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarImagemGrande(Carta carta) {
        // Mantido para implementacao futura de detalhe em tela/dialog.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cartas, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mostrarConfirmacaoLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarConfirmacaoLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair")
                .setMessage("Deseja realmente sair da sua conta?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sair", (dialog, which) -> executarLogout())
                .show();
    }

    private boolean isSessionError(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("sessao expirada") || lower.contains("login novamente") || lower.contains("http 401") || lower.contains("pgrst303");
    }

    private void handleSessionExpired(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        UserSession.clear(this);
        Intent intent = new Intent(CartasActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void executarLogout() {
        UserSession.clear(CartasActivity.this);
        Intent intent = new Intent(CartasActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
