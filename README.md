# Focus-On

An Android app that silences the apps that drain your time (Instagram, YouTube, anything you pick) for as long as you decide. Black and champagne-gold design, bold statements about focus, and a 7-day view of where your hours actually went.

## What it does

| Screen | What you see |
|---|---|
| **Onboarding** (first launch) | Three manifesto pages, then "Grant the keys": the permissions, each with Allow → gold tick |
| **Home** | A rotating statement, "The last 7 days" (time given to your 5 most-used apps), your apps ranked by daily use, duration presets (25 min – 3 h) or Custom (5 min – 12 h 55 min), **Begin focus** |
| **In focus** | A gold ring counting down, rotating statements, the silenced apps greyed out. No stop button. |
| **Silenced app opened** | Pure black, one bold statement, a small gold timer at the bottom. Back goes to the home screen. |
| **Silenced website opened** | Same black screen, labelled e.g. "INSTAGRAM.COM · SILENCED", when a silenced app's website is opened in a browser (needs Website blocking on). Underneath, Focus-On taps the browser's own Home / New tab button so the tab goes to its normal start page (search box, shortcuts); Back returns there, so the browser stays usable even if the silenced site was the last open tab. Browsers without such a button get a blank tab instead. |

Debug builds (run from Android Studio or `gradlew installDebug`) also show a **1 min · test** duration and an **End session** link so you don't have to wait while testing. Release builds don't.

## Build and run

Needs JDK 17+ (21 works), the Android SDK (platform 35, build-tools 34), and a phone with **USB debugging** on.

From PowerShell in this folder:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
.\gradlew.bat installDebug        # build and install on the connected phone
.\gradlew.bat testDebugUnitTest   # unit tests
```

`local.properties` points Gradle at the SDK (`sdk.dir=D\:\\Android\\Sdk`). If Gradle times out downloading, also set `$env:GRADLE_OPTS = "-Djava.net.preferIPv4Stack=true"`.

Or open the folder in Android Studio and press Run.

## Release build (for the phone you actually use)

Debug builds are **debuggable**: any computer you have ever authorised for USB debugging can read and change the app's data (`adb shell run-as`) or attach a debugger. Use a signed release build for daily use, and turn USB debugging off when you're not developing.

1. Create a signing key once, outside the repo folder (pick your own passwords; `keytool` ships with the JDK):
   ```powershell
   & "C:\Program Files\Java\jdk-21.0.10\bin\keytool.exe" -genkeypair -v -keystore D:\Android\keys\focus-on.jks -alias focus-on -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Create `keystore.properties` next to `settings.gradle.kts` (git-ignored, never commit it):
   ```properties
   storeFile=D:/Android/keys/focus-on.jks
   storePassword=your-store-password
   keyAlias=focus-on
   keyPassword=your-key-password
   ```
3. Uninstall the debug build from the phone (different signing key), then:
   ```powershell
   .\gradlew.bat installRelease
   ```

Back up the `.jks` file and its passwords somewhere safe: without them you can't install updates over the existing app.

## Security

- **No internet permission.** The app cannot send anything off the phone.
- **Website blocking is tightly scoped.** The accessibility service only receives events from the 18 browsers listed in `res/xml/web_guard_config.xml` (a unit test keeps that list in sync with the code), does nothing outside a focus session, reads only the address bar, and stores nothing.
- **Website blocking never stays on.** UPI and banking apps (Google Pay, PhonePe, Paytm, BHIM, bank apps) refuse payments while any non-system accessibility service is switched on: an anti-fraud rule against malware. So Focus-On's service switches itself off when a session ends, is ended early, or the phone restarts with no session, and **the moment a payment or bank app comes to the front** (any app that handles `upi://` links, plus a list of known wallet and bank apps; WhatsApp, Amazon and other everyday apps are left out). Nothing for the user to do before paying. Android lets only the user switch an accessibility service back on, so after the payment a notification offers to do it in one tap. A payment app you chose to silence doesn't count (it would be a way round website blocking). If an unrecognised payment app still refuses, the session screen has "Pause website blocking". Silenced apps stay silenced.
- **Calls always get through.** The phone app, in-call screens and emergency apps can never be silenced, and nothing is blocked while any call (phone, WhatsApp, Meet, …) is ringing or in progress.
- **Nothing exported except the launcher screen.** The service, lock screen and boot receiver can't be started or messaged by other apps.
- **Private data stays private:** session times and your app selection live in app-private storage, excluded from cloud backup and device-to-device transfer.
- **Task hijacking blocked** (`taskAffinity=""` on the main screen).
- **Build integrity:** the Gradle wrapper is Gradle 8.11.1's own, and a GitHub Action (`.github/workflows/gradle-wrapper-validation.yml`) rejects any push or pull request that swaps `gradle-wrapper.jar` for a non-genuine one.
- **Release builds** are not debuggable and are shrunk/obfuscated with R8.

## Permissions

| Permission | Why | Required? |
|---|---|---|
| Usage access | See which app is open, and rank apps by use | Yes |
| Display over other apps | Show the black screen on top of a silenced app | Yes |
| Website blocking (Accessibility) | Lock instagram.com, youtube.com, … in browsers. Offered on the session screen ("Block websites too"), on only for that session: it switches itself off when the session ends and whenever a payment app opens. If the switch is greyed out: Settings → Apps → Focus-On → ⋮ → Allow restricted settings | Optional, per session |
| Notifications | Countdown while you focus, "Focus complete" at the end | Recommended |
| Unrestricted battery | Stops Android closing the blocker mid-session | Recommended |

The app has no internet permission; nothing leaves the phone.

**Xiaomi / Redmi / POCO, Oppo, Vivo, Realme, OnePlus, Motorola:** also allow autostart / unrestricted background activity for Focus-On in **Settings → Apps**, or the blocker may stop after a while.

## How it works

```
MainActivity → MainScreen        picks Onboarding / Home / In-focus screen
SessionStore                     "silence these apps from START until END" (SharedPreferences)
Usage.kt                         last 7 days of foreground time per app (UsageStatsManager)
FocusService (foreground)        every 500 ms asks UsageStatsManager which app is in front
    └─ silenced app in front  →  opens BlockActivity on top of it
BlockActivity                    black screen, statement, timer; Back = home screen
BootReceiver                     restarts FocusService after a reboot if a session is still running
WebGuardService (accessibility)  during a session, reads the browser's address bar; silenced app's website → BlockActivity
```

| File | What it does |
|---|---|
| `ui/MainScreen.kt` | App state and routing between the three screens |
| `ui/OnboardingScreen.kt` | Manifesto pages and permissions |
| `ui/HomeScreen.kt` | Statement, 7-day insight, ranked app list, durations, custom-duration and confirm dialogs |
| `ui/ActiveSessionScreen.kt` | Countdown ring and silenced apps |
| `ui/Quotes.kt` | The statements. Edit freely; keep each under ~85 characters |
| `ui/components/Components.kt` | Gold button, chips, gold tick, rotating statement, section headers |
| `ui/theme/Theme.kt` | Colours (`Lux`), fonts (`LuxFonts`), text styles (`LuxType`) |
| `block/BlockActivity.kt` | The black lock screen |
| `service/FocusService.kt` | Foreground-app detection loop and notifications |
| `service/WebGuardService.kt`, `service/Browsers.kt` | Website blocking: browser address-bar reading |
| `data/Websites.kt` | Which websites belong to which app (Instagram → instagram.com, …) and safe address matching |
| `data/` | Session state, installed apps, usage stats |

## Fonts

Playfair Display (statements, titles, numbers) and Inter (labels, body) are bundled in `res/font` so the app looks the same on every phone. Both are under the SIL Open Font License; the licence texts ship in `assets/licenses`.

## Known limits

- **You can still cheat** by changing the phone's clock, force-stopping the app, turning off Usage access or uninstalling it in Settings. Making it strict (Device Admin to block uninstalling, locking Settings during a session) is the next step.
- **Website blocking covers 18 browsers** (Chrome, Brave, Edge, Firefox, Samsung Internet, Opera, DuckDuckGo, …) and the websites of 25 popular apps. Not covered: in-app browsers (a link opened inside Gmail or WhatsApp), and browsers not on the list unless their address bar can be found. Silence those browsers as apps if needed.
- **Split-screen and picture-in-picture** aren't specially handled.
- Built for **sideloading onto your own phone**. Publishing on Google Play would need policy declarations for the foreground service and the battery-optimization request.
- The internal package ID is still `com.focuslock.app` (changing it would install as a separate app).

## Tech

Kotlin · Jetpack Compose (Material 3) · minSdk 26 (Android 8.0) · targetSdk 35 · AGP 8.7.3 · Gradle 8.11.1 · tested on a Motorola Edge 60 Fusion (Android 17)
