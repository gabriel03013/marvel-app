# Running S.H.I.E.L.D. Archives

The UI uses Kotlin and Android XML layouts with View Binding. Compose is not used, as requested. `SCREENS.md` remains the product backlog and `DESIGN.md` remains the visual authority.

## Local API configuration

Keep the Comic Vine key in the untracked `local.properties`:

```properties
COMIC_VINE_API_KEY=your_private_key
```

The build also accepts the `COMIC_VINE_API_KEY` environment variable. Rebuild after changing the key. Never commit `local.properties`; `.gitignore` excludes it. API requests use HTTPS, a descriptive User-Agent, `api_key`, `format=json`, bounded pages and encoded parameters. Search resources are singular (`character`, `power`, `team`, etc.), while list endpoints are plural.

## Firebase Google login

1. Enable Google in Firebase Authentication → Sign-in method.
2. Register the Android application `com.example.marvel` and its debug/release signing fingerprints. Obtain local debug fingerprints with `./gradlew :app:signingReport`.
3. Download the matching `google-services.json` into `app/`. The generated `default_web_client_id` must be the web OAuth client ID.
The current local debug certificate SHA-1 is `BE:1F:84:65:7A:28:35:BC:72:5F:11:61:B3:2D:97:8C:BF:BF:0D:FA`. The current SHA-256 is `99:BF:5A:1A:0F:C7:86:3F:DF:72:E3:1B:D6:CA:F0:98:BF:8D:DC:D1:F2:D1:1E:BB:3E:C0:04:1F:E6:43:B8:6B`. Re-run `signingReport` if the debug keystore changes.

4. Create a Cloud Firestore database. Review and publish `firebase/firestore.rules` so each user can access only their own documents.

The project originally had only the debug SHA-256 registered and a web OAuth client in the local JSON. The debug SHA-1 above is now registered through the authenticated Firebase CLI, and `app/google-services.json` has been refreshed and checked to contain both Android (type 1, matching that SHA-1) and web (type 3) OAuth clients. Google and Email/Password were confirmed enabled through the project admin configuration. A real Google login still needs to be checked on a device with a Google account using the rebuilt app.

## Email/password login and signup

Enable **Authentication → Sign-in method → Email/Password → Email/Password** in the same Firebase project (passwordless email-link sign-in is not needed). Without this setting, Firebase rejects signup/login even though the native forms are available. See [Firebase password authentication](https://firebase.google.com/docs/auth/android/password-auth).

Welcome also offers **Continue as guest**. Enable **Authentication → Sign-in method → Anonymous** in the same Firebase project to use it. Guest profiles and saved collection data are stored under that anonymous Firebase UID. Signing out ends access to that guest archive; the app asks for confirmation first. If Anonymous is disabled, Firebase returns an unavailable-method message and the user can choose Google or email instead.

Welcome offers Google, email sign-in and account creation. Signup collects agent name, email, password and confirmation, creates the Firebase account, updates its name and persists the profile through the existing user repository. Existing users sign in to their own collection. Forgot Password sends Firebase’s reset email and displays a neutral confirmation. Inputs survive rotation/retries in memory; passwords are never saved in preferences or instance-state bundles and are cleared after authentication or leaving the form.

Google errors remain distinct from password errors. For Google certificate configuration, follow [Firebase Google sign-in setup](https://firebase.google.com/docs/auth/android/google-signin). Provider status was verified separately through the authenticated Identity Toolkit admin API; the JSON alone does not establish it. When signing with a different debug/release certificate, register its fingerprints and refresh the JSON again.

The app writes profiles and these collections automatically; no console seed data is needed:

```text
users/{uid}
users/{uid}/favorites/{characterId}
users/{uid}/teams/{teamId}
users/{uid}/missions/{teamId}
```

Teams and completed missions are saved atomically. Deleting a saved team preserves its mission-history snapshot. Firestore cache and pending writes are explicitly labeled. Stored snapshots retain identity and report evidence rather than huge description/issue payloads. Recent characters, recent searches and unfinished mission drafts are device-local and scoped by Firebase user ID.

## Build

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The existing Android 36.1 SDK is retained; AndroidX versions were selected to compile against it. Images are in `drawable-nodpi` to avoid density-driven texture inflation. The original unsupported Marvel SVG is preserved in `assets/originals/` and is not used as branding.

## Integration verification

`EmailAuthFlowTest` checks signup, invalid email, password confirmation, rotation, Firestore profile persistence, sign-out, incorrect-password recovery, password reset, successful email login, leaving a pending login with Back and retrying without an unwanted late session against local emulators. `AnonymousAuthFlowTest` checks guest entry and profile persistence. Run instrumented tests with the local Auth and Firestore emulators started and an Android emulator/device connected, for example `./gradlew :app:connectedDebugAndroidTest`.

`ArchiveFlowTest` uses real Comic Vine records and **local Firebase emulators**. It signs into an isolated emulator user; it does not create a production account. Start Auth on port 9099 and Firestore on 8080, then run:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.marvel.ArchiveFlowTest
```

To start the isolated services from this folder:

```sh
npx --yes firebase-tools emulators:start --project demo-shield-archives --only auth,firestore
```

Run this test only against an Android emulator whose host is `10.0.2.2`. The debug network-security override allows cleartext only for local emulator hosts; release traffic remains HTTPS. Review captures are saved under the app’s external-files `review` directory.

## Data limitations

- Heroes/Villains filtering is unavailable because Comic Vine has no reliable alignment field. The UI states this explicitly.
- Marvel filtering checks publisher ID 31 on loaded search pages. It does not claim the API’s overall result count is Marvel-only.
- Name sorting applies to browsing; searches use API relevance order.
- Timelines retrieve at most 20 distinct linked issues and sort documented cover dates. Undated arcs, volumes and teams are shown separately.
- Empty sections and missing images are recoverable states, not invented data.
- Mission roles and scores are labeled app inferences, with visible criteria. Saved reports are dated snapshots, not current or official Marvel statistics.
