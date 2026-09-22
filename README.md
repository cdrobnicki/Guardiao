# Guardião de Arquivos

Aplicativo Android que varre todo o armazenamento do celular, lista fotos, vídeos, áudios, PDFs,
documentos, planilhas e apresentações por categoria e analisa, **inteiramente no aparelho**, quais
arquivos aparentam conter informações pessoais. Para cada arquivo é possível **abri-lo** em outro
app ou **movê-lo para uma pasta de quarentena** escolhida pelo usuário (e restaurá-lo depois).

## Funcionalidades

- **Varredura completa** do armazenamento compartilhado e de cartões SD, incluindo `Android/media`
  (onde ficam as mídias do WhatsApp). Pastas ocultas, caches e `Android/data` são ignorados.
- **Categorias**: Fotos, Vídeos, Áudio, PDF, Documentos, Planilhas e Apresentações, com contagem
  total e quantidade de arquivos com indícios de dados pessoais em cada uma.
- **Análise de informações pessoais** (heurística, offline, sem enviar nada para a internet):
  - *Nome e pasta*: palavras como CPF, RG, CNH, comprovante, extrato, contrato, senha, currículo,
    laudo, exame etc.; pastas de capturas de tela, câmera, WhatsApp/Telegram, gravações de voz.
  - *Conteúdo* de PDF, DOC/DOCX, ODT, RTF, TXT, CSV, XLS/XLSX, ODS, PPT/PPTX, ODP e EPUB:
    CPF e CNPJ (com validação de dígitos), números de cartão (Luhn), e-mails, telefones, CEP, RG,
    senhas/tokens, termos bancários, de saúde e de documentos pessoais.
  - *Metadados* de fotos (EXIF: GPS, modelo do aparelho, autor), vídeos (localização) e áudios
    (áudios sem tags de música são tratados como prováveis gravações de voz).
  - Cada arquivo recebe uma pontuação de 0 a 100 e um nível: **Alto**, **Médio**, **Baixo** ou
    **Sem indícios**, com a lista de evidências encontradas (dados sensíveis são exibidos mascarados).
- **Pontuação de risco de 0 a 100** em cada arquivo, mostrada como anel preenchido com o número no
  centro. As listas vêm **ordenadas do maior risco para o menor**, mantendo a divisão por tipo de
  arquivo.
- **Miniaturas** das fotos e dos vídeos nas listas, no detalhe e na quarentena, para revisar sem
  precisar abrir cada arquivo. São decodificadas no tamanho exibido e mantidas **apenas em
  memória**: nenhuma cópia de conteúdo pessoal é gravada em cache no disco.
- **Abrir** qualquer arquivo no app apropriado (via FileProvider).
- **Quarentena**: escolha uma pasta pelo seletor do sistema; arquivos movidos ficam listados na
  aba Quarentena, de onde podem ser abertos, **restaurados** para o local original ou esquecidos.

## Identidade visual

O ícone é um Jack Russell farejador em traço plano e minimalista: cabeça de frente, orelhas
dobradas e a mancha caramelo assimétrica sobre um olho, que é a marca da raça. Vem como ícone
adaptativo (`fundo` + `primeiro plano` + camada `monochrome` para os ícones temáticos do Android
13+) e também como bitmap em cinco densidades, para os launchers do Android 7 e 8.

A paleta do app sai do próprio ícone — azul-marinho e caramelo — e é **fixa**, não a cor dinâmica
do sistema: as cores de risco precisam significar sempre a mesma coisa. Há tema claro e escuro, com
as cores de risco em variantes próprias para manter o contraste legível nos dois.

Enquanto a varredura roda, o mesmo cão aparece de perfil farejando o chão, com a cabeça balançando
e marcas de cheiro subindo à frente do nariz.

## Permissões

- Android 11 ou superior: **Acesso a todos os arquivos** (`MANAGE_EXTERNAL_STORAGE`), concedido em
  uma tela do sistema aberta pelo próprio app. É necessário para ler documentos fora das pastas de
  mídia e para mover arquivos.
- Android 7 a 10: permissões de leitura e escrita do armazenamento.

O app não usa internet: nenhum arquivo ou resultado sai do aparelho. O projeto não declara a
permissão `INTERNET` e não tem nenhuma dependência de rede — a análise usa apenas a biblioteca
padrão do Java/Kotlin e as APIs do próprio Android.

## Como compilar

**Pré-requisitos:** [Android Studio](https://developer.android.com/studio) (ou JDK 21 + Gradle 9.3.1).

1. Abra o projeto no Android Studio e deixe-o sincronizar.
2. O tipo de build `debug` usa `./debug.keystore` (ignorado pelo git). Gere um com:
   ```bash
   keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey \
     -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```
   ou remova a linha `signingConfig = signingConfigs.getByName("debugConfig")` de `app/build.gradle.kts`.
3. Execute no aparelho ou emulador (`gradle assembleDebug` gera `app/build/outputs/apk/debug/`).

Os testes de unidade da análise (`gradle testDebugUnitTest`) cobrem validação de CPF/CNPJ/Luhn,
detecção em texto, heurísticas de nome/pasta, extração de texto de DOCX e PDF, e a ordenação por
pontuação de risco.

O workflow do GitHub Actions (`.github/workflows/build-apk.yml`) roda os testes e publica o APK de
debug como artefato a cada push.

## Estrutura

```
app/src/main/java/com/guardiao/arquivos/
├── MainActivity.kt
├── GuardiaoApp.kt   # carregador de miniaturas (somente em memória, sem cache em disco)
├── scanner/         # categorias, varredura, heurísticas e extração de texto
│   ├── FileCategory.kt / Models.kt
│   ├── FileScanner.kt / StoragePermissions.kt
│   ├── PatternDetectors.kt / FilenameHeuristics.kt / TextExtractor.kt
│   └── PrivacyAnalyzer.kt / AndroidMediaInspector.kt
├── quarantine/      # banco Room com o histórico e movimentação/restauração de arquivos
└── ui/              # ViewModel e telas em Jetpack Compose
    ├── theme/       # paleta da marca, cores de risco e escala tipográfica
    └── screens/     # início, lista, detalhe, quarentena, miniaturas, anel de risco e o cão
```

## Limitações conhecidas

- A análise é heurística: pode haver falsos positivos (ex.: números que parecem CPF) e falsos
  negativos (ex.: fotos de documentos não são reconhecidas por OCR).
- PDFs com fontes com codificação personalizada ou apenas imagens não têm texto extraído.
- Arquivos dentro de `Android/data` de outros apps não são acessíveis pelo sistema.
- Miniaturas de HEIC/HEIF dependem do Android 9 ou superior; abaixo disso a lista mostra o ícone da
  categoria no lugar da pré-visualização.
