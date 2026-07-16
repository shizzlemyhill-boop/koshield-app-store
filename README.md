# KoShield App Store

A private, self-hosted Android app store for KoShield. It fetches a catalog of
apps from a JSON file **you** host, shows them in a clean list, and installs or
updates them directly on your tablet. To add or change apps later, you just edit
the JSON — no need to rebuild the store itself.

- **Install APKs directly** (not just links)
- **Remote catalog** you control — edit `catalog.json`, the store updates instantly
- **Detects installed apps** and shows Install / Update / Open automatically
- **Settings screen** to point the store at any catalog URL

---

## How it works

```
  Your tablet                         Anything you can host a file on
 ┌───────────────────┐               ┌──────────────────────────────┐
 │ KoShield App Store │ ── fetch ──▶  │  catalog.json  (list of apps) │
 │       (this APK)   │               │  your-app.apk  (the APKs)     │
 └───────────────────┘               │  icon.png      (app icons)    │
        │  install                    └──────────────────────────────┘
        ▼
   The app is installed on the tablet
```

You host two kinds of files somewhere public (GitHub works great and is free):
1. **`catalog.json`** — the list of apps (name, version, description, icon URL, APK URL).
2. **The APK files and icons** the catalog points to.

The store app reads `catalog.json`, shows the apps, and installs the APKs on tap.

---

## Part 1 — Build the store APK

You don't need Android Studio or any developer tools. The included GitHub Action
builds the APK for you in the cloud.

1. Create a **free GitHub account** if you don't have one.
2. Create a **new repository** (e.g. `koshield-app-store`) and upload this whole
   folder to it (drag-and-drop works on github.com → *Add file* → *Upload files*).
3. Go to the **Actions** tab of your repo. If prompted, click *"I understand my
   workflows, enable them."*
4. The build starts automatically on upload. When it finishes (green check),
   click the run, scroll to **Artifacts**, and download
   **`KoShield-App-Store-debug`**. Inside is **`app-debug.apk`** — that's your store.

> Prefer Android Studio? Just open this folder in Android Studio and click
> **Run** (or *Build → Build APK(s)*). The APK lands in
> `app/build/outputs/apk/debug/app-debug.apk`.

The APK is *debug-signed*, which is completely fine for sideloading onto your own
devices. (See "Going to production" below if you ever distribute more widely.)

---

## Part 2 — Sideload the store onto your tablet

1. Copy `app-debug.apk` to the tablet (USB, email, cloud drive, etc.).
2. Open it with the tablet's Files app.
3. Android will ask to **allow installing unknown apps** — allow it for the app
   you opened the file with. Then tap **Install**.
4. Open **KoShield App Store**. The first time you install an app *through* the
   store, Android will again ask to allow the store to install unknown apps —
   allow it, then tap Install again.

---

## Part 3 — Point the store at your catalog

Set your catalog location in one of two ways:

- **Quick (no rebuild):** Open the store → menu (⋮) → **Settings** → paste your
  **Catalog URL** (the raw link to your `catalog.json`).
- **Baked in as the default:** Edit `DEFAULT_CATALOG_URL` near the top of
  `app/src/main/java/com/koshield/appstore/CatalogRepository.kt` before building,
  so the store ships already pointing at your catalog.

If you host on GitHub, the raw URL looks like:
```
https://raw.githubusercontent.com/YOUR-USERNAME/koshield-catalog/main/catalog.json
```

---

## Part 4 — Add or update apps later

Edit `catalog.json` and upload your APK. That's the whole workflow — the store
picks up changes the next time it's opened or refreshed (pull down to refresh).

Each app entry:

| Field         | Required | Notes                                                              |
|---------------|----------|-------------------------------------------------------------------|
| `id`          | ✅       | The app's real package name (e.g. `com.koshield.filter`). Used to detect installs. |
| `name`        | ✅       | Display name.                                                      |
| `apkUrl`      | ✅       | Direct download link to the `.apk`.                               |
| `version`     | ▫️       | Shown to the user, e.g. `2.1.0`.                                  |
| `versionCode` | ▫️       | Integer. If higher than the installed one, the store shows **Update**. |
| `description` | ▫️       | Short blurb.                                                       |
| `iconUrl`     | ▫️       | PNG/JPG icon link.                                                 |
| `category`    | ▫️       | e.g. `Utilities`.                                                  |
| `size`        | ▫️       | Free text, e.g. `8.4 MB`.                                          |

A ready-to-edit example lives in [`catalog/catalog.json`](catalog/catalog.json).

**To publish an update to an app:** upload the new APK, then bump its `version`
and `versionCode` in `catalog.json`. Users will see an **Update** button.

---

## Notes & tips

- **Package name = `id`.** For Update/Open detection to work, `id` must match the
  installed app's package name exactly.
- **Cleartext (http) is allowed** so you can use a plain local server if you want.
  For anything on the internet, prefer `https`.
- **Icons** are optional; without one, a neutral placeholder is shown.
- The store keeps downloaded APKs in its private folder and overwrites them on
  each install, so it won't pile up storage.

### Going to production (optional)
The debug APK is signed with Android's shared debug key. If you later want a
stable, self-signed release build (so updates to the *store itself* install
cleanly over old versions), create a keystore and add a `signingConfig` +
`assembleRelease` step. Ask and this can be wired up.

---

## Project layout

```
KoShieldAppStore/
├─ app/
│  ├─ build.gradle                 app module config + dependencies
│  └─ src/main/
│     ├─ AndroidManifest.xml       permissions, activities, FileProvider
│     ├─ java/com/koshield/appstore/
│     │  ├─ MainActivity.kt        app list, refresh, routing
│     │  ├─ AppAdapter.kt          list rows + Install/Update/Open logic
│     │  ├─ AppItem.kt             catalog model + JSON parsing
│     │  ├─ CatalogRepository.kt   fetches catalog.json  ← default URL here
│     │  ├─ ApkInstaller.kt        download + install engine
│     │  ├─ IconLoader.kt          lightweight icon loader
│     │  └─ SettingsActivity.kt    catalog URL setting
│     └─ res/                      layouts, theme, icons, strings
├─ catalog/catalog.json            sample catalog (host your own copy)
├─ .github/workflows/build.yml     cloud APK build
├─ settings.gradle / build.gradle  project config
└─ README.md
```
