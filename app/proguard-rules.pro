# ============================================================================
# ProGuard / R8 — Regras de ofuscacao do EuColecionoCards Mobile
# ============================================================================
# O R8 (substituto moderno do ProGuard) e ativado com minifyEnabled true no
# build.gradle. Ele realiza 3 acoes:
#   1. Shrinking  — remove classes, metodos e campos nao utilizados
#   2. Optimization — otimiza bytecode (inlining, merging, etc.)
#   3. Obfuscation — renomeia classes, metodos e campos para nomes curtos
#
# As regras abaixo protegem classes que NAO podem ser renomeadas ou removidas,
# pois sao acessadas por reflexao (Gson, Retrofit, Glide) ou pelo Android
# Framework (Activities declaradas no AndroidManifest).
# ============================================================================

# ---------- DTOs do Gson (desserializacao JSON por reflexao) ----------------
# O Gson usa reflexao para mapear campos JSON para campos Java.
# Se o R8 renomear "accessToken" para "a", o mapeamento com
# @SerializedName("access_token") pode falhar em versoes antigas do Gson.
# Manter todos os campos publicos dos DTOs garante compatibilidade.
-keep class com.eucolecionocards.data.remote.** { *; }

# ---------- Retrofit (interfaces de servico acessadas via proxy) ------------
# Retrofit cria implementacoes dinamicas das interfaces via Proxy.newProxyInstance.
# Anotacoes como @GET, @POST, @Header e @Query sao lidas por reflexao.
-keep interface com.eucolecionocards.data.api.SupabaseService { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ---------- Modelos internos (usados em Adapters e listas) -----------------
-keep class com.eucolecionocards.model.** { *; }

# ---------- OkHttp / Retrofit (regras padrao) ------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---------- Gson (regras padrao) -------------------------------------------
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ---------- Glide (carregamento de imagens) --------------------------------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ---------- AndroidX Security (EncryptedSharedPreferences) -----------------
-keep class androidx.security.crypto.** { *; }

# ---------- Manter nomes de excecoes para stack traces legiveis ------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

