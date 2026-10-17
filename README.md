# TecOff 📱

> **App Android nativo para resolução de questões do TEC Concursos de modo 100% offline**, com sincronização bidirecional, navegação hierárquica por pastas e subpastas, download em lote (BULK) com proteção contra *rate limit* (HTTP 429), cache de imagens e formatação HTML original com comentários do professor.

Integrado ao ecossistema aberto [tec-agent](https://github.com/warleysr/tec-agent).

---

## ✨ Funcionalidades Principais

1. **Resolução de Questões 100% Offline**:
   - Baixe seus cadernos completos para o dispositivo e estude em qualquer lugar, mesmo sem internet (modo avião, metrô, viagens).
   - Enunciados e comentários em HTML com preservação de formatação, tabelas, fórmulas e imagens baixadas localmente.
   - Alternativas interativas com suporte a **riscar opções (eliminação)** para apoiar o raciocínio.
   - Gabarito oficial e correção comentada passo a passo elaborada pelos professores do TEC.

2. **Navegação Avançada de Questões**:
   - ⏩ **Pra frente (Próxima)**: Avança sequencialmente pelo caderno.
   - ⏪ **Pra trás (Anterior)**: Retorna à questão anterior.
   - ⏭️ **Próxima Não Resolvida**: Salta diretamente para a próxima questão pendente, economizando tempo.
   - 🔀 **Aleatória (Não Resolvida)**: Sorteia uma questão em branco para simular provas ou estudo dinâmico.
   - ↩️ **Desfazer Navegação (Undo Stack)**: Histórico completo com pilha de navegação para voltar com um toque exatamente de onde você veio.

3. **Autenticação no Próprio App (`tec login`)**:
   - **Login Integrado com Navegador**: WebView que carrega o formulário oficial do TEC permitindo autenticar com e-mail/senha e resolver reCAPTCHA v2 / Cloudflare diretamente no app.
   - **Importação de Cookies**: Suporte a colar cookies exportados do navegador ou do CLI `tec` (formato header ou JSON).
   - **Sessão Persistente**: Gerenciamento e renovação automática de `TecPermanecerLogado` (validade de até 14 dias), `JSESSIONID`, `AWSALB` e `AWSALBCORS`.
   - **Tratamento de Sessão Expirada**: Diálogo automático alertando caso a sessão seja invalidada no servidor, permitindo renovar com um toque ou continuar resolvendo offline.

4. **Sincronização Inteligente & Proteção Anti-429**:
   - **Prioridade em BULK**: Utiliza a rota de gabarito em lote do TEC (`/api/cadernos/{id}/gabarito`) para baixar metadados e posições de até 100 questões por página.
   - **Limitador de Taxa (Rate Limiting)**: Intervalo mínimo entre requisições (350ms) e fila sequencial para proteger contra bloqueios de rede.
   - **Backoff Exponencial**: Em caso de resposta 429 (*Too Many Requests*) ou instabilidades temporárias do servidor, o app pausa e retenta automaticamente com retardo inteligente.

5. **Sincronização de Resoluções com o TEC**:
   - As respostas assinaladas offline ficam salvas no banco de dados local Room (`pendingSync = true`).
   - Ao acionar a opção de **Sincronizar Resoluções**, o app envia as respostas em lote (`POST /api/cadernos/{id}/gabarito/resolver`) diretamente para a sua conta no TEC Concursos.
   - As questões ficam marcadas como resolvidas/corrigidas no site oficial, contabilizando em suas estatísticas de estudo.

6. **Estrutura Completa de Pastas e Subpastas**:
   - Exibição em árvore idêntica à organização do usuário no TEC Concursos (Pastas › Subpastas › Cadernos).
   - Indicadores visuais de progresso (resolvidas / total), taxa de acerto (%) e selos de disponibilidade offline.

---

## 🛠️ Arquitetura e Tecnologias

- **Linguagem**: Kotlin (JVM 17)
- **UI Toolkit**: Jetpack Compose & Material 3 (Material Design moderno com tema claro e escuro)
- **Banco de Dados Local**: Room Database (SQLite) com transações e índices otimizados
- **Rede & HTTP**: OkHttpClient com rate-limiting, interceptador de cabeçalhos AWS ALB/TEC e cookie jar inteligente
- **Armazenamento de Imagens**: `ImageStorageManager` que extrai URLs de imagens do HTML, salva em armazenamento interno seguro (`context.filesDir`) e reescreve as fontes para `file:///` local
- **Navegação**: Jetpack Navigation Compose
- **Desserialização**: Google Gson
- **Imagens e WebView**: Android WebView com aceleração de hardware e suporte a ativos locais

---

## 🚀 Como Compilar e Executar

### Pré-requisitos
- JDK 17
- Android SDK (API 34 ou superior)
- Gradle 8.5+

### Compilando via Terminal
```bash
# Definir JAVA_HOME caso necessário (JDK 17):
export JAVA_HOME="/caminho/para/jdk-17"

# Clonar o repositório:
git clone https://github.com/warleysr/TecOff.git
cd TecOff

# Compilar o APK de Debug:
./gradlew assembleDebug

# O APK gerado estará em:
# app/build/outputs/apk/debug/app-debug.apk
```

### Instalando em um dispositivo / emulador conectado:
```bash
./gradlew installDebug
```

---

## 📖 Fluxo de Utilização Recomendado

1. **Primeiro Acesso**:
   - Abra o **TecOff**.
   - Toque no ícone de perfil no canto superior direito para fazer login com suas credenciais do TEC Concursos (resolvendo o captcha na tela do app) ou importando seus cookies do `tec login`.
2. **Sincronizar Pastas**:
   - Toque no botão de sincronização no topo para carregar suas pastas, subpastas e cadernos cadastrados no TEC.
3. **Download Offline**:
   - Escolha o caderno desejado e toque em **"Baixar Offline"**. O app baixará todas as questões, alternativas, imagens e comentários do professor em lote (BULK).
4. **Resolver Questões**:
   - Toque em **"Resolver"** para iniciar.
   - Navegue livremente usando os controles inferiores (**Próxima**, **Anterior**, **Não Resolvida**, **Aleatória**, **Desfazer**).
   - Selecione a alternativa, risque opções improváveis e toque em **"Responder"** para ver a correção instantânea e o comentário didático do professor.
5. **Sincronizar com o TEC**:
   - Quando tiver conexão com a internet, toque no ícone de nuvem (`CloudUpload`) para enviar todas as suas correções offline ao TEC Concursos de uma só vez!

---

## 📄 Licença
Distribuído sob a licença MIT. Consulte `LICENSE` para mais informações.
