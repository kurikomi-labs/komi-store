# Homebrew Cask (reference copy)

The Cask in this directory is a **reference/seed copy**. The live, user-facing
Cask lives in [`OpenHub-Store/homebrew-tap`](https://github.com/OpenHub-Store/homebrew-tap)
and is auto-updated on each release by
[`.github/workflows/homebrew-tap-publish.yml`](../../.github/workflows/homebrew-tap-publish.yml).

Users install with:

```bash
brew tap OpenHub-Store/tap
brew install --cask github-store
xattr -dr com.apple.quarantine /Applications/GitHub-Store.app
```

The final `xattr` is required until the app is signed and notarized.

## Releasing

No manual step needed. On each `release: types: [released]` event, the workflow:

1. Downloads `GitHub-Store-<version>-arm64.dmg` + `GitHub-Store-<version>-x64.dmg`.
2. Computes SHA256 of both.
3. Patches `version` + `sha256` in the tap repo's `Casks/github-store.rb`.
4. Commits + pushes to the tap repo.

Requires the `HOMEBREW_TAP_TOKEN` repo secret — a fine-grained PAT with
`contents: write` permission on the tap repo.

This reference copy is not auto-synced; treat the tap repo as canonical. If you
edit Cask metadata (caveats, zap paths, etc.), update both this file and the
tap repo until we drop one or the other.
