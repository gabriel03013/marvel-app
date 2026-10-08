---
name: S.H.I.E.L.D. Archives
description: 'Cut, Collect, Recruit: a native comic paper archive.'
colors:
  carbon-ink: '#171819'
  action-vermilion: '#C93422'
  deep-vermilion: '#8C241A'
  reading-paper: '#F7F2E6'
  warm-white: '#FFFCF5'
  progress-yellow: '#EFD76E'
  archive-cobalt: '#274AC4'
  body-ink: '#343431'
  muted-ink: '#625F57'
  paper-rule: '#D4CBBA'
  chip-outline: '#8E887C'
typography:
  display:
    fontFamily: Bebas Neue
    fontSize: 48sp
    fontWeight: 400
  form-title:
    fontFamily: Bebas Neue
    fontSize: 44sp
    fontWeight: 400
  compact-title:
    fontFamily: Bebas Neue
    fontSize: 36sp
    fontWeight: 400
  record-title:
    fontFamily: Oswald
    fontSize: 24sp
    fontWeight: 400
  action-title:
    fontFamily: Oswald
    fontSize: 22sp
    fontWeight: 400
  category-title:
    fontFamily: Oswald
    fontSize: 28sp
    fontWeight: 400
  body:
    fontFamily: Space Grotesk
    fontSize: 16sp
    fontWeight: 300
  secondary:
    fontFamily: Space Grotesk
    fontSize: 14sp
    fontWeight: 300
  note:
    fontFamily: Space Grotesk
    fontSize: 13sp
    fontWeight: 300
  navigation:
    fontFamily: Barlow Condensed
    fontSize: 12sp
    fontWeight: 700
  top-bar:
    fontFamily: Barlow Condensed
    fontSize: 22sp
    fontWeight: 700
  evaluation:
    fontFamily: Bebas Neue
    fontSize: 64sp
    fontWeight: 400
rounded:
  selection: 4dp
  control: 8dp
  paper-image: 12dp
  chip: 24dp
spacing:
  space-4: 4dp
  space-6: 6dp
  space-8: 8dp
  space-10: 10dp
  space-12: 12dp
  space-16: 16dp
  space-18: 18dp
  space-20: 20dp
  space-24: 24dp
components:
  button-primary:
    backgroundColor: '{colors.action-vermilion}'
    textColor: '{colors.warm-white}'
    typography: '{typography.body}'
    rounded: '{rounded.control}'
    padding: 8dp 20dp
    width: match_parent
  button-outline:
    textColor: '{colors.carbon-ink}'
    typography: '{typography.body}'
    rounded: '{rounded.control}'
    padding: 8dp 20dp
    width: match_parent
  button-text:
    textColor: '{colors.action-vermilion}'
    typography: '{typography.body}'
  field:
    backgroundColor: '{colors.warm-white}'
    textColor: '{colors.carbon-ink}'
    typography: '{typography.body}'
    rounded: '{rounded.control}'
    padding: 12dp 16dp
  chip:
    backgroundColor: '{colors.warm-white}'
    textColor: '{colors.carbon-ink}'
    typography: '{typography.secondary}'
    rounded: '{rounded.chip}'
    padding: 10dp 18dp
  chip-selected:
    backgroundColor: '{colors.progress-yellow}'
    textColor: '{colors.carbon-ink}'
    typography: '{typography.secondary}'
    rounded: '{rounded.chip}'
    padding: 10dp 18dp
  navigation-bar:
    backgroundColor: '{colors.carbon-ink}'
    textColor: '{colors.reading-paper}'
    typography: '{typography.navigation}'
  navigation-selected:
    backgroundColor: '{colors.action-vermilion}'
    textColor: '{colors.reading-paper}'
    typography: '{typography.navigation}'
    rounded: '{rounded.control}'
    padding: 8dp 0dp
  paper-surface:
    backgroundColor: '{colors.warm-white}'
    rounded: '{rounded.control}'
  mission-cover:
    backgroundColor: '{colors.warm-white}'
    textColor: '{colors.carbon-ink}'
    typography: '{typography.compact-title}'
    padding: '{spacing.space-20}'
    width: match_parent
  record-row:
    textColor: '{colors.carbon-ink}'
    typography: '{typography.record-title}'
    padding: 12dp 0dp
    width: match_parent
  record-row-selected:
    backgroundColor: '{colors.warm-white}'
    textColor: '{colors.carbon-ink}'
    typography: '{typography.record-title}'
    padding: 12dp 8dp
    width: match_parent
  native-title:
    textColor: '{colors.carbon-ink}'
    typography: '{typography.display}'
---

# Design System: S.H.I.E.L.D. Archives

## Overview

**Creative North Star: "Cut, Collect, Recruit"**

Cut, Collect, Recruit is a usable comic fanzine: condensed ink headlines, original torn illustrations, vermilion actions and cobalt annotations on warm reading paper. Collage is present throughout the app, while opaque reading surfaces keep records, forms and controls clear.

The material comes from three original PNG cutouts with real alpha. Comic Vine imagery identifies characters and archive records; generated illustration decorates the archive. All titles, labels and product copy remain live English native text. The launcher carries an original open-book mark with a bookmark and cobalt annotation.

**Key Characteristics:**

- Intense original comic collage with proportional cutouts.
- Condensed native headlines beside readable record imagery.
- Quiet opaque paper for reading, forms and reports.
- Vermilion actions, cobalt metadata and explicit selected states.
- Native Android navigation, reflow and recoverable data states.

The YAML frontmatter is normative. Measurements use Android `dp` and `sp`; width values preserve native layout semantics. The [JSON sidecar](/home/gabriel/Documents/codes/marvel/.impeccable/design.json) records Android-only geometry, state behavior, accessibility, motion and source provenance.

## Colors

Warm paper and carbon ink carry the reading surface; vermilion, cobalt and yellow give actions and annotations distinct roles.

### Primary

- **Action Vermilion** (`action-vermilion`): filled primary actions, selected navigation, field focus, selection outlines and account text actions.
- **Deep Vermilion** (`deep-vermilion`): error feedback, selected-record copy and the dismissible message strip.

### Secondary

- **Archive Cobalt** (`archive-cobalt`): provider metadata, selected archive category accents and the launcher annotation.

### Tertiary

- **Progress Yellow** (`progress-yellow`): checked chips and the current recruitment step.

### Neutral

- **Carbon Ink** (`carbon-ink`): headlines, primary record text, system navigation ground and launcher background. The native `archive_black` and `ink_black` resources share this value.
- **Reading Paper** (`reading-paper`): the full-page ground and navigation labels.
- **Warm White** (`warm-white`): forms, fields, mission covers, state panels and primary-action text.
- **Body Ink** (`body-ink`): long reading copy and result decks.
- **Muted Ink** (`muted-ink`): field hints, notes, secondary copy and default field outlines.
- **Paper Rule** (`paper-rule`): quiet button borders, disabled field outlines, image backing and skeleton blocks. The `stage_muted` resource is an alias.
- **Chip Outline** (`chip-outline`): the unselected chip's thin outline.

**The Paper Ground Rule.** Keep record copy and controls on opaque paper; use collage as a proportional illustration beside or above the task.

The light paper palette stays stable in night mode. Source: [colors.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/values/colors.xml), [themes.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/values/themes.xml), and [chip_background.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/drawable/chip_background.xml).

## Typography

**Display Font:** Bebas Neue, bundled Regular face.
**Record / Section Font:** Oswald, bundled variable Regular default.
**Body Font:** Space Grotesk, bundled variable Light default inherited by the theme.
**Navigation Font:** Barlow Condensed, bundled Bold face.

Compressed headlines give the archive its comic-cover voice. Record and section titles stay narrower than body copy, allowing real names and descriptions to wrap naturally.

### Hierarchy

| Token | Use |
| --- | --- |
| `display` | Standard page titles, Welcome and Splash lettering. |
| `form-title` | Account-form titles beside a small collage cutout. |
| `compact-title` | Compact detail, list, selection and submitted-search headers; mission cover titles. |
| `record-title` | Character names, sections and Welcome supporting title. |
| `action-title` | Reusable action-row titles. |
| `category-title` | Archive category actions. |
| `body` | Reading copy, field text, button labels and data values. |
| `secondary` | Record metadata, excerpts, data labels, chips and progress steps. |
| `note` | Evidence notes and profile email. |
| `navigation` | Main destination labels. |
| `top-bar` | The persistent archive or report label. |
| `evaluation` | The explicitly app-generated report score. |

Weights in the frontmatter describe the bundled font defaults where the app does not request a different style. Space Grotesk defaults to Light; Barlow Condensed is a Bold file. Oswald and Space Grotesk have weight axes, but the UI declares no `fontVariationSettings`. The profile name requests native `textStyle="bold"`; the sidecar preserves that request without treating it as a separate custom font face.

Body text adds (4dp) line spacing. Notes, excerpts and data rows add (3dp); record metadata and Welcome supporting copy add (2dp). The app specifies extra native spacing rather than a fixed line height. Most text retains Android font padding; the Welcome wordmark/title/supporting title and auth title explicitly remove it. No custom letter spacing is declared.

**The Live Lettering Rule.** Keep titles, labels, facts and actions as native text. Artwork carries no interface copy.

Sources: [themes.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/values/themes.xml), [font resources](/home/gabriel/Documents/codes/marvel/app/src/main/res/font), [row_character.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/layout/row_character.xml), and the native font metadata recorded in the sidecar.

## Layout

Pages are vertical native scroll layouts with the `space-20` gutter. Content uses wrap-content heights, so headings, body copy and actions can reflow with font scale. Standard section titles declare (24dp) top and (10dp) bottom margins; component-specific spacing is retained rather than forced into a new universal grid.

At a screen width of (600dp), primary navigation becomes a left rail (104dp). The normal page container is centered and bounded to (640dp), including its padding, or the available width after the rail. Welcome uses a centered (520dp) content column at that threshold. Account content is always bounded to `min(520dp, screenWidthDp − 40dp)` inside the padded scroll layout. These are native width rules, not CSS breakpoints.

The top bar has a (56dp) minimum height and (48dp) Back/search actions. Bottom destination items have a (64dp) minimum height, equally weighted phone widths, (24dp) icons and native ripple feedback. Public authentication screens show their own cover or Back action; authenticated secondary screens keep Android Back and the shared bar.

Standard title art occupies (112dp × 112dp). Compact titles use (88dp × 80dp) art and the `compact-title` type token. The Welcome hero is (230dp) tall; mission-cover art is (176dp) tall; briefing art is (156dp) tall. Each image is fit proportionally rather than stretched. Compact headers apply to image, detail, archive list, related records, selection, global search and submitted Search.

System bars, display cutouts and the keyboard feed the root inset padding. Root bottom padding uses the larger of the system-bar and keyboard insets. Same-screen updates preserve scroll, focused field and cursor; route changes place focus on the screen host.

Sources: [screen_page.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/layout/screen_page.xml), [activity_main.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/layout/activity_main.xml), [tablet shell](/home/gabriel/Documents/codes/marvel/app/src/main/res/layout-w600dp/activity_main.xml), [ScreenRenderer.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/ScreenRenderer.kt), [AuthScreens.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/AuthScreens.kt), and [MainActivity.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/MainActivity.kt).

## Elevation & Depth

Paper depth is printed into the overlapping original art. Reading panels are opaque native fills with no app-defined drop-shadow vocabulary. Buttons inherit Android `Widget.Material.Button` elevation and its state animator; the app does not replace those defaults with a custom shadow token. Ripple feedback is native: primary buttons declare translucent white, outline buttons translucent dark ink, and rows/navigation use the platform selectable-item foreground.

**The Material Depth Rule.** Let overlapping artwork provide the paper depth. Keep reading containers flat and retain native widget feedback.

A changed route receives one (180ms) opacity reveal from (0.88) to (1), using native `DecelerateInterpolator`. It runs only when `ValueAnimator.areAnimatorsEnabled()` is true. Same-screen data updates do not replay it. Loading skeletons are static; progress indicators use native indeterminate behavior. No continuous collage animation is declared.

Sources: [CollageArtView.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/CollageArtView.kt), [button_red.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/drawable/button_red.xml), [button_outline.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/drawable/button_outline.xml), and [MainActivity.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/MainActivity.kt).

## Shapes

Controls, current navigation backgrounds and Kotlin reading surfaces use the `control` radius. Chips use the larger `chip` radius. Comparison image paper uses `paper-image`. A selected record layers a `selection`-radius outline over the `control`-radius paper surface; preserve both native layers rather than inventing a unified card shape.

Default field outlines and quiet button borders are (1dp). Focused fields and selected records use a (2dp) vermilion outline. Native controls and text are not rotated. Organic edges belong to the original transparent illustrations and label asset.

The launcher is an original open-book silhouette on carbon ink, with a vermilion bookmark and cobalt annotation. Adaptive and round variants share the native foreground/background; monochrome retains the book silhouette. Legacy launcher resources resolve to the same artwork layers.

Sources: [field_background.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/drawable/field_background.xml), [card_paper.xml](/home/gabriel/Documents/codes/marvel/app/src/main/res/drawable/card_paper.xml), [ScreenRenderer.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/ScreenRenderer.kt), and [launcher foreground](/home/gabriel/Documents/codes/marvel/app/src/main/res/drawable/ic_launcher_foreground.xml).

## Components

### Buttons

Confident native actions with readable, mixed-case labels. Primary and outline buttons share the frontmatter's geometry and a (56dp) minimum height; their height wraps content. The outline variant has a transparent fill and paper-rule border. Account text actions use a native selectable background and (48dp) minimum height. Back uses ink text, its native arrow and a narrower content-width layout.

Shared generated buttons lower alpha to (0.55) when disabled. Auth buttons disable while busy and show `Please wait…`; their opacity is inherited from the native widget rather than the shared helper. Platform ripple, focus and disabled feedback remain native. Do not add web hover variants.

### Inputs / Fields

Quiet warm-white fields carry ink text and muted hints, visible native labels, the declared padding and a (56dp) minimum height. The focused outline thickens and turns vermilion. Disabled fields switch to reading paper with a paper-rule outline. Search pairs a weighted field with a (48dp) Clear action, accepts the Search IME action and exposes a separate primary Search button.

Auth fields use appropriate email/password/name input types, labels linked with `labelFor`, native autofill hints and a (48dp) Show passwords checkbox. Busy state disables the form. Auth feedback is a polite live region on paper with deep-vermilion errors. Input and visibility state are retained in memory during retry/rotation; successful auth or leaving the form clears passwords. Multiline mission briefing has a three-line minimum.

### Chips

Native RadioButtons present filter and collection choices. Unselected chips use warm white and the chip outline; checked chips use progress yellow. They have a (48dp) minimum height, horizontal scroll when needed and (8dp) trailing spacing. Disabled choices use (0.55) alpha and an explicit unavailable accessibility description. Selection is a native checked state, with readable content descriptions. After layout, the chip strip centers the selected choice when space allows and clamps to its valid scroll range, keeping the active tab visible on entry and rerender.

### Navigation

The charcoal navigation ground carries paper labels and icons. A vermilion rounded item marks the active destination; native `isSelected` and the spoken `selected` suffix repeat the visual state. Decorative icons are excluded from duplicate accessibility announcements. Phone navigation distributes five destinations horizontally; the width-qualified shell places them in the left rail. Contextual routes retain their parent destination's selection.

### Cards / Containers

Paper panels use an opaque fill. Mission covers are editorial: original cosmic art, a condensed title, live objective, and a distinct vermilion action. Other missions and saved teams use the quieter reusable action row with an arrow and supporting text. Action rows have a (64dp) minimum height and wrap their copy.

Record rows put a (96dp × 128dp) provider image beside the record title, true metadata and an optional three-line excerpt. Images use `centerCrop`; detail and comparison images use `centerInside`. A selected row shows a vermilion outline and readable removal/confirmation label. Labels and provider names wrap; only the intentional excerpt is ellipsized.

### Native Title / Collage

Every main page title pairs live ink lettering with proportional original art. `CollageArtView` uses `FIT_CENTER` and excludes decorative artwork from accessibility. Welcome overlays a live wordmark on the original label image (200dp × 67dp); auth titles pair with a (112dp × 96dp) decorative cutout. The three shipping PNGs preserve alpha and embedded generation provenance. See [asset-provenance.json](/home/gabriel/Documents/codes/marvel/artifacts/design/rebuild/asset-provenance.json).

### Dossier / Report Data

Data rows align label and value using native layout weights (0.42 / 0.58), with wrap-content heights and `Not documented` for blank values. Report scores carry the visible `App-generated evaluation` label; suggested roles carry `app inference`. Recruitment uses three equal-width native progress labels, with yellow for the current step and a spoken step/current-state description.

The shared `hasRecruitmentDraft` guard includes a roster, a nonblank team name, or a nonblank custom mission objective. Home and Recruit expose the resumable draft. Choosing another mission asks `Replace your active team?` with `Keep current team` and `Start new mission`; confirmed replacement clears the roster, team name, custom objective and report.

### Loading / Empty / Error

Remote records use a shared paper state panel with a decorative art fragment, live heading/copy, native spinner, static skeleton and an optional Retry action. Existing rows remain visible during loading or partial failure. Empty guidance reflects the filter that actually ran: Marvel-specific recovery applies only to filtered non-selection character results; selectors and other archives use generic available recovery. Pagination shows a disabled loading action while another page is pending. Timeline refresh retains previous dated records and discloses any reused records when individual requests fail. Related lists retain their own heading, records and pagination position through nested Android Back and Activity recreation.

Collection states distinguish loading, sync interruption, pending local changes and cached data. Cached or failed retrieval is not presented as a confirmed empty collection or fresh count. Dismissible global messages are polite live regions and remain visible for (5000ms) unless dismissed first.

All remote images start with the dossier placeholder. A blank URL is missing; a failed request adds `Image unavailable` to the image's description. The full-image viewer separately shows visible `Loading image…`, missing-image copy or connection-error copy, and exposes `Retry image` only on failure. Success hides status and Retry and retains the uncropped provider image in a (420dp) frame; other large record frames are (280dp). List/detail image slots keep the placeholder and accessible failure description; they do not expose an individual image-retry button.

Sources: [ScreenRenderer.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/ScreenRenderer.kt), [DiscoveryScreens.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/DiscoveryScreens.kt), [MissionScreens.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/MissionScreens.kt), [ArchiveViewModel.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/ArchiveViewModel.kt), and [ArchiveImages.kt](/home/gabriel/Documents/codes/marvel/app/src/main/java/com/example/marvel/ui/ArchiveImages.kt).

## Booster Collection Extension

The booster shop and reveal use the same warm-paper ground, opaque reading panels, Bebas Neue page titles, Oswald record names, Space Grotesk body copy, vermilion primary actions, and cobalt metadata. Keep pack art proportional in `FIT_CENTER` ImageViews; generated closed/open pack art is decoration, while the pack name, card facts, quantity, level, and actions remain native text. Each theme has its own original closed and opened image pair in `drawable-nodpi`. Generation and crop provenance is recorded in [booster-artwork-provenance.json](/home/gabriel/Documents/codes/marvel/artifacts/booster-artwork-provenance.json).

Booster tiles use quiet paper surfaces with the sealed pack above its live title, description, and quantity. The reveal keeps one Comic Vine character portrait and its documented metadata as the reading focus, with opened-pack art as a smaller cue. Loading, empty, and retry messages use the existing opaque paper state panel. Cards in My Collection retain the existing collection chips and provider-backed character rows.

Repeated-copy progression is labeled `Collection level · app-generated` and must not be presented as a canonical power statistic. Pack theme names describe the app's visual editions; only Comic Vine records identify the pulled characters.

## Do's and Don'ts

### Do:

- **Do** fit original collage proportionally and preserve its alpha.
- **Do** place Comic Vine images and facts at the center of record content.
- **Do** use the declared native dp/sp tokens and allow text and controls to grow.
- **Do** retain cache and partial results while showing clear sync, error and retry copy.
- **Do** show selection through readable labels, native selected state and the established outline.
- **Do** use the shared recruitment draft guard before replacing a mission.
- **Do** label evaluations and suggested roles as app-generated.

### Don't:

- **Don't** put generated illustration in place of a provider character image or fabricate archive facts.
- **Don't** use official Marvel logos as created branding.
- **Don't** stretch paper scraps, paint text into raster art or add checkerboard backings.
- **Don't** rotate native text, controls or touch targets.
- **Don't** cover reading areas with texture or turn every result into a decorated tile.
- **Don't** add continuous animation, web hover states or a synthetic CSS shadow system.
- **Don't** present cached counts as confirmed fresh data or app evaluations as official statistics.
