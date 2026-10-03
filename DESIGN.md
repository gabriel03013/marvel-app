# S.H.I.E.L.D. Archives — Comic Paste-up

## Direction

A living comic archive, assembled by hand. Every screen belongs to a comic zine made of clipped panels, cream stock, scarlet paper, tape, registration marks and printed halftone. The user explicitly wants **intense collage throughout the app**. Decoration is visible on every surface; opaque paper protects reading and controls stay aligned.

Preserve Search. Discover. Recruit, real Comic Vine records and images, Firebase authentication and persistence, English copy, Android Back and honest app-generated evaluations. Retain the existing Kotlin and native XML implementation.

## Materials and composition

- Ink is the stage; paper is the reading surface. Welcome, Home and mission flow use charcoal archive stages. Search, archives, collection and account pages use warm paper stock.
- An editorial title slab, visible asset composition and torn paper edges identify each page. Different functions need different composition, rather than the same stack of outlined buttons.
- Character images are real API photographs in paper frames. Tape and registration details stay at the perimeter, away from faces and factual text.
- Result rows are cream slips with image, headline, metadata and optional summary. Selection adds the visible Selected label and an outline.
- Categories are folder covers, teams are report slips, missions have a dominant briefing and action strip.
- Rotate decoration by at most 6 degrees. Never rotate controls, text, result lists or touch targets.
- Use original transparent atlases at draw time: select individual scraps, tape and bursts. Never display a whole sprite sheet as a card.
- No official Marvel wordmark or promotional art as created branding.

## Color roles

| Android token | Value | Role |
|---|---|---|
| archive_black | #151719 | Stage and navigation |
| ink_black | #171715 | Primary ink on paper |
| archive_red | #B62436 | Primary action, warm-white text |
| deep_red | #7D1728 | Red annotations |
| paper_cream | #EFE4CE | Paper page stock |
| warm_white | #FFF8EA | Opaque reading/form surfaces |
| comic_yellow | #F2CE59 | Mission paper, active navigation, dark ink |
| archive_blue | #245D7B | Reference paper and metadata |
| ink_gray | #383832 | Body ink |
| muted_ink | #625D52 | Quiet metadata/placeholders |
| paper_rule | #C4B79E | Rules and outlines |
| stage_muted | #D0C5B1 | Supporting ink on dark ground |

Color never carries state alone. Body/placeholder contrast >=4.5:1, large type >=3:1. Texture fades or stops before reading. Keep the authored paper-and-ink palette independent of wallpaper and do not invert cream raster images in night mode.

## Typography

Bundled licensed fonts only. Bebas Neue is the cover voice, Oswald names sections and records, Space Grotesk carries reading/actions. Barlow Condensed is reserved for compact wordmarks/navigation.

| Role | Size | Face |
|---|---|---|
| Cover | 48sp | Bebas Neue |
| Page headline | 40sp | Bebas Neue |
| Section / record | 22–24sp | Oswald |
| Body / field / action | 16sp, 4dp extra leading | Space Grotesk |
| Metadata | 13–14sp, 3dp extra leading | Space Grotesk |
| Navigation | 12sp, wraps if needed | Barlow Condensed |

No arbitrary technical eyebrows, fake dossier IDs or invented statistics. Sequence numbers are allowed when they show real mission progress. Titles normally take two or three lines; long API names wrap fully. Essential copy uses sp, wrap_content and minimum heights, also at font scale 1.3.

## Geometry

4dp grid: 8, 12, 16, 20, 24, 32, 40. Phone gutter 20dp. Expanded reading width up to 640dp; auth width up to 520dp. Sections have 28–32dp above and 12dp below; related copy 8–12dp; rows 12dp apart.

Targets >=48dp; primary buttons >=56dp. Paper has torn contours; controls have 8dp radius, visible focus and ripple. Layering comes from overlapping real scraps and different stocks, with no hard block shadows. Five labeled bottom destinations on phones; existing rail from 600dp. Filled selection indicator and accessibility selection state. Respect status, cutout, navigation and keyboard insets. Every page and form scrolls.

## Components

CollageArtView draws cached original assets. CollageSurface supplies title paper, state slips, rows and framed images. XML owns structure; ScreenRenderer binds data/actions. Reuse title panels, taped section headings, dossier rows, category covers, report slips, labeled data pairs, real progress strips, image frames and state panels. Secondary links are quiet ink/paper rows, primary actions filled red. Dense related records remain skimmable.

## Screen contract

| Surface | Composition and action |
|---|---|
| Splash | Own wordmark, central comic collage, honest restoring indicator |
| Welcome / Google | Comic cover with scarlet title strip, cream Archives lettering, scraps and burst; Google then email then signup |
| Email / signup / reset | Paper form on collage stock; compact cover, opaque labeled fields, visible feedback and Back |
| First run / Profile | Agent identity slip with photo/data, grouped saved counts; enter archive or settings |
| Home | Greeting, identity strip, featured mission cover, paired discovery actions, real recent/favorite records and last saved report |
| Search / global search | Blue-and-cream editorial title, large query/action, recent/suggested slips; actual results dominate after search |
| Detail | Name and framed API image, identity metadata, primary Recruit; overview and related readable dossier sections |
| Image | Quiet ink frame, full uncropped real image and attribution |
| Recruit | Distinct mission covers; continue an existing roster above choices |
| Briefing | Mission title, actual step strip, objective paper and suggested abilities; Begin Recruitment |
| Picker | Roster/capacity and labeled selected slips, search/favorites; Continue to Assembly |
| Assembly | Named roster and removal controls, visible criteria; Generate Report |
| Report / saved | Team identity, printed score sheet explicitly labeled app evaluation, evidence/member dossiers; Save or Share |
| Archives | Folder covers for real API categories, then comparison/timeline tools; no made-up counts |
| Lists / related | Compact category title, query where applicable, states and paginated slips |
| Compare / result | Two labeled identity columns and consistently grouped facts; unknown stays unknown |
| Timeline | Dated issue sections, first-appearance marker, expandable summaries; undated relations separate |
| Collection and subtabs | Scrollable labeled tabs, real count and record/report slips; empty actions lead to Search or Recruit |
| Settings | Quiet paper document with readable account, language, attribution and privacy |

## States and accessibility

Loading has labeled progress and dossier skeletons. Empty has a comic mark, explanation and useful next action. Error has opaque paper, clear recovery and Retry; existing data remains visible during partial failure. Missing image uses our dossier icon with an honest description. Decorative assets are excluded from accessibility. Missing facts say Not documented; no inferred alignment or official-looking strength statistics. Selection/disabled states and keyboard focus remain clear. Long names/columns wrap, cache/offline notices stay honest, auth retries retain input.

## Motion and verification

Signature: a paper cover settles on route change in 180ms, with no replay on data updates. Use native ripple for selection. Respect ValueAnimator.areAnimatorsEnabled and transition instantly under Remove animations. No continuous background movement, marquee, hover or JavaScript libraries.

Review each route with a background design agent, then combine material corrections. Native screenshots must come from the installed app, not web mockups or old captures. Verify phone, expanded widths and font scale 1.3 when available. Run assemble, relevant tests and lint; report unavailable checks.

## Skills synthesis

Impeccable supplies native craft and bounded critique. GPT Taste supplies editorial composition, varied scale and purposeful movement adapted to Android. Mobile UI and mobile-design supply touch, keyboard, navigation and user-state principles. Style skills contribute magazine, zine, image-collection and canvas lessons without replacing the brand. Full review: artifacts/design/skill-review.md.
