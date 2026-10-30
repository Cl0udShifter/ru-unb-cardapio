# RU UnB - Cardápio & Carteirinha Digital (Campus Darcy Ribeiro)

[![Latest Release](https://img.shields.io/github/v/release/Cl0udShifter/ru-unb-cardapio?label=Download%20APK&color=success)](https://github.com/Cl0udShifter/ru-unb-cardapio/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.00-blue.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Enabled-teal.svg)](https://m3.material.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Aplicativo Android nativo para consulta rápida, moderna e offline do cardápio semanal, emissão de carteirinha digital com QR Code para as catracas e acompanhamento de saldo do Restaurante Universitário (RU) da Universidade de Brasília (**UnB - Campus Darcy Ribeiro**).

Projetado sob a filosofia visual **Notion Editorial (Warm Paper)**: estética limpa, tipografia acolhedora, espaçamento generoso, tema claro/escuro integrado com edge-to-edge e ergonomia pensada para uso diário com apenas uma mão.

---

## 📥 Instalação Recomendada (.APK Pronto)

> **Não é necessário compilar o código fonte.** Você pode baixar diretamente a versão estável pronta para instalar no seu celular Android:

### 👉 **[Baixar RU-UnB.apk (Última Versão Oficial)](https://github.com/Cl0udShifter/ru-unb-cardapio/releases/latest/download/RU-UnB.apk)**

Você também pode acompanhar todas as versões, notas de lançamento e histórico de mudanças na página oficial de **[Releases no GitHub](https://github.com/Cl0udShifter/ru-unb-cardapio/releases)**.

### Passo a passo para instalar no Android:
1. Toque no link acima para baixar o arquivo `RU-UnB.apk` no seu celular.
2. Quando o download terminar, abra o arquivo através da barra de notificações ou pelo aplicativo de *Downloads / Gerenciador de Arquivos*.
3. Se o Android exibir o aviso *"Para sua segurança, seu smartphone não tem permissão para instalar apps desconhecidos desta fonte"*, toque em **Configurações** e ative a opção **Permitir desta fonte**.
4. Toque em **Instalar** e abra o app!

### 🔄 Sistema de Atualização Integrado (In-App Updates):
O aplicativo conta com checagem de atualizações automática e manual conectada diretamente aos lançamentos do GitHub:
- **Verificação Automática:** Ao abrir, o app verifica silenciosamente se há uma versão mais nova disponível. Caso haja, uma janela intuitiva exibe as novidades e o botão **"Atualizar Agora"**.
- **Verificação Manual:** Na tela da Carteirinha Digital, acesse o botão **Buscar atualização** para checar sob demanda.
- **Instalação em 1 Toque:** O app faz o download em segundo plano com barra de progresso e abre automaticamente o instalador de pacotes do Android, sem necessidade de baixar arquivos manualmente pelo navegador.

---

## 🍽️ Principais Funcionalidades

### 1. Cardápio Semanal Sem Fricção
- **Zero PDFs:** Elimina a necessidade de abrir PDFs semanais e dar zoom para ler os pratos.
- **Detecção Temporal Automática:** Ao abrir, o app identifica o horário do dispositivo e seleciona diretamente a refeição atual:
  - **Café da Manhã:** das 07h00 às 09h30
  - **Almoço:** das 11h00 às 14h30
  - **Jantar:** das 17h00 às 19h30
- **Navegação por Fita & Gesto:** Fita de dias da semana (Segunda a Domingo) no topo e controle inferior flutuante ao alcance do polegar (*thumb zone*), com suporte a gesto lateral (*swipe*).
- **Classificação Dietética:** Destaque para opções Padrão, Ovolactovegetariana, Vegana e Sobremesa/Fruta.
- **Pull to Refresh:** Puxe a tela para baixo a qualquer momento para atualizar o cardápio.
- **Offline-First:** Guarda em cache o cardápio mais recente com fallback imediato para dados embutidos.

### 2. Carteirinha Digital & Catraca do RU
- **Acesso com 1 Toque:** Gere e visualize o QR Code oficial de 16 dígitos aceito pelos leitores ópticos das catracas do RU.
- **Modo Tela Cheia & Brilho Máximo:** Toque no QR Code para expandir e maximizar o brilho da tela, acelerando a leitura nas catracas mesmo sob luz forte.
- **Sincronização com o SIGAA:** Login integrado com o portal discente da UnB para extrair automaticamente o token da carteirinha, nome, matrícula, curso e validade.
- **100% Offline:** Uma vez sincronizada, a carteirinha abre instantaneamente sem depender de conexão à internet.

### 3. Saldo & Extrato de Consumo
- **Saldo Atual:** Acompanhamento do saldo em conta no RU diretamente no app.
- **Histórico de Transações:** Lista detalhada de débitos de refeições e recargas efetuadas.
- **Atualização Silenciosa:** Atualize o saldo com um clique sem necessidade de redigitar credenciais.
- **Privacidade & Segurança:** Credenciais armazenadas exclusivamente de forma local no dispositivo do estudante.

### 4. Notificações Inteligentes & Granulares
- **Avisos Prévios:** Notificações disparadas 1 hora antes de cada refeição (6h00 para café, 10h00 para almoço e 16h00 para jantar).
- **Controle por Dia da Semana:** Configure individualmente quais dias e quais refeições deseja ser notificado.
- **Deep Linking:** Tocar na notificação abre o app diretamente na aba da respectiva refeição.
- **Segundo Plano & Economia de Bateria:** Diferentes fabricantes de smartphones Android (Xiaomi HyperOS/MIUI, Samsung OneUI, Motorola, Realme, Asus, etc.) possuem políticas agressivas de suspensão de processos em segundo plano. Para garantir o recebimento dos alertas diários com o app fechado ou em repouso:
  - Abra as **Informações do App** nas configurações do Android;
  - Defina o uso de bateria como **Sem restrições** (ou *Não otimizar*);
  - Em aparelhos Xiaomi/Redmi/POCO, certifique-se também de habilitar a permissão de **Início Automático** (*Autostart*).

### 5. Guia Oficial & Regulamento do RU
- Consulta integrada às regras e valores do restaurante:
  - **Grupo 1 (Isentos):** R\$ 0,00 (PNAES / CadÚnico)
  - **Grupo 2 (Estudantes UnB):** R\$ 6,10 (Graduação e Pós-graduação)
  - **Grupo 4 (Comunidade Universitária):** R\$ 14,35
  - **Grupo 3 (Visitantes):** R\$ 17,90
  - **4º Andar (Bufê Executivo):** R\$ 60,42/kg
- Orientações de circulação, salão vegetariano e devolução de bandejas.

---

## 🎨 Design System: Notion Editorial

- **Modo Claro (Warm Paper):** Fundo off-white acolhedor (`#F9F9F7` / `#FBFBFA`), superfície pura (`#FFFFFF`) e tipografia de alto contraste (`#2E2E2E`).
- **Modo Escuro:** Tons profundos suaves (`#191918` e `#242424`) com integração contínua até a barra de navegação do sistema (edge-to-edge).
- **Tipografia:** Hierarquia refinada com títulos em serif editorial e corpos de texto em sans-serif geométrica limpa.

---

## 🛠️ Arquitetura e Tecnologias

O projeto segue padrões modernos de desenvolvimento Android:

- **Linguagem:** Kotlin
- **Toolkit de UI:** Jetpack Compose + Material 3
- **Gerador de QR Code:** ZXing (`com.google.zxing:core`)
- **Comunicação HTTP:** OkHttp 4 com suporte a cookies em sessão e redirecionamentos CAS/SSO
- **Serialização:** Google Gson
- **In-App Updater:** Integração nativa com GitHub Releases API, `FileProvider` e Package Installer do Android
- **Manipulação de PDF:** Android PdfRenderer para processamento local de documentos
- **Parser Automatizado:** Script Python (`pdfplumber` + `requests`) executado via GitHub Actions para converter os PDFs da UnB em JSON padronizado.

### Estrutura de Pacotes

```text
app/src/main/java/br/unb/ru/
├── data/
│   ├── api/
│   │   └── RuApiService.kt             # Cliente HTTP e sincronização remota
│   ├── model/
│   │   ├── AppUpdateInfo.kt            # Modelo de dados de atualizações remotas
│   │   ├── MenuModels.kt               # Modelos de dados do cardápio semanal
│   │   └── StudentCardModel.kt         # Modelos da carteirinha e extrato
│   └── repository/
│       ├── AppUpdateRepository.kt      # Download, checagem e instalação de APK
│       ├── MenuRepository.kt           # Gerenciamento de cache e fallback
│       └── StudentCardRepository.kt    # Integração SIGAA, parsing e persistência
├── notifications/
│   ├── MealNotificationReceiver.kt     # Disparo de notificações locais
│   └── NotificationScheduler.kt        # Agendamento periódico inteligente
└── ui/
    ├── components/
    │   ├── AppUpdateDialog.kt          # Diálogo de novidades e progresso de download
    │   ├── DaySelectorRail.kt          # Seletor superior de dias da semana
    │   ├── MealBottomControl.kt        # Controle inferior de refeições
    │   ├── MealSectionCard.kt          # Cards de itens do cardápio
    │   ├── NotificationSettingsBottomSheet.kt # Gerenciador granular de alertas
    │   └── StudentCardScreen.kt        # Carteirinha digital, extrato, guia e updater
    ├── screens/
    │   └── MenuHomeScreen.kt           # Tela principal unificada
    └── theme/
        ├── Color.kt                    # Paleta Warm Paper e Dark Notion
        ├── Theme.kt                    # Configuração de temas e status bar
        └── Type.kt                     # Escala tipográfica editorial
```

---

## 💻 Para Desenvolvedores: Como Compilar

Caso deseje compilar o projeto a partir do código fonte:

### Pré-requisitos
- JDK 17
- Android SDK instalado com Build Tools 34+
- Dispositivo físico ou emulador com Android 8.0 (API 26) ou superior

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/Cl0udShifter/ru-unb-cardapio.git
   cd ru-unb-cardapio
   ```

2. **Compile o APK de depuração:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Instale via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Privacidade

- As credenciais de acesso ao SIGAA são utilizadas unicamente para autenticação direta com os servidores oficiais da Universidade de Brasília (`sigaa.unb.br`).
- Nenhum dado pessoal, senha, matrícula ou histórico é transmitido para servidores de terceiros ou serviços externos. Todo o armazenamento é local no dispositivo do usuário.

---

## 📄 Licença

Distribuído sob a licença MIT. Consulte `LICENSE` para mais informações.
