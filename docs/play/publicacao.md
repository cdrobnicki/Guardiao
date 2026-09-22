# Publicar na Google Play Store

Este arquivo reúne tudo que o app precisa para ser submetido: textos prontos, respostas de
formulário e o que só você pode fazer. Os gráficos obrigatórios estão nesta mesma pasta.

---

## ⚠️ Leia isto antes de pagar a taxa

A permissão **Acesso a todos os arquivos** (`MANAGE_EXTERNAL_STORAGE`) é a mais restrita da Play
Store. O Google só a aprova para categorias específicas — gerenciador de arquivos, antivírus,
backup, migração de aparelho e gestão de documentos — e exige um **formulário de declaração** e um
**vídeo** mostrando a permissão em uso.

O Guardião se encaixa plausivelmente em "gerenciador de arquivos / gestão de documentos", mas a
aprovação **não é garantida**. Se for recusada, o caminho alternativo é abrir mão da permissão e
usar apenas o seletor de documentos do Android (SAF) e a biblioteca de mídia — o que faria o app
perder a varredura automática e passar a depender de o usuário escolher pastas manualmente.

Vale decidir se esse risco compensa antes de investir na conta de desenvolvedor.

---

## 1. O que só você pode fazer

| Passo | Onde |
|---|---|
| Criar a conta de desenvolvedor (US$ 25, uma vez) e verificar identidade | [play.google.com/console](https://play.google.com/console) |
| Gerar a chave de upload e guardá-la em lugar seguro | seu computador (comando abaixo) |
| Cadastrar os secrets no GitHub | Settings → Secrets and variables → Actions |
| Publicar a página da política de privacidade | Settings → Pages → Source: `main` / `/docs` |
| Gravar as capturas de tela e o vídeo da permissão | seu celular |
| Preencher os formulários e enviar para revisão | Play Console |

### Gerar a chave de upload

Rode **no seu computador**, não aqui. Guarde o arquivo e as senhas em um gerenciador de senhas:
perder essa chave impede publicar atualizações do app.

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

Depois, para cadastrar no GitHub Actions:

```bash
base64 -w0 upload-keystore.jks > keystore.base64.txt   # macOS: base64 -i upload-keystore.jks
```

Crie estes secrets no repositório:

| Secret | Conteúdo |
|---|---|
| `KEYSTORE_BASE64` | o conteúdo de `keystore.base64.txt` |
| `STORE_PASSWORD` | a senha do keystore |
| `KEY_PASSWORD` | a senha da chave (geralmente igual) |
| `KEY_ALIAS` | `upload` |

Com os secrets no lugar, rode o workflow **Build AAB (release)** pela aba Actions. Ele produz o
`.aab` assinado como artefato — é esse arquivo que sobe para o Play Console.

> O `.aab` sai como artefato privado do Actions, e não como release pública, de propósito: é o
> pacote de produção assinado com a sua chave.

---

## 2. Textos da ficha da loja

### Nome do app (máx. 30)
```
Guardião de Arquivos
```

### Descrição curta (máx. 80)
```
Veja quais arquivos do seu celular guardam dados pessoais. Tudo no aparelho.
```

### Descrição completa (máx. 4000)
```
O Guardião de Arquivos vasculha o armazenamento do seu celular e mostra, de forma organizada,
quais dos seus arquivos podem conter informações pessoais — para você decidir o que fazer com
cada um.

COMO FUNCIONA

O app varre fotos, vídeos, áudios, PDFs, documentos do Word, planilhas e apresentações em todo o
armazenamento, incluindo as mídias recebidas por aplicativos de mensagem. Cada arquivo encontrado
recebe uma pontuação de risco de 0 a 100, e o app explica exatamente o que encontrou nele.

A análise combina três frentes, todas dentro do aparelho:

• Nome e pasta do arquivo — termos como CPF, RG, CNH, comprovante, extrato, contrato, laudo,
  currículo, além de pastas de capturas de tela, câmera e gravações de voz.

• Conteúdo dos documentos — CPF e CNPJ (com validação dos dígitos verificadores), números de
  cartão (algoritmo de Luhn), e-mails, telefones, CEP, RG, senhas anotadas, termos bancários e
  de saúde.

• Metadados de mídia — coordenadas de GPS em fotos e vídeos, modelo do aparelho, e áudios sem
  marcação de música, que costumam ser gravações de voz.

O QUE VOCÊ PODE FAZER

• Ver os arquivos organizados por tipo, com os de maior risco sempre no topo.
• Abrir qualquer arquivo no aplicativo que preferir — o app lembra sua escolha por tipo.
• Mover arquivos para uma pasta de quarentena escolhida por você, um a um, vários de uma vez ou
  todos os de risco alto com um toque.
• Restaurar qualquer arquivo da quarentena para o lugar de origem, quando quiser.
• Ver miniaturas de fotos, vídeos, PDFs e documentos, para reconhecer o arquivo sem abrir.

PRIVACIDADE

Nada sai do seu aparelho. O app não cria conta, não tem servidor, não usa publicidade nem
rastreamento — e não declara sequer permissão de acesso à internet, o que impede qualquer
conexão de rede pelo próprio sistema Android. As miniaturas ficam apenas na memória, sem cópia
em disco. O código-fonte é público.

SOBRE A ANÁLISE

A verificação é heurística: ela aponta indícios, não certezas. Pode marcar arquivos inofensivos
e deixar passar outros — fotos de documentos, por exemplo, não são lidas por reconhecimento de
texto. Confira cada arquivo antes de decidir.

POR QUE O APP PEDE ACESSO A TODOS OS ARQUIVOS

Essa permissão é a função central do app: sem ela não é possível encontrar documentos fora das
pastas de mídia nem mover arquivos para a pasta de quarentena que você escolher. O app usa o
acesso apenas para isso, dentro do aparelho.
```

### Outros campos

| Campo | Valor |
|---|---|
| Categoria | Ferramentas |
| E-mail de contato | *(preencha o seu)* |
| Site | `https://cdrobnicki.github.io/Guardiao/` |
| Política de privacidade | `https://cdrobnicki.github.io/Guardiao/politica-de-privacidade.html` |

---

## 3. Formulário de Segurança dos dados

O app não coleta nada, então o formulário é curto:

| Pergunta | Resposta |
|---|---|
| Seu app coleta ou compartilha algum dos tipos de dados exigidos? | **Não** |
| Todos os dados do usuário são criptografados em trânsito? | Não se aplica — não há transmissão |
| Você oferece uma forma de solicitar exclusão de dados? | Não se aplica — desinstalar o app remove tudo |
| Os dados são processados apenas no dispositivo? | **Sim** |

Justificativa, se pedirem: *o aplicativo não possui permissão de rede (`android.permission.INTERNET`
não é declarada), portanto nenhum dado pode ser transmitido. O histórico da quarentena e as
preferências ficam na área privada do app e o backup automático está desativado.*

---

## 4. Declaração da permissão de acesso a todos os arquivos

Texto sugerido para o formulário:

```
O Guardião de Arquivos é um aplicativo de gestão e organização de arquivos. Sua função central é
percorrer o armazenamento do dispositivo, identificar documentos, fotos, vídeos e áudios que
contenham informações pessoais, e permitir que o usuário mova esses arquivos para uma pasta de
quarentena escolhida por ele.

Por que MANAGE_EXTERNAL_STORAGE é necessária:

1. A varredura precisa alcançar documentos (PDF, Word, planilhas) em qualquer pasta do
   armazenamento. A MediaStore expõe apenas imagens, vídeos e áudio, e não os documentos que
   concentram os dados pessoais mais sensíveis.

2. O app move arquivos entre pastas arbitrárias, da origem para a quarentena escolhida pelo
   usuário, e os restaura de volta ao caminho original. O Storage Access Framework exigiria que o
   usuário selecionasse manualmente cada pasta de origem antes de qualquer varredura, o que
   inviabiliza a função de "varrer o aparelho".

3. O acesso é usado exclusivamente para leitura, análise local e movimentação de arquivos a pedido
   do usuário. O aplicativo não possui permissão de internet e não transmite nada.
```

**Vídeo exigido:** grave a tela do celular (30 a 60 segundos) mostrando, em sequência: a tela
inicial pedindo a permissão, a concessão na tela do sistema, a varredura em andamento, a lista de
resultados e um arquivo sendo movido para a quarentena. Suba no YouTube como *não listado* e cole
o link no formulário.

---

## 5. Capturas de tela

Mínimo de 2, recomendado 4 a 8. Formato: PNG ou JPEG, lado maior entre 320 e 3840 px.
Telas que valem capturar:

1. Tela inicial depois da varredura, com os números e as categorias.
2. Varredura em andamento, com o cão farejando.
3. Lista de uma categoria, com as pontuações de risco.
4. Detalhe de um arquivo, mostrando o que foi encontrado.
5. Tela de quarentena.

> Use arquivos de exemplo, não os seus documentos reais — a imagem fica pública na loja.

---

## 6. Gráficos prontos

| Arquivo | Uso | Tamanho |
|---|---|---|
| `icone-512.png` | Ícone da loja | 512 × 512 |
| `destaque-1024x500.png` | Gráfico de destaque | 1024 × 500 |

---

## 7. Ordem sugerida

1. Criar a conta de desenvolvedor e aguardar a verificação de identidade (pode levar dias).
2. Ativar o GitHub Pages e conferir se a política de privacidade abre.
3. Gerar a chave, cadastrar os secrets, rodar o workflow e baixar o `.aab`.
4. Criar o app no Play Console e preencher a ficha com os textos acima.
5. Subir o `.aab` em um **teste interno** primeiro — instale a partir dele e confirme que o app
   de release funciona igual ao de debug.
6. Preencher Segurança dos dados, classificação de conteúdo e a declaração da permissão.
7. Enviar para revisão.
