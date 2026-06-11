# 🚀 Komi Store 1.9.1 — GitHub Store is now Komi Store

> ## 🪪 New name: **Komi Store**, a product of **Kurikomi**
> This is a priority release. To stay clear of trademark risk around the **GitHub** name, the app has been **renamed from GitHub Store to Komi Store** and now lives under the **Kurikomi** organization. The app itself is unchanged — your library, favourites, starred repos, tokens, settings, and installed apps all carry over automatically. Only the name and home have moved.

Following 1.9.0's big design overhaul, **1.9.1 is mainly the rename** plus a batch of new browsing features and stability fixes.

## ✨ New Features
- Browse a repository's **Issues** without leaving the app — filter open/closed, see labels at a glance, and open full threads with comments, images, and Markdown
- New per-repository **Security** page — published advisories with severity badges, plus the project's SECURITY.md policy inline
- **Sign in to interact** — star a repo, comment on issues, open new issues, and react with 👍
- Browse a repository's **Pull requests** with open/closed filters
- **Android home-screen shortcuts** — long-press the app icon to jump straight to Search, Library, Favourites, or Recently Viewed
- **Import any GitHub user's starred repositories** into your favourites — no sign-in needed
- **Select & copy** a repository's name, description, and stats from its detail page
- **Windows portable** build — unzip and run `Komi-Store.exe` directly, no installer
- **Desktop menu bar** — new File and Go menus to jump around the app, open settings, or quit

## 🐛 Bug Fixes
- **Debian 12 / Ubuntu 22.04** can install the `.deb` again — single package now installs across every Debian generation (time64 dependency fix)
- Desktop sign-in no longer gets stuck on "Waiting for authorization…" — it times out so you can retry or switch to a device code / token
- **Windows**: "Open in Komi Store" links now reliably open the app — handler registered more robustly and re-registered after updates
- **macOS**: the app no longer quits unexpectedly when you open your Profile or switch between windows

**Full Changelog**: [1.9.0...1.9.1](https://github.com/kurikomi-labs/komi-store/compare/v1.9.0...v1.9.1)

🌐 [Website](https://komistore.app) | 💬 [Discord](https://discord.github-store.org)
