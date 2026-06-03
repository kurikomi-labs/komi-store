# Flatpak Packaging for GitHub Store

## Sandbox limitations

The sandbox can't exec or install host binaries, so the Flatpak is browse-and-download only: files land in `~/Downloads` (`--filesystem=xdg-download:rw`); the user installs them manually outside.

Don't add `--filesystem=host` or `--talk-name=org.freedesktop.Flatpak` without a tested host-spawn path — Flathub rejects unjustified host access. For install + auto-update, use the AppImage / deb / rpm / Arch builds.

## Prerequisites

Install Flatpak and the build tools:

```bash
# Fedora
sudo dnf install flatpak flatpak-builder

# Ubuntu/Debian
sudo apt install flatpak flatpak-builder

# Arch
sudo pacman -S flatpak flatpak-builder
```

Install the required runtimes:

```bash
flatpak install flathub org.freedesktop.Platform//24.08
flatpak install flathub org.freedesktop.Sdk//24.08
flatpak install flathub org.freedesktop.Sdk.Extension.openjdk21//24.08
```

## Setup (One-Time)

### 1. Generate Gradle dependency sources

Flatpak builds run without network access, so all Maven/Gradle dependencies must
be pre-downloaded and listed in a JSON manifest.

Add the plugin to your root `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.jwharm.flatpak-gradle-generator") version "1.7.0"
}
```

Then generate the sources file:

```bash
./gradlew flatpakGradleGenerator --no-configuration-cache
```

This creates `flatpak-sources.json` in the project root. Move it to this directory:

```bash
mv flatpak-sources.json packaging/flatpak/
```

### 2. Verify SHA256 hashes

The manifest uses pre-computed SHA256 hashes. To verify or update them:

```bash
# Gradle distribution
curl -sL https://services.gradle.org/distributions/gradle-8.14.3-bin.zip | sha256sum

# JBR x64 (check latest at https://github.com/JetBrains/JetBrainsRuntime/releases)
curl -sL https://cache-redirector.jetbrains.com/intellij-jbr/jbr-21.0.10-linux-x64-b1163.105.tar.gz | sha256sum

# JBR aarch64
curl -sL https://cache-redirector.jetbrains.com/intellij-jbr/jbr-21.0.10-linux-aarch64-b1163.105.tar.gz | sha256sum
```

### 3. Update screenshot URLs

Edit `zed.rainxch.githubstore.metainfo.xml` to point to hosted screenshot images.
Flathub requires at least one screenshot with a publicly accessible URL.

## Building Locally

```bash
cd packaging/flatpak

# Build
flatpak-builder --force-clean build-dir zed.rainxch.githubstore.yml

# Test run
flatpak-builder --run build-dir zed.rainxch.githubstore.yml githubstore

# Install locally
flatpak-builder --user --install --force-clean build-dir zed.rainxch.githubstore.yml
```

## Validating

```bash
# Validate AppStream metainfo
flatpak run org.freedesktop.appstream-glib validate zed.rainxch.githubstore.metainfo.xml

# Lint manifest (requires org.flatpak.Builder)
flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest zed.rainxch.githubstore.yml
```

## Publishing to Flathub

1. Fork `https://github.com/flathub/flathub`
2. Checkout the `new-pr` branch
3. Copy the manifest YAML and `flatpak-sources.json` to the repo root
4. Open a PR titled "Add zed.rainxch.githubstore"
5. Reviewers will trigger test builds with `bot, build`
6. After approval, you get write access to `flathub/zed.rainxch.githubstore`

## File Reference

| File | Purpose |
|------|---------|
| `zed.rainxch.githubstore.yml` | Flatpak build manifest |
| `zed.rainxch.githubstore.desktop` | Desktop launcher entry |
| `zed.rainxch.githubstore.metainfo.xml` | AppStream metadata for Flathub listing |
| `githubstore.sh` | Shell launcher (invokes `java -jar` with bundled JRE) |
| `disable-android-for-flatpak.sh` | Strips Android targets for sandbox build |
| `flatpak-sources.json` | Pre-downloaded Gradle dependencies (generated) |
