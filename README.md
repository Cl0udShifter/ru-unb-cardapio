# RU UnB - Cardápio do Restaurante Universitário (Campus Darcy Ribeiro)

Aplicativo Android nativo em **Jetpack Compose** para consulta rápida, moderna e offline do cardápio semanal do Restaurante Universitário da Universidade de Brasília (UnB - Darcy Ribeiro).

Projetado com base na **Direção Visual Notion Editorial (Warm Paper)**: estética limpa, tipografia serifada/sans acolhedora, espaçamento generoso e ergonomia pensada para uso diário com apenas uma mão (thumb zone).

---

## Recursos

- **Zero PDF:** Acaba com a necessidade de abrir PDFs semanais e dar zoom para ler os pratos.
- **Offline-First:** O cardápio abre instantaneamente, guardando em cache a última versão baixada e incluindo dados de contingência nos assets.
- **Uso com Uma Só Mão:**
  - Fita horizontal superior para navegar entre os dias da semana (Segunda a Domingo).
  - Pílula inferior flutuante ao alcance do polegar para alternar entre **Café da Manhã**, **Almoço** e **Jantar**.
  - Suporte a gesto de arrasto (swipe) lateral para trocar de dia.
- **Detecção Temporal Inteligente:** Abre automaticamente na refeição e dia de acordo com o horário do seu celular.
- **Automação Diária Gratuita:** Pipeline no GitHub Actions que consulta o site oficial do RU (`https://ru.unb.br/cardapio-refeitorio/`), extrai o PDF com `pdfplumber` e gera o JSON atualizado.

---

## Estrutura do Projeto

- `app/`: Aplicativo Android em Jetpack Compose (Kotlin).
  - `data/`: Modelos, Gson e repositório com política de cache local e fallback.
  - `ui/theme/`: Cores Notion Warm Paper (`#FBFBFA`), tipografia editorial e temas claro/escuro.
  - `ui/components/`: Componentes modulares (DaySelectorRail, MealBottomControl, MealSectionCard, DietaryChip).
  - `ui/screens/`: Tela principal unificada.
- `parser/`: Script Python de extração do PDF oficial com `pdfplumber`.
- `.github/workflows/`: Automação periódica que atualiza o cardápio no GitHub.

---

## Como Executar

### Pré-requisitos
- JDK 17
- Android SDK (API 34)

### Compilação e Instalação
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
