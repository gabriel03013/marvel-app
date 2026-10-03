# Agent Instructions

## Before changing code

1. Read `CONTEXT.md` and `DESIGN.md`.
2. Read `SCREENS.md` when working on navigation, UI, Firebase persistence, API data, or any user flow.
3. Inspect the existing implementation before introducing new architecture.
4. Preserve user changes and avoid unrelated refactors.
5. Keep the app buildable after each meaningful change.

## Product implementation directive

Implement the complete screen map in `SCREENS.md`. Treat it as the authoritative product backlog for the Android app. Do not stop at the initial placeholder screen. Build the flows end-to-end, using real Comic Vine data and Firebase persistence where specified.

Every screen must:

- Use English user-facing copy.
- Have a clear loading, empty, error, and retry behavior when it depends on remote data.
- Support Android back navigation.
- Follow the visual system in `DESIGN.md`.
- Use real API/Firebase data instead of hardcoded demo content, except for mission templates and intentional UI placeholders.

If a screen is marked optional, implement it after all mandatory screens and only when its API data is sufficient. Do not let optional archive areas delay Login, Home, Search, Character Detail, Collection, Recruit, and Team Report.

## Implementation priorities

Work in this order unless the task explicitly says otherwise:

1. App shell and navigation.
2. Firebase configuration validation, Google login, and auth state.
3. Firestore user profile and persistence repositories.
4. Comic Vine API client and data models.
5. Character search with loading, empty, and error states.
6. Character detail / Hero Diary.
7. Favorites and Collection.
8. Recruit flow, mission selection, team assembly, and team report.
9. Archives, powers, teams, arcs, issues, comparison, and timeline.
10. Visual polish, motion, accessibility, and presentation details.

## Coding rules

- Use Kotlin and Jetpack Compose idiomatically.
- Keep API, state, and UI responsibilities separate.
- Prefer small composable functions and reusable visual components.
- Do not hardcode API results as the primary implementation.
- Do not expose the API key in committed source code. Use the existing local configuration approach or add a safe placeholder/configuration path.
- Add clear loading, error, retry, and empty-result states.
- Handle missing images, missing descriptions, missing powers, and incomplete API records gracefully.
- Use stable keys in lazy lists.
- Avoid adding dependencies unless necessary and compatible with the current Gradle setup.
- Do not use official Marvel logos or copyrighted promotional artwork as app-created branding. The visual language may be comic-inspired, while character imagery should come from the API or approved assets.

## Data rules

- Include `api_key` and `format=json` in API requests.
- Encode search/filter parameters safely.
- Respect pagination and avoid loading unnecessary large result sets.
- Cache or retain local favorites where useful, but do not pretend cached data is always fresh.
- Never present calculated scores as official Marvel statistics.

## Visual rules

- Follow `DESIGN.md`.
- Keep content readable above decoration.
- Use the provided collage assets selectively; do not cover every surface with texture.
- Prefer reusable modifiers/components for collage cards, paper scraps, comic accents, dossier labels, and halftone overlays.
- Use content descriptions for meaningful images and decorative descriptions only when appropriate.

## Verification

Before finishing a coding task:

- Run the most relevant available checks.
- If the Gradle wrapper is blocked by the environment, report that clearly instead of claiming a successful build.
- Check for obvious Compose/resource errors and invalid Android resource names.
- Summarize changed files, verification performed, and any remaining limitation.
