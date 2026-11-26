# PatCode – Leitor de etiquetas de patrimônio (Android)

App Android que lê o código de barras das etiquetas de patrimônio, salva tudo em um arquivo CSV no próprio celular, mostra a lista e o conteúdo do arquivo e compartilha por e-mail, WhatsApp ou qualquer outro app.

## O que o app faz

- **Escanear etiqueta**: abre a câmera e lê os formatos comuns (Code 128, Code 39, EAN, QR Code, Data Matrix etc.). Funciona sem internet.
- **Leitura contínua**: depois de cada leitura a câmera abre de novo, para levantar uma sala inteira rápido. Códigos repetidos são ignorados e o app avisa.
- **Lanterna**: liga o flash para ambientes escuros.
- **Local / Setor**: o que você digita nesse campo é gravado junto com as próximas leituras (ex.: "Sala 12 – Financeiro").
- **Digitar código**: para etiqueta danificada ou ilegível.
- **Tocar em um item**: adiciona uma observação (estado, responsável…) ou exclui o item.
- **Compartilhar**: envia o arquivo `patrimonio.csv` por WhatsApp, Gmail, Outlook, Drive etc.
- **Menu (⋮)**: ver o conteúdo do arquivo, compartilhar a lista como texto, salvar uma cópia em outra pasta (ex.: Downloads) e apagar tudo.

O arquivo abre direto no Excel ou no Google Planilhas. Ele tem as colunas `Codigo;Formato;DataHora;Local;Observacao`, separadas por ponto e vírgula, em UTF-8.

---

## Como gerar o APK (sem instalar nada no computador) – recomendado

O projeto já vem com um robô do **GitHub Actions** que compila o APK de graça na nuvem.

1. Crie uma conta grátis em https://github.com (se ainda não tiver).
2. Clique em **New repository**, dê o nome `PatCode` e clique em **Create repository**.
3. Na página do repositório, clique em **uploading an existing file** e arraste **todo o conteúdo** desta pasta, incluindo a pasta oculta `.github`. Depois clique em **Commit changes**.
   - Dica: se o navegador não mostrar a pasta `.github`, use o GitHub Desktop ou o comando `git push`.
4. Abra a aba **Actions**. O processo "Gerar APK" roda sozinho e leva de 3 a 6 minutos.
5. Quando ficar verde ✅, abra **Releases** (lado direito da página inicial do repositório) e baixe o **PatCode.apk**. Se abrir essa página no próprio celular, o download já fica no aparelho.

## Alternativa: gerar com o Android Studio

1. Instale o Android Studio: https://developer.android.com/studio
2. **File → Open** e selecione esta pasta. Aguarde a sincronização do Gradle.
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.
   - Ou ligue o celular por cabo USB com a **Depuração USB** ativada e clique em ▶ **Run**. O app é instalado e abre direto.

---

## Como instalar no celular sem a Play Store

1. **Leve o APK para o celular**: baixe pelo link do GitHub Releases, ou mande o arquivo para você mesmo por WhatsApp, e-mail, Google Drive ou cabo USB.
2. **Abra o arquivo** `PatCode.apk` (pela notificação de download ou pelo app **Arquivos → Downloads**).
3. O Android vai avisar que a instalação de apps desconhecidos está bloqueada. Toque em **Configurações** e ative **Permitir desta fonte** para o app que você usou para abrir o APK (Chrome, Arquivos, WhatsApp…).
   - Caminho manual: **Configurações → Apps → Acesso especial → Instalar apps desconhecidos**.
4. Volte e toque em **Instalar**.
5. Se o **Google Play Protect** mostrar "App não verificado", toque em **Mais detalhes → Instalar mesmo assim**. O aviso aparece porque o app não veio da loja; o código é o deste projeto.
6. Abra o **PatCode** e, no primeiro uso, permita o acesso à **câmera**.

Requisito: Android 7.0 ou superior.

### Atualizar o app
Gere um APK novo e instale por cima. Os registros são mantidos.
**Atenção:** desinstalar o app apaga o arquivo de dados. Compartilhe ou salve uma cópia antes.

### Onde o arquivo fica no celular
`Android/data/br.patcode.app/files/patrimonio.csv`
No Android 11 ou superior essa pasta é protegida. Para ter o arquivo em uma pasta comum, use **Menu → Salvar cópia em…** e escolha **Downloads**.

---

## Estrutura do projeto

```
app/src/main/java/br/patcode/app/
  MainActivity.kt            tela principal, leitura, compartilhamento
  Repositorio.kt             gravação e leitura do CSV
  ItensAdapter.kt            lista de itens
  CaptureActivityPortrait.kt câmera em modo retrato
.github/workflows/build-apk.yml   compilação automática do APK
```

Biblioteca de leitura: [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded) (código aberto, funciona offline).
