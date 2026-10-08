# S.H.I.E.L.D. Archives — Complete Screen Map

This is the official screen and navigation specification for the app. All visible copy must be in English.

## Navigation model

### Public flow

```text
SplashScreen
  -> WelcomeScreen
      -> SignInScreen
          -> HomeScreen
```

If Firebase finds a valid authenticated session, skip Welcome and Sign In and open Home.

### Authenticated flow

Bottom navigation:

```text
Home | Search | Recruit | Archives | Collection
```

Secondary screens are opened from these five destinations and from contextual actions.

## Screen inventory

## 1. Launch and authentication

### 1.1 SplashScreen

Route: `splash`

Purpose:

- Show the S.H.I.E.L.D. Archives identity while the app initializes.
- Restore Firebase authentication state.
- Prepare fonts, theme, and initial data.

Content:

- App mark or wordmark.
- Short tagline: `SEARCH. DISCOVER. RECRUIT.`
- Subtle halftone or dossier motion.

States:

- Initial loading.
- Authenticated session found.
- No authenticated session.
- Initialization failure with retry.

### 1.2 WelcomeScreen

Route: `welcome`

Purpose:

- Explain the product in one glance before login.

Content:

- Collage hero using the archive background.
- `S.H.I.E.L.D. ARCHIVES` title.
- Copy: `Explore the archive. Build your team.`
- Primary action: `Continue with Google`.
- Additional actions: `Sign in with email` and `Create an account` (explicitly requested by the user).
- Small privacy/data note.

### 1.3 SignInScreen / SignInBottomSheet

Route: `sign-in`

Purpose:

- Complete Google authentication.

Content:

- Google sign-in action.
- Loading state while authentication is running.
- Error message and retry action.
- Cancel/back action where applicable.

Email/password authentication was explicitly requested later and is now included:

- `email-sign-in`: email/password, validation, loading, retry, Forgot Password and signup navigation.
- `sign-up`: agent name, email, password and confirmation; real Firebase account creation, profile update and First Run Profile.
- `password-reset`: email and Firebase reset link request; neutral confirmation and return to sign-in.
- All three use native XML, support Back, retain form input in memory on retry/rotation and clear passwords after successful authentication or leaving the form.

### 1.4 FirstRunProfileScreen

Route: `first-run-profile`

Purpose:

- Confirm the account profile data received by Firebase.

Content:

- Profile photo from `photoUrl`.
- Display name.
- Email.
- Continue action: `Enter the Archives`.

This can be skipped if the first authenticated Home experience already displays the profile clearly.

## 2. Main navigation

### 2.1 HomeScreen

Route: `home`

Purpose:

- Main dashboard and entry point for the product.

Content:

- User greeting and profile avatar.
- Featured mission card: `Start a Mission`.
- Quick action: `Search Characters`.
- Quick action: `Explore Archives`.
- Recent characters.
- Favorite characters preview.
- Latest saved team preview.
- Decorative collage composition.

Interactions:

- Open ProfileScreen.
- Open MissionBriefingScreen.
- Open SearchScreen.
- Open ArchivesScreen.
- Open CharacterDetailScreen.
- Open CollectionScreen.

States:

- Loading user data.
- Empty first-use dashboard.
- Populated dashboard.
- Partial data when Firestore is unavailable.

### 2.2 Global SearchOverlay

Route: `global-search`

Purpose:

- Fast access to character search from any authenticated screen.

Content:

- Search field with clear action.
- Recent searches.
- Suggested searches.
- Character result rows.

### 2.3 ProfileScreen

Route: `profile`

Purpose:

- Show the authenticated user profile and account actions.

Content:

- Google photo.
- Display name.
- Email.
- Saved favorites count.
- Saved teams count.
- Completed missions count.
- `Edit profile` action for changing the display name.
- `Settings` action.
- `Sign out` action.

### 2.4 EditProfileScreen

Route: `edit-profile`

Content:

- Edit the account display name.
- Keep email and provider photo read-only.
- Validate and save the name to Firebase Authentication and the Firestore profile.
- Show saving, success, and retryable error states.
- `Cancel` returns to the profile.

### 2.5 SettingsScreen

Route: `settings`

Content:

- Account section.
- App language display: English.
- Visual style/about section.
- API attribution section.
- Comic Vine base URL and the API paths used by the archive.
- Privacy note.
- `Sign out`.
- `Delete local session` or account action only if implemented safely.

Do not add account deletion unless the complete re-authentication flow is implemented.

## 3. Character discovery

### 3.1 SearchScreen

Route: `search`

Purpose:

- Mandatory core feature for searching characters.

Content:

- Search input: `Search characters...`.
- Search submit action.
- Filter chips: `All`, `Heroes`, `Villains`, `Marvel`.
- Sort options where supported.
- Result count.
- Character cards with image, name, real name, publisher, and short description.
- Pagination or `Load more`.

States:

- Initial suggestions.
- Typing.
- Loading.
- Results.
- Empty results: `No agents found`.
- API error: `Signal lost` and `Retry`.
- Offline or timeout state.

### 3.2 CharacterDetailScreen / HeroDiaryScreen

Route: `character/{characterId}`

Purpose:

- Full character exploration experience.

Content:

- Back action.
- Hero image.
- Character name and real name.
- Publisher and origin.
- Favorite toggle.
- `Add to Team` action.
- `Compare` action.
- Overview / deck.
- First appearance.
- Issue appearance count.
- Powers list.
- Teams list.
- Allies and enemies when returned by the API.
- Related story arcs.
- Related issues.
- Related volumes.

States:

- Loading skeleton.
- Complete details.
- Missing sections hidden or replaced with `No data available`.
- Image error placeholder.
- API error with retry.

### 3.3 CharacterImageViewer

Route: `character/{characterId}/image`

Optional full-screen image viewer from the character detail screen.

### 3.4 CharacterSelectScreen

Route: `character-select`

Purpose:

- Reusable selection screen for Recruit and Compare.

Content:

- Search field.
- Character results.
- Selected character indicator.
- Confirm action.
- Selection limit when called by Recruit.

## 4. Archives

### 4.1 ArchivesScreen

Route: `archives`

Content:

- Category cards:
  - Characters
  - Powers
  - Teams
  - Story Arcs
  - Issues
  - Volumes
  - Publishers
  - Locations
- Featured archive item.
- Recently viewed items.

### 4.2 PowerArchiveScreen

Route: `archives/powers`

Content:

- Search powers.
- Power cards or list.
- Character count where available.
- Open PowerDetailScreen.

### 4.3 PowerDetailScreen

Route: `power/{powerId}`

Content:

- Power name.
- Description if available.
- Characters with this power.
- Add a character to a team.

### 4.4 TeamArchiveScreen

Route: `archives/teams`

Content:

- Search teams.
- Team cards.
- Member preview.
- First appearance and issue count where available.
- Open TeamDetailScreen.

### 4.5 TeamDetailScreen

Route: `team/{teamId}`

Content:

- Team name and image.
- Description/history.
- Members.
- Aliases.
- First appearance.
- Related issues and volumes.
- `Use this team as inspiration` action for Recruit.

### 4.6 StoryArcArchiveScreen

Route: `archives/story-arcs`

Content:

- Search story arcs.
- Arc cards with image, name, and description preview.
- Open StoryArcDetailScreen.

### 4.7 StoryArcDetailScreen

Route: `story-arc/{arcId}`

Content:

- Arc image and title.
- Description.
- Issues in the arc.
- Related characters.
- Related teams.

### 4.8 IssueArchiveScreen

Route: `archives/issues`

Content:

- Search and pagination.
- Issue covers.
- Issue number.
- Volume/title.
- Release date when available.
- Open IssueDetailScreen.

### 4.9 IssueDetailScreen

Route: `issue/{issueId}`

Content:

- Cover.
- Issue name and number.
- Volume.
- Release date.
- Description.
- Characters appearing.
- Writers and artists when available.

### 4.10 VolumeArchiveScreen and VolumeDetailScreen

Routes: `archives/volumes`, `volume/{volumeId}`

Use when the Comic Vine response provides enough volume data. Show title, publisher, description, issue count, image, and related issues.

### 4.11 PublisherArchiveScreen and PublisherDetailScreen

Routes: `archives/publishers`, `publisher/{publisherId}`

Use for Marvel filtering and publisher exploration. Show publisher identity and related characters, volumes, and issues when available.

### 4.12 LocationArchiveScreen and LocationDetailScreen

Routes: `archives/locations`, `location/{locationId}`

Optional archive area. Use only if API data is sufficiently rich.

## 5. Recruitment and missions

### 5.1 RecruitScreen

Route: `recruit`

Purpose:

- Primary differentiator of the app.

Content:

- Mission cards.
- `Start custom mission`.
- Active team preview.
- Saved teams preview.

Mission examples:

- `Cosmic Threat`
- `Technology Crisis`
- `Mutant Incident`
- `City Under Attack`
- `Build Your Own Mission`

### 5.2 MissionBriefingScreen

Route: `mission/{missionTemplateId}/briefing`

Content:

- Mission title.
- Threat description.
- Recommended team size.
- Suggested power/category.
- `Begin Recruitment` action.

### 5.3 MissionCharacterPickerScreen

Route: `mission/{missionTemplateId}/select`

Content:

- Mission requirements.
- Selected team members.
- Search characters.
- Suggested characters from API data.
- Remove member action.
- `Continue` action.

States:

- Empty team.
- Partially filled team.
- Team complete.
- Missing/invalid character data.

### 5.4 TeamAssemblyScreen

Route: `mission/{missionTemplateId}/assembly`

Content:

- Selected character collage.
- Team name input.
- Member cards.
- App-generated evaluation criteria.
- `Generate Report` action.

### 5.5 TeamReportScreen

Route: `team-report/{teamId}`

Content:

- Team name.
- Mission name.
- Team member collage.
- Powers and roles.
- API-based facts used in the report.
- App-generated compatibility/evaluation score clearly labeled.
- `Save Team`.
- `Share Report` only if implemented safely.
- `Start Another Mission`.

### 5.6 SavedTeamDetailScreen

Route: `collection/team/{teamId}`

Content:

- Saved team report.
- Members.
- Mission.
- Created date.
- Delete action with confirmation.

### 5.7 MissionHistoryScreen

Route: `collection/missions`

Content:

- Completed missions.
- Team used.
- Completion date.
- Open saved report.

## 6. Comparison and timeline

### 6.1 CompareSetupScreen

Route: `compare`

Content:

- Select Character A.
- Select Character B.
- Swap action.
- `Compare Profiles` action.

### 6.2 CompareResultScreen

Route: `compare/{characterAId}/{characterBId}`

Content:

- Side-by-side images and names.
- Real name.
- Publisher.
- First appearance.
- Issue appearances.
- Powers.
- Teams.
- Similarities and differences.
- Any score labeled `App-generated comparison`.

### 6.3 TimelineScreen

Route: `timeline`

Content:

- Character selector.
- Timeline of first appearance, issues, volumes, arcs, and teams.
- Filters by type.
- Expandable timeline entries.

States:

- No character selected.
- Loading timeline.
- Partial timeline.
- No chronological data available.

## 7. Collection

### 7.1 CollectionScreen

Route: `collection`

Tabs or segmented controls:

- Cards (booster inventory and collected character cards)
- Favorites
- Saved Teams
- Mission History
- Recently Viewed

### 7.2 FavoritesScreen

Route: `collection/favorites`

Content:

- Favorite character grid/list.
- Remove favorite.
- Open character detail.
- Empty state: `Your archive is empty`.

### 7.3 SavedTeamsScreen

Route: `collection/teams`

Content:

- Saved team cards.
- Open report.
- Delete team.
- Empty state.

### 7.4 RecentlyViewedScreen

Route: `collection/recent`

Optional local-only list of recently opened characters and archive items.

### 7.5 BoosterShopScreen

Route: `booster-shop`

Purpose:

- Add unlimited boosters to the signed-in user's inventory.
- Explain that the five pack themes are app-created and character records come from Comic Vine.

Pack themes:

- Street Signal
- Cosmic Signal
- Mutant Legacy
- Tech Protocol
- Legendary Archive

Content:

- Original sealed-pack artwork for each theme.
- Owned quantity and `Add booster · Free` action.
- `Open` action for packs in the user's inventory.
- Link to Cards in My Collection.

### 7.6 BoosterOpeningScreen and BoosterRevealScreen

Routes: `booster-opening/{type}` and `booster-reveal/{type}`

Content:

- Sealed and opened original artwork for every pack theme.
- Each opening retrieves five distinct character records from Comic Vine and atomically consumes one owned booster while adding cards to Firestore.
- Reveal one real character card at a time, with a next-card action and a link to its dossier.
- Repeated cards increment copies and app-generated collection level; never describe that level as an official statistic.

States:

- Loading and retry while Comic Vine or Firestore is unavailable.
- A pack cannot be opened without inventory or a complete live character pull.
- Firestore persists card records even if the user leaves the reveal flow.

## 8. Shared UI states and components

Every API-backed screen must account for:

- Loading skeleton.
- Empty response.
- Network/API error.
- Retry.
- Missing image.
- Missing text.
- Pagination/loading more.
- Pull-to-refresh where useful.

Reusable components:

- `ArchiveTopBar`
- `BottomNavigationBar`
- `CollageCard`
- `CharacterCard`
- `CharacterGrid`
- `DossierLabel`
- `ComicBurst`
- `PaperScrap`
- `HalftoneOverlay`
- `SearchField`
- `FilterChipRow`
- `LoadingSkeleton`
- `ErrorState`
- `EmptyState`
- `FavoriteButton`
- `ProfileAvatar`
- `MissionCard`
- `TeamMemberCard`
- `StatChip`
- `SectionHeader`
- `ConfirmDeleteDialog`

## 9. Persistence requirements

The app must create these paths automatically through Firestore writes:

```text
users/{uid}
users/{uid}/favorites/{characterId}
users/{uid}/teams/{teamId}
users/{uid}/missions/{missionId}
users/{uid}/boosters/{boosterType}
users/{uid}/collected_characters/{characterId}
```

No seed data is required in the Firebase Console. The app must gracefully handle a new user with no documents.

## 10. Definition of done

The screen map is complete when:

- Every primary destination is reachable.
- Back navigation works from every secondary screen.
- Login and sign-out work.
- Search can reach a character detail screen.
- Favorites persist between sessions.
- Users can add unlimited boosters, open one, reveal its five live Marvel character cards, and find saved copies and app-generated collection levels in Cards.
- A user can complete and save a recruitment mission.
- API errors and missing data do not crash the app.
- Every screen follows the English copy and visual rules in `DESIGN.md`.
