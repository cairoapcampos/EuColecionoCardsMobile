package com.eucolecionocards.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import java.security.MessageDigest;

/**
 * Proteção contra adulteração (anti-tampering).
 *
 * Verifica se a assinatura do APK em execução corresponde à assinatura
 * original do desenvolvedor. Se alguém descompilar o APK, modificar
 * o código (ex.: remover detecção de root) e reempacotar, a assinatura
 * será diferente e o app se recusará a executar.
 */
public final class TamperDetector {

    private static final String TAG = "TamperDetector";

    /**
     * Hash SHA-256 esperado do certificado de assinatura do desenvolvedor.
     *
     * Para obter o hash da sua keystore:
     *   keytool -list -v -keystore sua-keystore.jks -alias seuAlias
     *
     * Para debug:
     *   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
     *
     * Copie o valor SHA256 e cole abaixo (em maiúsculas, sem os dois-pontos).
     */
    private static final String EXPECTED_SIGNATURE_SHA256 =
            "4EBD88887DFD754CB559B3BD621F60F3B2F084F2C6833B14C5DF71FC241C5A0D";

    private TamperDetector() {}

    /**
     * Retorna true se o APK foi adulterado (assinatura não corresponde à esperada).
     */
    public static boolean isAppTampered(Context context) {
        try {
            String currentHash = getSignatureSHA256(context);
            if (currentHash == null) {
                Log.w(TAG, "Não foi possível obter a assinatura do APK");
                return true; // Na dúvida, considera adulterado
            }
            boolean tampered = !EXPECTED_SIGNATURE_SHA256.equalsIgnoreCase(currentHash);
            if (tampered) {
                Log.w(TAG, "Assinatura do APK não corresponde à esperada."
                        + " Esperado: " + EXPECTED_SIGNATURE_SHA256
                        + " Encontrado: " + currentHash);
            }
            return tampered;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao verificar assinatura do APK", e);
            return true; // Na dúvida, considera adulterado
        }
    }

    /**
     * Extrai o hash SHA-256 do certificado de assinatura do APK em execução.
     */
    @SuppressWarnings("deprecation")
    private static String getSignatureSHA256(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);

            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                return null;
            }

            Signature signature = packageInfo.signatures[0];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signature.toByteArray());
            return bytesToHex(hash);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao extrair assinatura", e);
            return null;
        }
    }

    /**
     * Converte array de bytes em string hexadecimal (maiúsculas, sem separadores).
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}

