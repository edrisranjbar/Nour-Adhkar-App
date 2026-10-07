# Bazaar and Myket releases

## Analysis and implementation plan

The original APK routed every user to Bazaar. Merely replacing the rating intent would leave Bazaar references in update actions, About sharing, verse/lecture/adhkar text shares and the shared image footer. A runtime store switch would also leave both stores' intent strings in the unminified release DEX. Store publication dates can differ, so a shared version feed can direct Myket users to a release that Myket has not approved yet.

The implemented plan is:

1. Add a `store` flavor dimension with `bazaar` and `myket`. Put one identically named `StoreConfig` in each flavor's source set so only the selected configuration is compiled.
2. Centralize explicit store rating and details intents with a same-store HTTPS fallback. Keep UI labels store-neutral in Persian and Arabic.
3. Route every app download link, including text and image sharing, through the selected configuration.
4. Keep Bazaar's current API/fallback; isolate Myket's published-version feed and reject unpublished, competing-store and inconsistent metadata.
5. Provide a repeatable release command, separate output directories, required FA/EN changelogs and artifact checks. Add focused tests for both variants.

## Routing audit

| Surface | Bazaar build | Myket build |
| --- | --- | --- |
| Rating prompt | `ACTION_EDIT`, `bazaar://details?id=ir.adhkar.app`, package `com.farsitel.bazaar` | `ACTION_VIEW`, `myket://comment?id=ir.adhkar.app`, package `ir.mservices.market` |
| Optional/required update | `ACTION_VIEW`, Bazaar details | `ACTION_VIEW`, `myket://details?id=ir.adhkar.app` |
| Missing/blocked store app | `https://cafebazaar.ir/app/ir.adhkar.app` | `https://myket.ir/app/ir.adhkar.app` |
| About, lecture, article, adhkar and verse sharing | Bazaar HTTPS link | Myket HTTPS link |
| Shared image footer | Bazaar link on the left | Myket link on the left |
| Update metadata | Existing API, then `version.json` on main | `version-myket.json` on main only |

Myket's official [rating intent documentation](https://myket.ir/kb/pages/user-rate-comment-submission-intent/) specifies the comment URI and `ACTION_VIEW`; its [app-page intent documentation](https://myket.ir/kb/pages/open-application-page-in-myket/) specifies the details URI for directing users to a new version. The store package is also documented in the official [Myket referrer sample](https://github.com/myketstore/referrer-sample). Rating must be tested after Myket approves the listing. Opening the store page remains the existing update flow; no background installation or store SDK is added. These changes do not guarantee store review acceptance.

## Identity and compatibility

Both release variants retain `ir.adhkar.app`, the current version name/code and the permanent release signing configuration. No Room schema, preferences, installation UUID, authentication or reminder schedules change. A correctly signed same/newer-version APK from either store updates the same installation and preserves its data; both store releases are not separate apps. Equal-version store switching should be checked on-device before relying on a store client's behavior. Google login continues to use the existing package/certificate; if a store re-signs a distributed APK, its certificate requires separate OAuth configuration and verification.

Debug variants retain the existing `.debug` suffix and therefore also replace each other. Select `bazaarDebug` or `myketDebug` explicitly in Android Studio. There is no runtime store selector and no automatic installed-market detection.

## Independent publication

Bazaar keeps the companion admin/API's existing publication workflow. Legacy metadata without a `store` or `published` field stays valid only in this flavor.

Myket's checked-in `version-myket.json` initially has `published: false`, an empty name and code zero: no Myket release is claimed. After approval, set it to the version actually available in Myket, for example:

```json
{
  "store": "myket",
  "published": true,
  "versionName": "2.4.0",
  "versionCode": 23,
  "minRequiredVersionCode": 0
}
```

Commit this metadata through develop and merge the PR into main after store approval. The app reads main, so changing only develop does not announce a release. Future Myket updates follow the same process; do not announce based on Bazaar's approval. Keep `minRequiredVersionCode` at zero for optional updates; a forced minimum must not exceed the published version code. A missing feed, offline request, unpublished entry, wrong channel or invalid version produces no update prompt. Myket requires an explicit store and publication flag. The current admin's global release editor continues to control Bazaar only; a store-specific admin editor is a separate follow-up if desired.

## Building and packaging

Only build/run when requested. No version bump or store upload is performed by these scripts. Prepare changelogs matching the current version, then:

```powershell
.\scripts\Release-Stores.ps1 -Store both -ChangelogFa .\path\changelog-fa.md -ChangelogEn .\path\changelog-en.md
```

`-Store bazaar` or `-Store myket` builds a single channel. The script resolves version values from Gradle, uses the checked-in wrapper and known workstation tool locations (overridable `JavaHome`, `SdkBuildTools`, `BundleSigner`), uses the standard user Gradle cache and enables normal release lint. It runs each flavor's debug unit tests and builds its actual signed release APK/AAB. Signing comes from the same ignored properties/environment configuration as before; BIN passwords are passed through environment variables and restored afterward.

If dependency access fails, `-GradleInitScripts` accepts existing workstation-specific repository init scripts (for example the ignored `.tooling/release-local.init.gradle` and `.tooling/release-mirror.init.gradle`). Paths are resolved before Gradle starts. This does not change the checked-in repositories or skip lint.

Outputs are separate timestamped `.d` directories under `release/<store>-<version>-vc<code>/`. Both contain APK, AAB, FA/EN changelogs, Gradle log, signing report and SHA-256 hashes. Bazaar additionally contains its Bundle Signer BIN/log; Myket gets APK/AAB, without a Bazaar-specific BIN. Existing output directories are never reused or cleaned. The script verifies package/version and APK signatures, and checks that both APK certificates match when building both stores.

## Running on the phone

`run.cmd` (or `.\scripts\Run-Phone.ps1`) builds the signed `assembleBazaarRelease` APK, then installs it with `adb install -r` as an update of `ir.adhkar.app`, so data is kept, and launches `MainActivity`. Use `-Store myket` for the Myket flavor. `-Serial` is needed only when several devices are connected. The script picks the device before building, so a missing or unauthorized phone fails fast. It uses the ignored `.tooling/release-local.init.gradle` and `.tooling/release-mirror.init.gradle` when present (`-NoInitScripts` turns them off; `-GradleInitScripts` overrides them). It checks the APK's package name and signature. It reports the installed version, the update time and the foreground activity. It never uninstalls, clears data or downgrades. If Android refuses the update, the script stops with Android's message. `-SkipLint` applies the documented local lint fallback and warns that lint did not pass. The Gradle log is in `app/build/run-phone/gradle.log`. This is for phone testing only: release folders, changelogs and the BIN still come from `Release-Stores.ps1`.

`Verify-StoreArtifact.ps1` scans APK/AAB DEX, manifests, resources and textual assets for the competing store's scheme, domain and package. It fails if the selected routing is absent. This is a packaging guard, not a replacement for device verification or store review.

Explicit Gradle tasks:

```powershell
.\gradlew.bat assembleBazaarRelease bundleBazaarRelease --project-cache-dir .gradle-card-design --no-configuration-cache
.\gradlew.bat assembleMyketRelease bundleMyketRelease --project-cache-dir .gradle-card-design --no-configuration-cache
.\gradlew.bat testBazaarDebugUnitTest testMyketDebugUnitTest --project-cache-dir .gradle-card-design --no-configuration-cache
```

APK paths are `app/build/outputs/apk/<store>/release/app-<store>-release.apk`; AAB paths are `app/build/outputs/bundle/<store>Release/app-<store>-release.aab`. Generic `assembleRelease` now targets both flavors; use explicit tasks when only one is intended.

## Verification status and release checks

Implementation-time checks cover diff whitespace, PowerShell parsing, source separation, and synthetic artifact-scanner acceptance/rejection. Run `scripts/Test-StoreIsolation.ps1` for these source/fixture checks without Gradle; the release script also runs it before building. Unit tests are authored for intent contracts, same-store fallback, blocked/unavailable handlers, independent publication, required updates, invalid metadata and shared verse text.

Android compilation, unit-test execution, actual artifact inspection and device verification remain pending until a build is requested. Before distribution, build both flavors with tests/lint; inspect each artifact; install each as an update using the permanent signing identity; verify rating, optional/required update and shares in Persian/Arabic, both with the selected store installed and absent. Check Myket rating against an approved listing. Do not uninstall or clear app data to switch variants. Upload each artifact only to its corresponding store, then update that store's publication metadata after approval.
