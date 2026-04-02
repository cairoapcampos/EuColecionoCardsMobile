# Segurança — EuColecionoCards Mobile

Este documento descreve as técnicas de segurança implementadas no app Android.

> Voltar para o [README principal](../README.md)

---

## Sumário

1. [Segurança de senha](#1-segurança-de-senha)
2. [Ofuscação de código (R8/ProGuard)](#2-ofuscação-de-código-r8proguard)
3. [Detecção de ambiente comprometido (root/jailbreak)](#3-detecção-de-ambiente-comprometido-rootjailbreak)
4. [Armazenamento criptografado (EncryptedSharedPreferences)](#4-armazenamento-criptografado-encryptedsharedpreferences)
5. [Proteção contra adulteração (anti-tampering)](#5-proteção-contra-adulteração-anti-tampering)
6. [Certificate pinning (SSL pinning)](#6-certificate-pinning-ssl-pinning)

---

## 1. Segurança de senha

- A validação atual no login exige senha com no mínimo 10 caracteres.
- Para maior segurança, recomenda-se reforçar a política no cliente e no Supabase Auth.

---

## 2. Ofuscação de código (R8/ProGuard)

**Arquivo de regras:** `app/proguard-rules.pro`

### O que é ofuscação e por que é importante?

Quando um APK Android é gerado, o bytecode Java pode ser facilmente **descompilado** de volta
para código legível usando ferramentas como `jadx`, `apktool` ou `dex2jar`. Sem ofuscação, um
atacante pode:

- Ler a lógica de negócio completa do app
- Encontrar URLs de API, endpoints e padrões de autenticação
- Identificar verificações de segurança (ex.: detecção de root) e removê-las
- Copiar ou modificar o app (engenharia reversa)

A ofuscação transforma o código compilado, renomeando classes, métodos e campos para nomes
curtos e sem significado, dificultando significativamente a engenharia reversa.

### O que o R8 faz (3 etapas)

| Etapa | O que faz | Exemplo |
|---|---|---|
| **Shrinking** (encolhimento) | Remove classes, métodos e campos não utilizados pelo app | Uma classe utilitária que nunca é chamada é completamente removida do APK |
| **Optimization** (otimização) | Otimiza o bytecode (inlining de métodos, merge de classes, simplificação de fluxo) | Um método chamado apenas uma vez é inserido diretamente no chamador |
| **Obfuscation** (ofuscação) | Renomeia identificadores para nomes curtos sem significado | `RootDetector.isDeviceRooted()` → `a.b()` |

### Antes vs. Depois da ofuscação

**Antes (código descompilado legível):**
```java
public class RootDetector {
    public static boolean isDeviceRooted() {
        return checkSuBinary() || checkRootManagementApps();
    }
    private static boolean checkSuBinary() { ... }
}

public class UserSession {
    public static void saveAuthSession(Context context, String userId, String accessToken) { ... }
    public static String getAccessToken(Context context) { ... }
}
```

**Depois (código ofuscado — resultado aproximado):**
```java
public class a {
    public static boolean a() {
        return b() || c();
    }
    private static boolean b() { ... }
}

public class b {
    public static void a(Context a, String b, String c) { ... }
    public static String b(Context a) { ... }
}
```

> Um atacante que descompile o APK verá apenas `a.b()`, `c.a()`, etc. — sem contexto
> sobre o que cada classe ou método faz.

### Configuração no build.gradle

```groovy
buildTypes {
    release {
        minifyEnabled true       // Ativa R8: shrinking + ofuscação
        shrinkResources true     // Remove recursos (imagens, layouts) não utilizados
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

> **Nota:** a ofuscação só é aplicada no build **release**. O build **debug** continua
> sem ofuscação para facilitar o desenvolvimento e depuração.

### Regras de preservação (proguard-rules.pro)

Algumas classes **não podem** ser ofuscadas porque são acessadas por **reflexão** em tempo de
execução. Se o R8 renomear essas classes, o app quebra. As regras `-keep` protegem essas classes:

| Regra | Classes protegidas | Motivo |
|---|---|---|
| `-keep class ...data.remote.** { *; }` | `CardDto`, `AuthSessionDto`, `AuthUserDto`, `ProfileDto`, `FavoriteDto`, `AuthSignInRequest`, `FavoriteUpsertRequest`, `ProfileUpsertRequest` | O **Gson** usa reflexão para mapear campos JSON para campos Java. Se `accessToken` for renomeado para `a`, o mapeamento com `@SerializedName("access_token")` pode falhar |
| `-keep interface ...SupabaseService { *; }` | `SupabaseService` | O **Retrofit** cria implementações dinâmicas via `Proxy.newProxyInstance()`. As anotações `@GET`, `@POST`, `@Header` são lidas por reflexão |
| `-keep class ...model.** { *; }` | `Carta`, `CarrinhoItem` | Modelos usados em Adapters e passados entre Activities |
| `-keep class retrofit2.** { *; }` | Biblioteca Retrofit | Internals do Retrofit acessados por reflexão |
| `-keep class com.google.gson.** { *; }` | Biblioteca Gson | Motor de desserialização JSON |
| `-keep ...GlideModule` | Módulos do Glide | Glide descobre módulos por reflexão |
| `-keep class androidx.security.crypto.** { *; }` | EncryptedSharedPreferences | Acessa Android Keystore por reflexão |
| `-keepattributes SourceFile,LineNumberTable` | Stack traces | Mantém números de linha para depuração de crashes em produção |

### Classes ofuscadas vs. preservadas

```
com.eucolecionocards/
├── security/
│   ├── RootDetector.java        → OFUSCADA (ex.: a.class)
│   └── SecurePrefs.java         → OFUSCADA (ex.: b.class)
├── session/
│   └── UserSession.java         → OFUSCADA (ex.: c.class)
├── adapter/
│   ├── CartasAdapter.java       → OFUSCADA
│   ├── CartasRecyclerAdapter.java → OFUSCADA
│   └── CarrinhoAdapter.java     → OFUSCADA
├── data/
│   ├── remote/
│   │   ├── CardDto.java         → PRESERVADA (Gson reflexão)
│   │   ├── AuthSessionDto.java  → PRESERVADA (Gson reflexão)
│   │   ├── AuthSignInRequest.java → PRESERVADA (Gson reflexão)
│   │   └── ...                  → PRESERVADA
│   ├── api/
│   │   ├── SupabaseService.java → PRESERVADA (Retrofit proxy)
│   │   ├── ApiClient.java       → OFUSCADA
│   │   └── SupabaseConfig.java  → OFUSCADA
│   └── repository/
│       ├── AuthRepository.java  → OFUSCADA
│       ├── CartaRepository.java → OFUSCADA
│       └── ...                  → OFUSCADA
├── model/
│   ├── Carta.java               → PRESERVADA (Adapter/Intent)
│   └── CarrinhoItem.java        → PRESERVADA (Adapter/Intent)
├── LoginActivity.java           → PRESERVADA (AndroidManifest)
├── CartasActivity.java          → PRESERVADA (AndroidManifest)
├── CarrinhoActivity.java        → PRESERVADA (AndroidManifest)
├── ProfileActivity.java         → PRESERVADA (AndroidManifest)
└── PagamentoActivity.java       → PRESERVADA (AndroidManifest)
```

> Activities declaradas no `AndroidManifest.xml` são automaticamente preservadas pelo R8
> (não precisam de regra `-keep` explícita).

### Como verificar a ofuscação

Após gerar o APK release:
```bash
./gradlew assembleRelease
```

O R8 gera um arquivo de mapeamento em:
```
app/build/outputs/mapping/release/mapping.txt
```

Este arquivo mapeia os nomes ofuscados de volta para os originais. Exemplo:
```
com.eucolecionocards.security.RootDetector -> a:
    boolean isDeviceRooted() -> a
    boolean checkSuBinary() -> b
    boolean checkRootManagementApps() -> c
```

> **Importante:** guarde o `mapping.txt` de cada release. Ele é necessário para traduzir
> stack traces de crashes em produção de volta para nomes legíveis.

---

## 3. Detecção de ambiente comprometido (root/jailbreak)

**Classe:** `com.eucolecionocards.security.RootDetector`

### O que é root e por que é um risco

Root (Android) ou jailbreak (iOS) é o processo de obter privilégios de superusuário no
dispositivo. Isso permite acesso irrestrito a **todos** os arquivos do sistema, incluindo
dados internos de qualquer app instalado.

Em um dispositivo com root, um atacante pode:
- Ler tokens de autenticação e sequestrar a sessão do usuário
- Acessar bancos de dados locais e SharedPreferences (mesmo criptografados, a chave pode ser extraída)
- Interceptar tráfego de rede com certificados falsos (man-in-the-middle)
- Injetar código em tempo de execução (hooking via Xposed/Frida)
- Modificar o APK e reinstalar sem verificação de assinatura

Por isso, apps que lidam com dados sensíveis (financeiros, pessoais, autenticação) devem
**detectar e bloquear** a execução em ambientes comprometidos.

### Como funciona no app

A `LoginActivity` é o ponto de entrada do app. **Antes** de verificar sessão, exibir campos
de login ou qualquer outra lógica, ela invoca `RootDetector.isDeviceRooted()`:

```
Usuário abre o app
  └─> LoginActivity.onCreate()
        └─> RootDetector.isDeviceRooted()
              ├─> checkSuBinary()          // Verifica binário su no sistema de arquivos
              ├─> checkRootManagementApps() // Verifica apps de root instalados
              ├─> checkBuildTags()          // Verifica build tags do sistema
              └─> checkSuCommand()          // Tenta executar "which su" no shell
              │
              ├── Alguma retornou true?
              │     └─> SIM: Exibe diálogo bloqueante → fecha o app
              └── Todas retornaram false?
                    └─> NÃO: Continua fluxo normal (login/sessão)
```

### As 4 verificações detalhadas

| # | Método | O que verifica | Como funciona | Por que é importante |
|---|---|---|---|---|
| 1 | `checkSuBinary()` | Presença do executável `su` | Percorre 13 caminhos conhecidos no sistema de arquivos (`/system/bin/su`, `/sbin/su`, `/su/bin/su`, etc.) e verifica se o arquivo existe com `new File(path).exists()` | O binário `su` é o principal mecanismo de elevação de privilégios; sua presença é o indicador mais direto de root |
| 2 | `checkRootManagementApps()` | Apps de gerenciamento de root instalados | Verifica se o diretório de dados de 6 pacotes conhecidos existe em `/data/data/` (Magisk, SuperSU, KoushikDutta Superuser, etc.) | Mesmo que o binário `su` esteja oculto, a presença desses apps indica que o dispositivo foi rooteado |
| 3 | `checkBuildTags()` | Tags de compilação do sistema | Lê `android.os.Build.TAGS` e verifica se contém a string `test-keys` | ROMs oficiais usam `release-keys`; a presença de `test-keys` indica uma ROM customizada ou build de desenvolvimento, comum em dispositivos rooteados |
| 4 | `checkSuCommand()` | Execução real do `su` | Executa `Runtime.getRuntime().exec({"which", "su"})` e verifica o exit code; retorno `0` significa que o sistema encontrou o `su` no PATH | Cobre casos onde o binário existe em um caminho não listado explicitamente; é a verificação mais abrangente porém mais lenta |

### Caminhos verificados para o binário `su`

```
/system/bin/su
/system/xbin/su
/sbin/su
/data/local/xbin/su
/data/local/bin/su
/system/sd/xbin/su
/system/bin/failsafe/su
/data/local/su
/su/bin/su
/su/bin
/system/app/Superuser.apk
/system/app/SuperSU.apk
/system/app/SuperSU/SuperSU.apk
```

### Pacotes de root verificados

| Pacote | App |
|---|---|
| `com.topjohnwu.magisk` | Magisk Manager |
| `eu.chainfire.supersu` | SuperSU |
| `com.koushikdutta.superuser` | Superuser (ClockworkMod) |
| `com.noshufou.android.su` | Superuser (ChainsDD) |
| `com.thirdparty.superuser` | Superuser (genérico) |
| `com.yellowes.su` | Root checker alternativo |

### O que acontece quando root é detectado

O app exibe um **diálogo modal bloqueante** que:
- **Não pode ser cancelado** (`setCancelable(false)`) — tocar fora ou pressionar "voltar" não fecha
- **Tem apenas o botão "Fechar"** que encerra a Activity
- **Impede qualquer interação** com o app — nenhum dado é carregado, nenhuma requisição é feita

```java
// LoginActivity.onCreate — ANTES de qualquer outra lógica
if (RootDetector.isDeviceRooted()) {
    new AlertDialog.Builder(this)
            .setTitle("Dispositivo comprometido")
            .setMessage("Foi detectado que este dispositivo possui acesso root. "
                    + "Por segurança, o app não pode ser utilizado em "
                    + "dispositivos comprometidos.")
            .setCancelable(false)
            .setPositiveButton("Fechar", (dialog, which) -> finish())
            .show();
    return; // Não executa mais nada
}
```

### Fluxo na LoginActivity

```
onCreate()
  │
  ├─ 1. setContentView (carrega layout)
  │
  ├─ 2. RootDetector.isDeviceRooted()?
  │     ├── SIM → mostra diálogo → finish() → FIM
  │     └── NÃO → continua ↓
  │
  ├─ 3. UserSession.isLoggedIn()?
  │     ├── SIM → abrirFluxoPosLogin() → FIM
  │     └── NÃO → continua ↓
  │
  └─ 4. Exibe campos de login/cadastro
```

### Limitações conhecidas

| Limitação | Descrição |
|---|---|
| **Root oculto (MagiskHide/Zygisk)** | Ferramentas como Magisk podem ocultar o root de apps específicos, burlando as verificações baseadas em arquivos |
| **Verificação apenas no início** | A detecção ocorre somente ao abrir o app; se o root for ativado após o início, não será detectado na sessão atual |
| **Sem verificação de integridade do APK** | Não valida se o próprio APK foi modificado (anti-tampering separado seria necessário) |

> Para ambientes de produção com requisitos mais rígidos, recomenda-se complementar com
> bibliotecas especializadas como [SafetyNet/Play Integrity API](https://developer.android.com/training/safetynet)
> do Google, que validam a integridade do dispositivo diretamente com os servidores do Google.

---

## 4. Armazenamento criptografado (EncryptedSharedPreferences)

**Classe:** `com.eucolecionocards.security.SecurePrefs`

**Dependência adicionada:**
```groovy
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

### O problema resolvido

O `SharedPreferences` padrão do Android salva dados em um arquivo XML em **texto puro** dentro do
dispositivo (em `/data/data/com.eucolecionocards/shared_prefs/`). Em um aparelho com root, qualquer
app ou usuário pode abrir esse arquivo e ler diretamente tokens de autenticação, IDs e dados pessoais.

Com o `EncryptedSharedPreferences`, o conteúdo do arquivo fica **cifrado em disco** — mesmo que
alguém acesse o arquivo, verá apenas dados ilegíveis.

### Antes vs. Depois (conteúdo do arquivo XML em disco)

| | Antes (SharedPreferences) | Agora (EncryptedSharedPreferences) |
|---|---|---|
| **Arquivo** | `com.eucolecionocards_preferences.xml` | `eucolecionocards_secure_prefs.xml` |
| **Nome da chave** | `access_token` | `AX2fE8kR9m...` (hash cifrado) |
| **Valor armazenado** | `eyJhbGciOiJIUzI1NiIs...` (token JWT legível) | `AUkR7mN3vBx...` (blob cifrado ilegível) |

### Algoritmos de criptografia utilizados

| Componente | Algoritmo | Finalidade |
|---|---|---|
| **Chave mestra** | AES-256 GCM via Android Keystore (`MasterKeys.AES256_GCM_SPEC`) | Protege as chaves de criptografia; armazenada em hardware seguro (TEE/StrongBox) |
| **Nomes das chaves** | AES-256 SIV (deterministic AEAD) | Cifra os nomes das preferências (ex.: `access_token` vira um hash) |
| **Valores** | AES-256 GCM (AEAD) | Cifra os valores das preferências (ex.: o token JWT) |

### Fluxo detalhado

**Escrita (salvar dados):**
```
UserSession.saveAuthSession(context, userId, accessToken, refreshToken)
  └─> getPrefs(context)
        └─> SecurePrefs.get(context)
              ├─> MasterKeys.getOrCreate(AES256_GCM_SPEC)
              │     └─> Android Keystore gera ou recupera chave AES-256
              │         (a chave NUNCA sai do hardware seguro)
              └─> EncryptedSharedPreferences.create(...)
                    └─> Retorna instância que cifra automaticamente
  └─> prefs.edit()
        .putString("access_token", "eyJhbGciOi...")  // texto puro na memória
        .apply()
              └─> EncryptedSharedPreferences internamente:
                    1. Cifra o nome "access_token" com AES-256-SIV → "AX2fE8kR9m..."
                    2. Cifra o valor "eyJhbGciOi..." com AES-256-GCM → "AUkR7mN3vBx..."
                    3. Salva em disco o par cifrado
```

**Leitura (recuperar dados):**
```
UserSession.getAccessToken(context)
  └─> getPrefs(context)
        └─> SecurePrefs.get(context)  // mesma instância criptografada
  └─> prefs.getString("access_token", "")
              └─> EncryptedSharedPreferences internamente:
                    1. Cifra o nome "access_token" com AES-256-SIV para localizar a entrada
                    2. Descriptografa o valor com AES-256-GCM usando a chave mestra
                    3. Retorna "eyJhbGciOi..." em texto puro (somente na memória)
```

> O código de leitura/escrita continua **idêntico** ao `SharedPreferences` normal
> (`putString`, `getString`, `edit().apply()`). A criptografia é **transparente** para o desenvolvedor.

### Dados protegidos

| Chave | Conteúdo | Onde é usado |
|---|---|---|
| `access_token` | Token JWT de autenticação do Supabase | `UserSession` |
| `refresh_token` | Token para renovar a sessão | `UserSession` |
| `local_user_id` | UUID do usuário autenticado | `UserSession` |
| `perfil_nome` | Nome de exibição (cache local) | `UserSession` / `ProfileActivity` |
| `perfil_bio` | Biografia do usuário (cache local) | `UserSession` / `ProfileActivity` |
| `perfil_avatar` | URI do avatar (cache local) | `UserSession` / `ProfileActivity` |

### Fallback de segurança

Se o dispositivo não suportar o Android Keystore (raro em API 23+), o `SecurePrefs` captura a exceção,
registra um log de aviso e retorna um `SharedPreferences` normal para evitar crash do app:

```java
} catch (Exception e) {
    Log.e(TAG, "Falha ao criar EncryptedSharedPreferences, usando fallback", e);
    return context.getApplicationContext()
            .getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
}
```

### Classes envolvidas

| Classe | Papel |
|---|---|
| `SecurePrefs` (`security/`) | Cria e retorna a instância de `EncryptedSharedPreferences` |
| `UserSession` (`session/`) | Usa `SecurePrefs.get()` para ler e gravar tokens e sessão |
| `ProfileActivity` | Usa `SecurePrefs.get()` para cache local de perfil |

> **Nota:** o `minSdk` foi elevado de 21 para 23 pois `EncryptedSharedPreferences` requer API 23+.

---

## 5. Proteção contra adulteração (anti-tampering)

**Classe:** `com.eucolecionocards.security.TamperDetector`

### O que é adulteração de APK e por que é um risco?

Adulteração (tampering) é o processo de **descompilar** um APK, **modificar** seu código ou
recursos e **reempacotar** com uma nova assinatura. Isso permite que um atacante:

- Remova verificações de segurança (ex.: detecção de root, validação de senha)
- Injete código malicioso (ex.: enviar credenciais para um servidor externo)
- Altere a URL do backend para redirecionar requisições a um servidor falso
- Distribua versões pirateadas ou adulteradas do app

A ofuscação (técnica nº 2) **dificulta** a engenharia reversa, mas **não impede** a modificação.
O anti-tampering **detecta** que o APK foi modificado e bloqueia a execução.

### Como funciona

Cada APK é assinado digitalmente com um certificado do desenvolvedor. Quando alguém modifica
e reempacota o APK, precisa assiná-lo com um **certificado diferente** (pois não possui a
keystore original). O `TamperDetector` compara o hash SHA-256 do certificado em execução
com o hash esperado embutido no código:

```
App inicia → LoginActivity.onCreate()
  │
  ├─ 1. RootDetector.isDeviceRooted()? (verificação de root)
  │
  ├─ 2. TamperDetector.isAppTampered(context)?
  │     │
  │     ├─ PackageManager.getPackageInfo(GET_SIGNATURES)
  │     │     └─ Extrai o certificado de assinatura do APK em execução
  │     │
  │     ├─ MessageDigest.getInstance("SHA-256")
  │     │     └─ Calcula o hash SHA-256 do certificado
  │     │
  │     └─ Compara com EXPECTED_SIGNATURE_SHA256
  │           ├── IGUAL → App original → continua normalmente
  │           └── DIFERENTE → App adulterado → bloqueia execução
  │
  └─ 3. Fluxo normal (login/sessão)
```

### As 3 verificações internas

| # | Etapa | O que faz | Resultado se falhar |
|---|---|---|---|
| 1 | Extrair assinatura | Obtém o certificado de assinatura do APK via `PackageManager.getPackageInfo()` | Considera adulterado (retorna `true`) |
| 2 | Calcular hash | Gera o SHA-256 do certificado com `MessageDigest` | Considera adulterado (retorna `true`) |
| 3 | Comparar hash | Compara o hash calculado com o `EXPECTED_SIGNATURE_SHA256` embutido no código | Se diferente, o APK foi reassinado com outro certificado |

### Cenário de ataque neutralizado

```
Atacante descompila o APK com apktool/jadx
  └─> Remove o código de RootDetector (para rodar em dispositivo rooteado)
        └─> Reempacota o APK com: apktool b app/ -o app-modified.apk
              └─> Assina com certificado próprio: apksigner sign --ks hacker.jks app-modified.apk
                    └─> Instala no dispositivo
                          └─> App inicia → TamperDetector compara assinaturas
                                └─> Hash do certificado do hacker ≠ hash esperado
                                      └─> "App adulterado" → app fecha ✓
```

### O que acontece quando adulteração é detectada

O app exibe um **diálogo modal bloqueante** (igual ao de root):
- **Não pode ser cancelado** — `setCancelable(false)`
- **Única opção:** botão "Fechar" que encerra o app
- **Nenhum dado é carregado**, nenhuma requisição é feita ao Supabase

```java
// LoginActivity.onCreate — logo após a verificação de root
if (TamperDetector.isAppTampered(this)) {
    new AlertDialog.Builder(this)
            .setTitle("App adulterado")
            .setMessage("Foi detectado que este app foi modificado. "
                    + "Por segurança, não é possível continuar. "
                    + "Instale a versão oficial.")
            .setCancelable(false)
            .setPositiveButton("Fechar", (dialog, which) -> finish())
            .show();
    return;
}
```

### Fluxo completo de segurança na LoginActivity

```
onCreate()
  │
  ├─ 1. setContentView (carrega layout)
  │
  ├─ 2. RootDetector.isDeviceRooted()?
  │     ├── SIM → diálogo "Dispositivo comprometido" → finish()
  │     └── NÃO → continua ↓
  │
  ├─ 3. TamperDetector.isAppTampered()?
  │     ├── SIM → diálogo "App adulterado" → finish()
  │     └── NÃO → continua ↓
  │
  ├─ 4. UserSession.isLoggedIn()?
  │     ├── SIM → abrirFluxoPosLogin()
  │     └── NÃO → continua ↓
  │
  └─ 5. Exibe campos de login/cadastro
```

### Como obter o hash do certificado de assinatura

**Para a keystore de debug:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

**Para a keystore de release (produção):**
```bash
keytool -list -v -keystore sua-keystore.jks -alias seuAlias
```

Copie o valor **SHA256** exibido (ex.: `4E:BD:88:88:7D:FD:...`), remova os dois-pontos
e cole na constante `EXPECTED_SIGNATURE_SHA256` do `TamperDetector.java`:

```java
private static final String EXPECTED_SIGNATURE_SHA256 =
        "4EBD88887DFD754CB559B3BD621F60F3B2F084F2C6833B14C5DF71FC241C5A0D";
```

> **Importante:** ao gerar a versão de release com uma keystore diferente, atualize este hash.
> Caso contrário, o próprio app oficial será bloqueado como "adulterado".

### Relação com as outras técnicas de segurança

| Técnica | Sem anti-tampering | Com anti-tampering |
|---|---|---|
| **Ofuscação (R8)** | Atacante pode modificar código ofuscado e reempacotar | Modificação detectada pela assinatura diferente |
| **Detecção de root** | Atacante remove o `RootDetector` e reempacota | Remoção detectada pela assinatura diferente |
| **EncryptedSharedPreferences** | Atacante pode alterar a lógica de criptografia | Alteração detectada pela assinatura diferente |

> O anti-tampering funciona como uma **camada de proteção transversal** que garante a integridade
> de todas as outras técnicas implementadas.

### Limitações conhecidas

| Limitação | Descrição |
|---|---|
| **Hash embutido no código** | O hash esperado está no próprio código-fonte; um atacante sofisticado pode localizá-lo e substituí-lo pelo hash do próprio certificado (mitigado pela ofuscação) |
| **API `GET_SIGNATURES` depreciada** | A API usada (`PackageInfo.signatures`) é depreciada a partir da API 28; continua funcionando, mas versões futuras do Android podem exigir `GET_SIGNING_CERTIFICATES` |
| **Não protege contra hooking** | Ferramentas como Frida/Xposed podem interceptar o retorno de `isAppTampered()` em tempo de execução, sem modificar o APK |

> Para máxima proteção, recomenda-se combinar anti-tampering com [Play Integrity API](https://developer.android.com/google/play/integrity)
> do Google, que valida a integridade do app e do dispositivo via servidores externos.

---

## 6. Certificate pinning (SSL pinning)

**Classe:** `com.eucolecionocards.data.api.ApiClient`

### O que é SSL pinning e por que é importante

Por padrão, o Android confia em qualquer certificado emitido por uma **Autoridade Certificadora (CA)**
presente no repositório de confiança do sistema. Isso significa que:

- Se um atacante instalar um **certificado CA falso** no dispositivo (via perfil de configuração,
  MDM ou acesso root), ele pode interceptar **todo o tráfego HTTPS** — incluindo credenciais e tokens.
- Ferramentas como **mitmproxy**, **Charles Proxy** e **Burp Suite** exploram exatamente essa
  vulnerabilidade para capturar requisições em texto puro.

O SSL pinning resolve isso fixando ("pinando") os hashes das chaves públicas dos certificados
esperados diretamente no código do app. Mesmo que o sistema confie em um certificado falso,
o OkHttp rejeita a conexão porque o hash não corresponde.

### Cenário de ataque: man-in-the-middle (MITM)

**Sem SSL pinning:**
```
App ──HTTPS──> Proxy do atacante ──HTTPS──> supabase.co
                    │
                    └─ Certificado falso emitido por CA instalada no dispositivo
                       → Android confia → conexão aceita
                       → Atacante lê: email, senha, access_token, dados do usuário
```

**Com SSL pinning:**
```
App ──HTTPS──> Proxy do atacante ──HTTPS──> supabase.co
                    │
                    └─ Certificado falso emitido por CA instalada no dispositivo
                       → OkHttp compara hash da chave pública
                       → Hash ≠ pin esperado → CONEXÃO REJEITADA ✓
                       → javax.net.ssl.SSLPeerUnverifiedException
```

### Como funciona no app

O `ApiClient` configura o `CertificatePinner` do OkHttp com dois pins:

```java
CertificatePinner certificatePinner = new CertificatePinner.Builder()
        .add("*.supabase.co", PIN_LEAF)         // Certificado do servidor
        .add("*.supabase.co", PIN_INTERMEDIATE)  // CA intermediária (backup)
        .build();

OkHttpClient client = new OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
        .addInterceptor(loggingInterceptor)
        .build();
```

### Pins configurados

| Pin | Certificado | Emissor | Finalidade |
|---|---|---|---|
| `sha256/GU2W4j1P24T3sqlI+o6YTnidzz0PI8fB/Gvd2ITfSZE=` | Leaf (servidor) | `CN=supabase.co` emitido por Google Trust Services WE1 | Pin principal — valida o certificado direto do servidor |
| `sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=` | Intermediária | Google Trust Services WE1 | Pin de backup — se o Supabase renovar o certificado leaf, a CA intermediária mantém a conexão funcionando |

> **Por que dois pins?** Certificados leaf são renovados com frequência (a cada 90 dias no caso
> do Let's Encrypt/Google Trust). Se apenas o leaf fosse pinado, o app pararia de funcionar
> a cada renovação. O pin da CA intermediária serve como **fallback** — enquanto o Supabase
> continuar usando a mesma CA, a conexão continua funcionando.

### Fluxo detalhado

```
ApiClient.getSupabaseService()
  └─> Cria OkHttpClient com CertificatePinner
        └─> Retrofit usa este client para todas as requisições
              └─> Ao conectar em *.supabase.co:
                    1. TLS handshake normal (troca de certificados)
                    2. OkHttp extrai a chave pública do certificado recebido
                    3. Calcula SHA-256 da chave pública
                    4. Compara com os pins configurados:
                       ├── Algum pin corresponde? → Conexão permitida ✓
                       └── Nenhum corresponde? → SSLPeerUnverifiedException ✗
```

### O que o app pina (chave pública vs. certificado completo)

O OkHttp usa **public key pinning** (HPKP), não pinagem de certificado completo:

| Abordagem | O que compara | Vantagem | Desvantagem |
|---|---|---|---|
| **Certificate pinning** | Hash do certificado inteiro (incluindo datas, emissor, etc.) | Mais restritivo | Quebra a cada renovação |
| **Public key pinning** ✓ | Hash apenas da chave pública (SPKI) | Sobrevive a renovações se a mesma chave for reutilizada | Menos restritivo |

### Como atualizar os pins

Quando o Supabase trocar de CA ou renovar os certificados com chaves novas, os pins devem ser atualizados:

```bash
# Obter pin do certificado leaf
echo | openssl s_client -servername SEU_PROJETO.supabase.co \
  -connect SEU_PROJETO.supabase.co:443 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64

# Obter pin da CA intermediária
echo | openssl s_client -servername SEU_PROJETO.supabase.co \
  -connect SEU_PROJETO.supabase.co:443 -showcerts 2>/dev/null \
  | awk '/BEGIN CERT/{n++} n==2' \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Depois, atualize as constantes em `ApiClient.java`:
```java
private static final String PIN_LEAF = "sha256/NOVO_HASH_AQUI=";
private static final String PIN_INTERMEDIATE = "sha256/NOVO_HASH_AQUI=";
```

### Relação com as outras técnicas de segurança

| Técnica | O que protege | SSL pinning complementa |
|---|---|---|
| **EncryptedSharedPreferences** | Dados em repouso (disco) | Protege os dados **em trânsito** (rede) |
| **Detecção de root** | Bloqueia dispositivos comprometidos | Em root, o atacante pode instalar CAs falsas — o pinning impede MITM mesmo assim |
| **Anti-tampering** | Detecta modificação do APK | Impede que um APK modificado com pins removidos se comunique com o Supabase (o atacante precisaria também modificar os pins) |
| **Ofuscação (R8)** | Dificulta leitura do código | Os hashes dos pins ficam ofuscados no APK release, dificultando sua localização |

### Limitações conhecidas

| Limitação | Descrição |
|---|---|
| **Renovação de certificados** | Se o Supabase trocar tanto o certificado leaf quanto a CA intermediária, o app parará de conectar até os pins serem atualizados |
| **Pins hardcoded** | Os hashes estão no código-fonte; atualizar exige novo build e deploy (mitigado pelo pin de backup da CA intermediária) |
| **Bypass com Frida/Xposed** | Em dispositivos rooteados, ferramentas de hooking podem desabilitar o `CertificatePinner` em tempo de execução (mitigado pela detecção de root) |
| **Não protege contra ataques no servidor** | O pinning valida apenas a identidade do servidor, não protege contra comprometimento do próprio backend |

> Para apps em produção, considere implementar um mecanismo de **pin update remoto** (ex.: via
> configuração Firebase Remote Config) para atualizar pins sem exigir novo build do app.

