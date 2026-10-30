# Ideias e Itens a Fazer (Roadmap RU UnB)

Lista de melhorias, novos recursos e complementos para a evolução do aplicativo, estruturados como itens de execução.

---

## 1. Widget de Tela Inicial (Jetpack Glance)
Um widget minimalista (tamanhos sugeridos 4x2 e 2x2) integrado à tela inicial do Android.

- [ ] **Exibição Dinâmica da Refeição:** Apresentar automaticamente o prato principal da refeição vigente (ou da próxima se o horário da atual já tiver encerrado) sem exigir a abertura do app.
- [ ] **Atalho Rápido para Catraca:** Incluir um botão de toque único no widget que abre instantaneamente a carteirinha digital em tela cheia com brilho máximo, agilizando a entrada no refeitório.
- [ ] **Arquitetura com Jetpack Glance:** Utilizar a biblioteca `androidx.glance:glance-appwidget` para construir o componente com a mesma linguagem declarativa do Jetpack Compose.
- [ ] **Cache Local:** Ler diretamente do repositório em cache (`MenuRepository`), garantindo carregamento instantâneo e funcionamento 100% offline.

---

## 2. Quick Settings Tile ("Catraca RU")
Atalho nos botões de configuração rápida da barra de notificações do Android (ao lado de Wi-Fi, Lanterna, Bluetooth, etc.).

- [ ] **Implementação do `TileService`:** Criar `CatracaTileService.kt` herdando de `android.service.quicksettings.TileService`.
- [ ] **Registro no Manifest:** Declarar o serviço no `AndroidManifest.xml` com a permissão `android.permission.BIND_QUICK_SETTINGS_TILE`.
- [ ] **Acesso Instantâneo:** Ao puxar a barra superior e tocar no bloco, acionar a `MainActivity` direcionando para a tela da carteirinha com o QR Code pronto para aproximação no vidro da catraca.
- [ ] **Ícone do Bloco:** Utilizar o ícone vetorial da carteirinha (`ic_stat_ru_menu` ou similar monocromático).

---

## 3. Projeção de Refeições Restantes e Alerta de Recarga
Cálculo inteligente do rendimento do saldo no cartão do estudante.

- [ ] **Contador de Refeições:** Relacionar o saldo atual com a categoria do usuário (ex: R$ 6,10 para estudantes não isentos) e exibir um indicador direto: *"Seu saldo rende 3 almoços/jantares e sobram R$ 3,15"*.
- [ ] **Alerta Preventivo de Saldo Insuficiente:** Notificação sutil no final da tarde caso o saldo fique abaixo do custo de uma refeição: *"Saldo insuficiente para a refeição de amanhã. Lembre-se de recarregar no RU"*.
- [ ] **Integração no Extrato:** Posicionar o indicador de projeção logo abaixo do saldo atual em `StudentCardScreen.kt`.

---

## 4. Termômetro de Fila (Colaborativo e Heurístico)
Estimativa visual do tempo de espera nas catracas do Campus Darcy Ribeiro.

- [ ] **Previsão Heurística por Horário:** Exibir status baseado em padrões típicos de movimentação durante os horários de pico (11h45 às 12h45 e 17h45 às 18h30):
  - Verde: Fila tranquila (espera estimada de 1 a 5 min)
  - Amarelo: Movimento moderado (espera estimada de 5 a 15 min)
  - Laranja: Horário de pico (espera estimada acima de 15 min)
- [ ] **Confirmação Comunitária de 1 Toque:** Após a leitura da carteirinha ou ao abrir o cardápio, disponibilizar um botão simples para os estudantes informarem rapidamente como está a fila.
- [ ] **Consolidação em Nuvem:** Endpoint leve (ex: Cloudflare Worker ou Firebase Realtime Database) para consolidar as respostas dos últimos 20 minutos.

---

## 5. Filtro de Preferência Alimentar no Cardápio
Reorganização visual automática das seções de prato conforme a dieta do estudante.

- [x] **Persistência da Dieta:** Integrada com as preferências salvas no app (Padrão, Ovolactovegetariano ou Vegano).
- [x] **Inversão da Hierarquia Visual:** Ao selecionar dieta vegetariana ou vegana, o prato correspondente sobe automaticamente para o topo do card como prato destacado (`MealSectionCard.kt`), reduzindo o destaque visual das carnes convencionais.
- [x] **Filtro Rápido na Tela Inicial:** Barra de seleção rápida (`DietFilterBar.kt`) diretamente abaixo dos dias da semana para alternância instantânea com 1 toque.
- [x] **Hierarquia de Pílulas Limpa:** Pílulas coloridas mantidas exclusivamente nos pratos principais (Padrão, Ovolacto, Vegano), convertendo sobremesa, refresco, fruta e complementos extras para linhas limpas.

---

## 6. Expansão para Outros Campi da UnB (FGA, FCE, FUP)
Suporte completo aos refeitórios das outras unidades acadêmicas da Universidade de Brasília.

- [x] **Mapeamento de Páginas no Parser:** Atualizado `parser/scrape_unb_ru.py` para capturar as seções específicas de Darcy Ribeiro, FGA (Gama), FCE (Ceilândia) e FUP (Planaltina).
- [x] **Geração de Arquivos JSON Separados:** Estruturados `menu_darcy.json`, `menu_gama.json`, `menu_ceilandia.json` e `menu_planaltina.json` tanto no repositório quanto em `assets/` para fallback offline.
- [x] **Seletor de Campus na UI:** Adicionado subtítulo interativo com Bottom Sheet Notion Editorial para alternar o campus selecionado com 1 toque, persistido em `SharedPreferences` e integrado aos alertas de notificação.
