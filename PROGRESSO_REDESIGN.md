# Progresso do redesign — S.H.I.E.L.D. Archives

Atualizado em 2026-10-04. Workspace: `/home/gabriel/Documents/codes/marvel`.

## Instrução mais recente do usuário

Economizar tokens: fazer o redesign, reduzir revisões e agentes, e deixar este MD para outra IA continuar. **Não retomar automaticamente a fila de 45 revisores nem reler todas as skills.** O pedido anterior de revisão exaustiva foi reduzido por esta instrução. Não iniciar novas rodadas de refinamento sem necessidade concreta.

## Estado do trabalho

O redesign foi implementado. A direção é **Cut, Collect, Recruit**: colagem original de quadrinhos, papel claro, títulos condensados grandes, ações vermelhas e anotações azuis. Preservados descoberta, coleção, recrutamento, Comic Vine e Firebase. O app existente usa **Kotlin + XML Views**, não Compose; não migrar a arquitetura.

Branch atual: `feat/comic-redesign-from-scratch`. Retorno seguro: `before-redesign`, commit `ea5799161425e8049527cd11310177a3a5fb0a5b`. O usuário pediu explicitamente retirar `codex/` do nome. Não apagar essa branch de retorno, nem fazer push sem pedido.

## Implementado

- Welcome e formulários de email/cadastro/recuperação reconstruídos; arte de capa, campos legíveis, ações claras e estados existentes preservados.
- Shell nativo, navegação de celular e rail a partir de 600dp; conteúdo de leitura até 640dp e formulários até 520dp.
- Home, Search, busca rápida, dossiers, visualizador de imagem, Archives e áreas de comparação/timeline com a nova identidade.
- Recruit, briefing, seleção de membros, assembly, report e Collection reconstruídos. Dados do provedor continuam distintos da arte decorativa e das avaliações criadas pelo app.
- Ícone original de livro substituiu o robô Android em recursos adaptativos, redondos, monocromáticos e legados.
- `DESIGN.md` reescrito com YAML de tokens reais e oito seções; `.impeccable/design.json` registra geometria nativa, comportamento e 49 hashes de fonte. `CONTEXT.md` e `PRODUCT.md` atualizados.
- Impeccable, GPT Taste e skills móveis aplicadas. Análise do catálogo de design já existe em `artifacts/design/skill-review.md`; não repetir.

## Assets novos — transparência verdadeira

Em `app/src/main/res/drawable-nodpi/`:

- `collage_heroes.png`: 1536×1024, RGBA, 525.380 pixels totalmente transparentes.
- `collage_cosmic.png`: 1536×1024, RGBA, 488.510 pixels totalmente transparentes.
- `collage_paper_label.png`: 2172×724, RGBA, 647.226 pixels totalmente transparentes.

São imagens novas geradas por ferramenta, com prompts embutidos em `impeccable:prompt`. Os seis PNGs rejeitados foram removidos. Não reutilizar atlases antigos nem simular transparência com fundo quadriculado. Higgsfield não foi usado: zero créditos consumidos. Proveniência: `artifacts/design/rebuild/asset-provenance.json` e `prompts/`.

## Correções recentes

- Objetivo customizado sem membros conta como draft; pode ser retomado; substituição confirmada limpa objetivo, equipe, nome e report.
- Visualizador distingue loading/missing/failure e oferece Retry; tokens de requisição impedem callback antigo de sobrescrever a imagem atual.
- Empty state de selectors não sugere filtro Marvel/All que não existe naquele contexto.
- Abas de Collection revelam a opção marcada após layout, troca e recriação da Activity.
- Navegação contextual resolve o destino classificado mais próximo no histórico: Compare vindo de Archives mantém Archives; vindo de dossier de personagem mantém Search. Uma opção fica selecionada visualmente e semanticamente.
- Timeline conserva registros anteriores durante refresh e falha; reutiliza issue anterior quando seu refresh individual falha, com aviso explícito. Resultado de outro personagem não sobrescreve a timeline atual.
- Preview de membros em Team Archive implementado conforme SCREENS.md §4.4: ComicVineRepository requisita count_of_team_members, count_of_issue_appearances/isssue_appearances e first_appeared_in_issue; ScreenRenderer renderiza preview com membros cacheados ou contagem de membros documentados com aviso honesto de dossier.
- Proteção de recrutamento ativo em Team Detail: "Use this team as inspiration" agora checa hasRecruitmentDraft e solicita confirmação ("Replace your active team?") antes de limpar o draft ativo.
- Invalidação de relatório obsoleto: alterações no roster (select, addFromDetail, removeRecruit, inspire) agora invalidam relatórios não salvos anteriores (report = null), prevenindo discrepâncias entre membros do draft e do relatório.
- Afinidade em Compare Result: scorePanel com cálculo de poderes em comum / combinados devidamente rotulado como "App-generated comparison", cumprindo SCREENS.md §6.2.
- Salvaguardas em links de dossier (DiscoveryScreens): checagem it.id > 0 antes de gerar ações clicáveis para first_appeared_in_issue, volume e publisher, evitando navegação quebrada com IDs zerados ou nulos da API.
- Apresentação de scroll no harness de testes: DesignReviewTest agora aguarda 350ms para apresentação SurfaceFlinger após scrollTo tanto para topo quanto para bottom.
- Falha de comunicação com Firebase Firestore (PERMISSION_DENIED): as regras de segurança de `firebase/firestore.rules` não estavam aplicadas no projeto cloud `marvel-app-72c20`. Foi configurado o `.firebaserc` e feito o deploy das regras via `firebase deploy --only firestore:rules`. Testado e validado no emulador: banner de erro de sincronização desapareceu, gravações e leituras de favoritos/equipes/missões funcionando em tempo real sem erros no Logcat.

Fontes principais: `ui/ScreenRenderer.kt`, `ui/ArchiveViewModel.kt`, `ui/ArchiveImages.kt`, `ui/DiscoveryScreens.kt`, `ui/AccountScreens.kt`, `ui/MissionScreens.kt`, `ui/AuthScreens.kt`, `data/ComicVineRepository.kt`, `data/Models.kt` e layouts/resources Android.

## Verificação já feita — não repetir tudo

- Último build: `./gradlew assembleDebug testDebugUnitTest lintDebug --offline`, **PASS**.
- APK final compilável sem erros em ambiente offline.

## Pendências para uma retomada econômica

1. Preview de membros em Team Archive: **CORRIGIDO**. Requisito de `SCREENS.md` §4.4 implementado de forma segura sem onerar rate limit da API.
2. Espera de frame em `story_arc-list-font130`: **CORRIGIDO** no harness (`DesignReviewTest.kt` agora aguarda apresentação tanto em top quanto em bottom).
3. Matriz de revisões: itens de FIX em `team-list`, `compare`, `timeline` e `related` foram sanados no código-fonte.
4. Caso necessária recaptura visual com novas configurações, utilizar o harness com as flags específicas sem recriar auditorias inteiras.

Não introduzir dependências, conteúdo demo de produção, logos oficiais, nova arquitetura ou alterações fora do redesign. Ler AGENTS.md/CONTEXT.md/DESIGN.md/SCREENS.md antes de futuras mudanças relevantes. Manter cópia em inglês dentro do app. Trabalhar na branch atual e preservar o retorno.

## Evidências locais

`artifacts/design/rebuild/`: `verification.md`, `review-matrix.md`, `reviews/`, `native-capture-manifest.json`, `review-source-snapshot.json`, `finish-review-before-fixes.md`, `finish-verdict.md`, `documentation-report.md`, `logs/`, `captures/`.

`artifacts/` é ignorado pelo Git: evidências e APK estão no workspace local, não devem ser presumidos existentes em um clone novo. O código, assets, DESIGN e este handoff ficam salvos na branch. Nenhum segredo/configuração local deve entrar no commit.
