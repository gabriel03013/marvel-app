# Implementation design review

Reviewed 2026-09-30 against `PRODUCT.md`, `DESIGN.md`, `CONTEXT.md`, `.impeccable/android-screens.md`, `SETUP.md`, native XML resources and the Kotlin screen renderers. This is an ordinary extension of the user-pinned comic archive world. `DESIGN.md` remains the visual authority and is preserved; this review introduces no replacement identity, token system or design-document refresh.

## Bindings to the incumbent design

| Design commitment | Implementation evidence |
| --- | --- |
| Comic intelligence archive, readable above decoration | Dark archive artwork at 0.12 opacity on Home and mission pages; quiet cream discovery pages; warm-white dossier cards. `screen_dark.xml`, `screen_page.xml`, `row_character.xml`. |
| Red actions, yellow mission emphasis, blue reference information | Existing named palette in `values/colors.xml`; primary red buttons, yellow mission cards, blue character metadata. `button_red.xml`, `card_mission.xml`, `row_character.xml`. Muted ink is an implementation utility for hints and borders; the supplied muted-blue suggestion is unused. |
| Condensed display with legible interface copy | Bundled Bebas Neue titles (36sp; welcome 64sp), Oswald sections (23sp) and dossier names (22sp), Barlow Condensed shell labels, Space Grotesk body and controls. `values/themes.xml`, `screen_welcome.xml`, shell and row layouts. Font licenses remain in `licenses/fonts/`. |
| Aligned controls and generous margins | Standard pages use 24dp padding, 12dp dossier corners and 14dp mission corners. Cards remain aligned rather than rotated. This is consistent with selective collage accents and predictable interactions. |
| Distinct selected recruits and transparent report | Selected cards have a red border and explicit selected text. Reports show documented powers, dated evidence and an app-generated evaluation disclaimer. `card_selected.xml`, `ScreenRenderer.kt`, `MissionScreens.kt`. |
| Recoverable empty/error states | Reusable dossier state card, progress indicator and Retry action; missing-image dossier icon and descriptive accessibility text. Search errors/results now follow query and filter controls before explanatory metadata. HTML descriptions strip U+FFFC replacement marks. |

## Native platform behavior and review evidence

Kotlin binds native XML Views. Phone navigation contains the five supplied destinations; `layout-w600dp/activity_main.xml` changes them to a 104dp navigation rail at widths of at least 600dp. Standard content is centered with a maximum 640dp outer reading column (including padding). Welcome has its own full-width layout. System/cutout/keyboard insets, Android back handling, IME search, native share chooser and deletion dialog follow platform conventions. Buttons are at least 52dp high; shell icon actions are 48dp and navigation items at least 64dp. Images receive character descriptions; decorative artwork is excluded from accessibility. Selection includes text and accessibility state, alongside color.

Evidence includes the phone flow contact sheet and phone, large-text phone and tablet captures in `.impeccable/review/`. Corrected `search-phone-fix.png` and `error-search-tablet-fix.png` visibly confirm results and retry prominence. Build/lint passed as reported by the implementation owner after the two review fixes; this documenting pass inspected resources and captures and did not rerun Gradle. Captures establish visual behavior, not a full TalkBack audit or production-service verification.

## Raster provenance

All six supplied collage PNGs now reside in `app/src/main/res/drawable-nodpi/` to avoid Android density inflation. Pixel content is preserved. Embedded provenance identifies each as a pre-existing user-supplied project asset formerly in `drawable`; original generation prompt and author were not supplied. A six-raster scan found zero missing provenance records. This records known origin without claiming generation authorship or a newly established license. Remote character imagery comes from Comic Vine; the preserved original Marvel SVG is excluded from app branding.

## Remaining documentation and factual limits

`CONTEXT.md` still names Compose and the original drawable directory; the user-approved XML change and nodpi move are accurately recorded in `PRODUCT.md` and `SETUP.md`. Existing documents were intentionally preserved. `DESIGN.md` keeps its supplied headings and prose rather than being migrated to another schema. Its optional layered-paper and motion suggestions are not universal implemented treatments; this release emphasizes aligned dossiers and quiet reading surfaces. The surface brief's red-paper-band wording describes an intention, while welcome currently uses a red Google action.

Production Google sign-in still requires an account/device and correctly registered Firebase signing credentials. API alignment filters remain unavailable; Marvel filtering covers loaded pages, timelines are bounded, and collection caches and report snapshots are labeled. Emulator persistence and live API evidence do not establish production Firebase readiness. These are factual product limits, not a requirement to revise the preserved design authority before completing this implementation.
