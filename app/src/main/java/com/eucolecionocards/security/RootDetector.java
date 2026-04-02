package com.eucolecionocards.security;

import android.os.Build;
import java.io.File;

/**
 * Detecta se o dispositivo esta com root/jailbreak ativo.
 * Verifica binarios su, paths comuns de root e propriedades do sistema.
 */
public final class RootDetector {

    private RootDetector() {}

    /** Caminhos conhecidos onde o binario su costuma ser instalado. */
    private static final String[] SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU/SuperSU.apk"
    };

    /** Pacotes comuns de apps de gerenciamento de root. */
    private static final String[] ROOT_PACKAGES = {
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "com.yellowes.su"
    };

    /**
     * Retorna true se houver indicios de root no dispositivo.
     */
    public static boolean isDeviceRooted() {
        return checkSuBinary() || checkRootManagementApps() || checkBuildTags() || checkSuCommand();
    }

    /** Verifica se existe o binario su em caminhos conhecidos. */
    private static boolean checkSuBinary() {
        for (String path : SU_PATHS) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    /** Verifica se apps de root management estao instalados. */
    private static boolean checkRootManagementApps() {
        for (String pkg : ROOT_PACKAGES) {
            File dir = new File("/data/data/" + pkg);
            if (dir.exists()) {
                return true;
            }
        }
        return false;
    }

    /** Build tags com "test-keys" indicam ROM customizada. */
    private static boolean checkBuildTags() {
        String tags = Build.TAGS;
        return tags != null && tags.contains("test-keys");
    }

    /** Tenta executar o comando which su. */
    private static boolean checkSuCommand() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

