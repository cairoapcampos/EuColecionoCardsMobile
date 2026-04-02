package com.eucolecionocards;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.eucolecionocards.data.remote.AuthSessionDto;
import com.eucolecionocards.data.repository.AuthRepository;
import com.eucolecionocards.security.RootDetector;
import com.eucolecionocards.security.TamperDetector;
import com.eucolecionocards.session.UserSession;

public class LoginActivity extends Activity {
    private final AuthRepository authRepository = new AuthRepository();
    private boolean modoCadastro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Deteccao de ambiente comprometido (root/jailbreak)
        if (RootDetector.isDeviceRooted()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.login_root_titulo))
                    .setMessage(getString(R.string.login_root_mensagem))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.login_fechar), (dialog, which) -> finish())
                    .show();
            return;
        }

        // Proteção contra adulteração (anti-tampering)
        if (TamperDetector.isAppTampered(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.login_tamper_titulo))
                    .setMessage(getString(R.string.login_tamper_mensagem))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.login_fechar), (dialog, which) -> finish())
                    .show();
            return;
        }

        if (UserSession.isLoggedIn(this)) {
            abrirFluxoPosLogin();
            return;
        }

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvToggleAuthMode = findViewById(R.id.tvToggleAuthMode);
        EditText etUser = findViewById(R.id.etUsername);
        EditText etPass = findViewById(R.id.etPassword);

        atualizarModoAuth(btnLogin, tvToggleAuthMode);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etUser.getText().toString().trim();
                String senha = etPass.getText().toString();

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_email_invalido), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (senha.length() < 6) {
                    Toast.makeText(
                            LoginActivity.this,
                            getString(R.string.login_senha_curta),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                btnLogin.setEnabled(false);
                if (modoCadastro) {
                    UserSession.clearProfileCache(LoginActivity.this);
                    authRepository.signUp(email, senha, new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(AuthSessionDto session) {
                            btnLogin.setEnabled(true);
                            if (session.accessToken != null && !session.accessToken.trim().isEmpty()) {
                                UserSession.saveAuthSession(
                                        LoginActivity.this,
                                        session.user.id,
                                        session.accessToken,
                                        session.refreshToken
                                );
                                abrirPerfilObrigatorio();
                                return;
                            }
                            modoCadastro = false;
                            atualizarModoAuth(btnLogin, tvToggleAuthMode);
                            Toast.makeText(LoginActivity.this, getString(R.string.login_cadastro_sucesso), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    authRepository.signIn(email, senha, new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(AuthSessionDto session) {
                            UserSession.saveAuthSession(
                                    LoginActivity.this,
                                    session.user.id,
                                    session.accessToken,
                                    session.refreshToken
                            );
                            btnLogin.setEnabled(true);
                            abrirFluxoPosLogin();
                        }

                        @Override
                        public void onError(String message) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        tvToggleAuthMode.setOnClickListener(v -> {
            modoCadastro = !modoCadastro;
            atualizarModoAuth(btnLogin, tvToggleAuthMode);
        });
    }

    private void abrirCartas() {
        startActivity(new Intent(LoginActivity.this, CartasActivity.class));
        finish();
    }

    private void abrirFluxoPosLogin() {
        if (UserSession.hasCompleteProfile(this)) {
            abrirCartas();
            return;
        }
        abrirPerfilObrigatorio();
    }

    private void abrirPerfilObrigatorio() {
        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
        intent.putExtra("force_complete_profile", true);
        startActivity(intent);
        finish();
    }

    private void atualizarModoAuth(Button btnLogin, TextView tvToggleAuthMode) {
        if (modoCadastro) {
            btnLogin.setText(R.string.login_btn_cadastrar);
            tvToggleAuthMode.setText(R.string.login_toggle_entrar);
        } else {
            btnLogin.setText(R.string.login_btn_entrar);
            tvToggleAuthMode.setText(R.string.login_toggle_cadastrar);
        }
    }
}
