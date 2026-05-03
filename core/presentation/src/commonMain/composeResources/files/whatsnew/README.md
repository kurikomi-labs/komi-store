# What's-new entries

One JSON file per release, named after the Android `versionCode` it ships with.
Loaded at runtime by `WhatsNewLoaderImpl` and rendered in the in-app sheet and
history screen.

## File layout

```
files/whatsnew/<versionCode>.json                  # default (English)
files/whatsnew/<locale>/<versionCode>.json         # localized translation
```

Compose Multiplatform Resources does not extend its `values-<qualifier>` /
`drawable-<qualifier>` resolution to the raw `files` directory, so locale
variants live as plain subfolders and `WhatsNewLoaderImpl` performs the lookup
manually. For each requested `versionCode`, the loader tries:

1. `files/whatsnew/<full-locale>/<versionCode>.json` (e.g. `zh-CN`, `pt-BR`)
2. `files/whatsnew/<primary-locale>/<versionCode>.json` (e.g. `zh`, `pt`)
3. `files/whatsnew/<versionCode>.json` (English fallback)

The current locale is supplied by `LocalizationManager.getCurrentLanguageCode()`
/ `getPrimaryLanguageCode()`, so the resolution honours the in-app language
override exposed in Tweaks, not just the OS locale.

Existing locale folders: `ar`, `bn`, `es`, `fr`, `hi`, `it`, `ja`, `ko`, `pl`,
`ru`, `tr`, `zh-CN`.

## Schema

```json
{
  "versionCode": 16,
  "versionName": "1.8.1",
  "releaseDate": "2026-05-03",
  "showAsSheet": true,
  "sections": [
    {
      "type": "NEW",
      "bullets": [
        "Short, user-facing sentence under ~90 characters."
      ]
    },
    {
      "type": "IMPROVED",
      "bullets": []
    },
    {
      "type": "FIXED",
      "bullets": []
    }
  ]
}
```

`type` accepts `NEW`, `IMPROVED`, `FIXED`, or `HEADS_UP`.

`showAsSheet = false` keeps the sheet silent on first launch (the loader still
records the version as seen). Use it for bug-fix-only patches that have nothing
worth interrupting the user for — silent patches preserve credibility for the
next real release.

## Per-release author workflow

1. Add `core/presentation/src/commonMain/composeResources/files/whatsnew/<versionCode>.json`.
2. Append the new `versionCode` to `KnownWhatsNewVersionCodes.ALL` in
   `composeApp/src/commonMain/kotlin/zed/rainxch/githubstore/app/whatsnew/WhatsNewLoaderImpl.kt`.
3. Keep bullets short, factual, and editorial — no marketing voice.

## Translator workflow

1. Copy `files/whatsnew/<versionCode>.json` to
   `files/whatsnew/<your-locale>/<versionCode>.json`. Use a BCP-47 code such as
   `de`, `pt-BR`, or `zh-CN`.
2. Translate the bullet text only. Leave `versionCode`, `versionName`,
   `releaseDate`, `showAsSheet`, and the section `type` values untouched.
3. Open a PR — translations land independently of the release that introduced
   the entry, and English remains the fallback for any version the locale has
   not translated yet.
