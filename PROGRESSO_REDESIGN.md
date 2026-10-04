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
- Listas relacionadas têm snapshots em memória por `Route.id`: título, registros e limite. Back de lista aninhada restaura a lista anterior. Snapshots sobrevivem à recriação da Activity, são descartados ao sair do fluxo e **não** são serializados após morte de processo; nesse caso há recuperação vazia honesta.

Fontes principais: `ui/ScreenRenderer.kt`, `ui/ArchiveViewModel.kt`, `ui/ArchiveImages.kt`, `ui/DiscoveryScreens.kt`, `ui/AccountScreens.kt`, `ui/MissionScreens.kt`, `ui/AuthScreens.kt` e layouts/resources Android.

## Verificação já feita — não repetir tudo

- Último build: `./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --offline`, **PASS em 17s**, após as correções recentes. Log: `artifacts/design/rebuild/logs/final-gradle.txt`.
- APK final: `artifacts/shield-archives-debug.apk`. SHA-256: `041e2deb19289cebd47805ffeb6cfa366ab86689090872aa6d7da9823740905f`.
- APK instalado e APK de instrumentation conferidos por hash: correspondem ao último build.
- `DesignReviewTest#captureContextNavigation`, **PASS no celular, 85.291s**, 18 capturas. Executa Compare a partir de Archives/dossier, selector/Back, seleção nativa, recriação, nested View All → Load more → Back duas vezes, e Retry de Timeline mantendo a issue. Falha de Timeline é fixture de identificador inválido, não falha de rede demonstrada. Log `context-native-phone.txt`; manifest `contextNavigationRuns.phone`.
- Collection: 7 estados por configuração, PASS celular 12.870s, fonte 1,3 10.959s, tablet 43.814s. Asserts de chip inteiramente visível, clique nativo e recriação.
- Antes das últimas correções contextuais: 65 estados por configuração, PASS celular 65.737s/fonte 1,3 65.857s/tablet 64.153s. Capturas anteriores têm limites de versão; não afirmar que todas mostram o APK final.
- Fluxos reais anteriores passaram: GoogleAuthProtocolTest (2 testes), ReportGenerationTest, EmailAuthFlowTest e ArchiveFlowTest. Usam Comic Vine real e Firebase local. **Google OAuth e Firebase de produção não foram certificados.** Capturas de coleção/report usam fixtures declaradas de registros reais, com Firestore offline.
- Parecer `finish-verdict.md`: ship restrito às quatro correções da revisão principal; não é aprovação universal das 45 telas.
- PNGs/metadata, YAML/JSON, hashes documentados e `git diff --check` verificados.

## Pendências para uma retomada econômica

1. Preview de membros em Team Archive: revisão apontou requisito de `SCREENS.md` §4.4. Lista atualmente não pede/renderiza membros. Verificar se `field_list=characters` na API de teams é suficiente antes de implementar; não disparar 20 requests de detail só para decorar a lista. **Ainda não corrigido.**
2. Uma captura antiga `story_arc-list-font130.png` parece mostrar frame anterior ao scroll, embora JSON indique scrollY=0. Não concluir bug de layout. Se necessário recapturar, aguardar apresentação por ~350ms **depois** de `scrollTo`, inclusive para primeiro viewport; o harness atualmente faz essa espera extra só para bottom.
3. Teste contextual final passou somente no celular. Fonte ampliada/tablet dessas últimas correções ainda não executados. Fazer apenas checks afetados se a retomada exigir, sem recriar a auditoria inteira.
4. Matriz tem 33 relatórios de 45 e 12 telas ainda em fila. Alguns FIX documentam bugs já corrigidos, mas aguardam fechamento formal. Essa fila foi interrompida conforme a nova preferência do usuário. Consultar `review-matrix.md` para detalhe, sem alegar 45/45 aprovado.

Não introduzir dependências, conteúdo demo de produção, logos oficiais, nova arquitetura ou alterações fora do redesign. Ler AGENTS.md/CONTEXT.md/DESIGN.md/SCREENS.md antes de futuras mudanças relevantes. Manter cópia em inglês dentro do app. Trabalhar na branch atual e preservar o retorno.

## Evidências locais

`artifacts/design/rebuild/`: `verification.md`, `review-matrix.md`, `reviews/`, `native-capture-manifest.json`, `review-source-snapshot.json`, `finish-review-before-fixes.md`, `finish-verdict.md`, `documentation-report.md`, `logs/`, `captures/`.

`artifacts/` é ignorado pelo Git: evidências e APK estão no workspace local, não devem ser presumidos existentes em um clone novo. O código, assets, DESIGN e este handoff ficam salvos na branch. Nenhum segredo/configuração local deve entrar no commit.
