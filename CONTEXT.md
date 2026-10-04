# S.H.I.E.L.D. Archives — Project Context

## Product

S.H.I.E.L.D. Archives is an English-language Android mobile app for exploring Marvel-related comic data and building teams for missions. It uses the Comic Vine API as its primary data source.

Core promise:

> Search. Discover. Recruit.

The app should feel like a modern intelligence archive combined with a comic-book collage: useful for searching characters, but memorable because users can explore hero histories and assemble teams.

## Main experiences

1. **Home** — mission highlight, recent/favorite characters, and shortcuts.
2. **Search** — search characters by name and open a complete profile.
3. **Hero Diary** — character identity, description, powers, teams, first appearance, issues, arcs, and related data.
4. **Archives** — browse characters, powers, teams, story arcs, issues, and publishers.
5. **Recruit** — choose characters for a mission and generate a team report.
6. **Compare** — compare two characters using available API data.
7. **Timeline** — organize appearances, issues, and story arcs chronologically when data is available.
8. **My Collection** — local favorites, saved teams, viewed characters, and completed missions.

## API

Base URL: `https://comicvine.gamespot.com/api/`

Relevant endpoints:

- `/characters`
- `/issues`
- `/volumes`
- `/teams`
- `/story_arcs`
- `/publishers`
- `/movies`
- `/powers`
- `/locations`

Requests must include `api_key` and `format=json`. Include a descriptive `User-Agent` when required by the API. Marvel filtering may require checking the returned publisher data because direct publisher filtering is not always supported. The Marvel publisher ID from the assignment is `4010-31`.

Do not invent API facts. If the app calculates a team score or comparison score, label it as an app-generated evaluation based on visible criteria, not as an official Marvel power ranking.

## Language

All user-facing app text must be in English. Character names, publisher names, team names, powers, and descriptions should remain as returned by the API unless there is a clear product reason to transform them.

## Current stack

- Android application
- Kotlin
- Native Android XML layouts and Kotlin Views
- Existing Android dependencies; no design rebuild dependency added
- Package: `com.example.marvel`

## Current asset folder

New original collage artwork is in `app/src/main/res/drawable-nodpi/`:

- `collage_heroes.png`
- `collage_cosmic.png`
- `collage_paper_label.png`

These generated decorative assets have real alpha and embedded provenance. Character images remain Comic Vine data. The six former assets were rejected by the user and replaced in the full design rebuild. See `artifacts/design/rebuild/asset-provenance.json`.

## Evaluation goals

The school project is evaluated on usability, visual design, API usage, creativity, functionality, and presentation. A working search and character profile are mandatory. The recruitment flow is the main differentiator.
