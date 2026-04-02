package com.eucolecionocards;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.eucolecionocards.data.remote.ProfileDto;
import com.eucolecionocards.data.repository.FavoriteRepository;
import com.eucolecionocards.data.repository.ProfileRepository;
import com.eucolecionocards.security.SecurePrefs;
import com.eucolecionocards.session.UserSession;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {
    private static final int MIN_NOME_LEN = 2;
    private ImageView ivAvatar;
    private EditText etNome, etBio;
    private TextView tvFavoritas;
    private SharedPreferences prefs;
    private final FavoriteRepository favoriteRepository = new FavoriteRepository();
    private final ProfileRepository profileRepository = new ProfileRepository();
    private ActivityResultLauncher<String> imagePickerLauncher;
    private boolean forceCompleteProfile;
    private String accessToken;
    private String userId;
    private String avatarUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        forceCompleteProfile = getIntent().getBooleanExtra("force_complete_profile", false);
        accessToken = UserSession.getAccessToken(this);
        userId = UserSession.getUserId(this);

        prefs = SecurePrefs.get(this);
        ivAvatar = findViewById(R.id.ivAvatar);
        etNome = findViewById(R.id.etNome);
        etBio = findViewById(R.id.etBio);
        tvFavoritas = findViewById(R.id.tvFavoritas);
        Button btnSalvar = findViewById(R.id.btnSalvarPerfil);
        Button btnVoltarCartas = findViewById(R.id.btnVoltarCartas);

        if (forceCompleteProfile) {
            btnVoltarCartas.setVisibility(android.view.View.GONE);
        } else {
            btnVoltarCartas.setOnClickListener(v -> finish());
        }

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), imageUri -> {
            if (imageUri == null) {
                return;
            }
            avatarUri = imageUri.toString();
            prefs.edit().putString("perfil_avatar", avatarUri).apply();
            carregarAvatar(avatarUri);
        });

        carregarPerfilLocal();
        carregarPerfilRemoto();
        atualizarEstatisticas();

        ivAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSalvar.setOnClickListener(v -> {
            String nome = etNome.getText().toString().trim();
            String bio = etBio.getText().toString().trim();

            if (nome.length() < MIN_NOME_LEN) {
                Toast.makeText(ProfileActivity.this, getString(R.string.perfil_nome_curto), Toast.LENGTH_SHORT).show();
                return;
            }

            salvarPerfilLocal(nome, bio, avatarUri);

            btnSalvar.setEnabled(false);
            profileRepository.saveProfile(userId, nome, bio, avatarUri, accessToken, new ProfileRepository.SaveProfileCallback() {
                @Override
                public void onSuccess() {
                    btnSalvar.setEnabled(true);
                    continuarPosSalvar();
                }

                @Override
                public void onError(String message) {
                    btnSalvar.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarEstatisticas();
    }

    @Override
    public void onBackPressed() {
        if (forceCompleteProfile) {
            Toast.makeText(this, getString(R.string.perfil_complete), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    private void carregarPerfilLocal() {
        etNome.setText(prefs.getString("perfil_nome", ""));
        etBio.setText(prefs.getString("perfil_bio", ""));
        avatarUri = prefs.getString("perfil_avatar", null);
        carregarAvatar(avatarUri);
    }

    private void carregarPerfilRemoto() {
        profileRepository.loadProfile(userId, accessToken, new ProfileRepository.LoadProfileCallback() {
            @Override
            public void onSuccess(ProfileDto profile) {
                if (profile.displayName != null && !profile.displayName.trim().isEmpty()) {
                    etNome.setText(profile.displayName);
                }
                if (profile.bio != null) {
                    etBio.setText(profile.bio);
                }
                if (profile.avatarUrl != null && !profile.avatarUrl.trim().isEmpty()) {
                    avatarUri = profile.avatarUrl.trim();
                    carregarAvatar(avatarUri);
                }
                salvarPerfilLocal(etNome.getText().toString().trim(), etBio.getText().toString().trim(), avatarUri);
            }

            @Override
            public void onEmpty() {
                // Perfil ainda nao existe no backend; manter estado local.
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void salvarPerfilLocal(String nome, String bio, String avatar) {
        prefs.edit()
                .putString("perfil_nome", nome)
                .putString("perfil_bio", bio)
                .putString("perfil_avatar", avatar)
                .apply();
    }

    private void carregarAvatar(String avatarPath) {
        if (avatarPath == null || avatarPath.trim().isEmpty()) {
            ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            return;
        }
        Glide.with(this)
                .load(avatarPath)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivAvatar);
    }

    private void continuarPosSalvar() {
        if (forceCompleteProfile) {
            Intent intent = new Intent(ProfileActivity.this, CartasActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        finish();
    }

    private void atualizarEstatisticas() {

        tvFavoritas.setText(R.string.perfil_favoritas_carregando);
        favoriteRepository.loadFavorites(UserSession.getUserId(this), UserSession.getAccessToken(this), new FavoriteRepository.LoadFavoritesCallback() {
            @Override
            public void onSuccess(Set<String> favoriteIds) {
                tvFavoritas.setText(getString(R.string.perfil_favoritas_formato, favoriteIds.size()));
            }

            @Override
            public void onError(String message) {
                tvFavoritas.setText(R.string.perfil_favoritas_indisponivel);
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
