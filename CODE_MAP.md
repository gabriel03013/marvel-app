# Code map

## Android UI

`app/src/main/java/com/example/marvel/ui/` contains one screen renderer per screen. Shared view creation, navigation, typography, and reusable controls live in `ScreenRenderer.kt`.

| Screen or flow | File |
| --- | --- |
| Home | `HomeScreen.kt` |
| Profile and profile editing | `ProfileScreen.kt`, `ProfileEditorController.kt` |
| Shared account actions such as sign-out | `AccountActions.kt` |
| Settings and API endpoints | `SettingsScreen.kt` |
| Collection tabs | `CollectionScreen.kt`, `CardCollectionScreen.kt` |
| Booster shop, opening, and reveal | `BoosterShopScreen.kt`, `BoosterOpeningScreen.kt`, `BoosterRevealScreen.kt` |
| Character search | `CharacterSearchScreen.kt` |
| Archive categories and records | `ArchivesScreen.kt`, `ArchiveListScreen.kt`, `CharacterDetailScreen.kt`, `RelatedRecordsScreen.kt` |
| Character image viewer | `CharacterImageScreen.kt` |
| Compare profiles | `CompareScreens.kt` |
| Character timeline | `TimelineScreen.kt` |
| Recruitment | `RecruitScreen.kt`, `MissionBriefingScreen.kt`, `CharacterPickerScreen.kt`, `TeamAssemblyScreen.kt`, `TeamSimulationScreen.kt`, `TeamReportScreen.kt` |
| Investigation | `InvestigationSetupScreen.kt`, `InvestigationBoardScreen.kt` |
| Authentication forms | `AuthScreens.kt` |

`MainActivity.kt` owns the Android activity, app shell, system navigation, and platform sign-in intents. `ScreenRenderer.kt` chooses the screen renderer for the current route.

## Screen state and actions

`ArchiveViewModel.kt` owns shared screen state, route history, and Firebase auth-state observation. Feature actions are grouped in separate files as extensions on `ArchiveViewModel`:

| Feature | File |
| --- | --- |
| Authentication and sign-out | `ui/AuthViewModel.kt` |
| Profile, collection persistence, favorites, and local history | `ui/AccountViewModel.kt` |
| Comic Vine lists, details, and timeline | `ui/ArchiveDataViewModel.kt` |
| Booster inventory and reveal | `ui/BoosterViewModel.kt` |
| Recruitment, evaluation, and reports | `ui/MissionViewModel.kt` |

## Data and persistence

- `data/Models.kt` defines archive, collection, booster, and mission models.
- `data/ComicVineRepository.kt` loads and caches Comic Vine records.
- `data/ComicVineEndpoints.kt` is the source for endpoint URLs displayed in Settings and used by the API client.
- `data/UserRepository.kt` owns Firebase profile and collection reads/writes.
- `firebase/firestore.rules` defines per-user Firestore access.

Instrumented tests are in `app/src/androidTest`; local JVM tests are in `app/src/test`.
