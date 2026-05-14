cask "github-store" do
  arch arm: "arm64", intel: "x64"

  version "1.8.2"
  sha256 arm:   "ad0c532873c0400736b7ea706a33604f7ef93b2479a4a6f32673a789b18ccd8e",
         intel: "faf002283c301db2a97f7a32193778f678d42ed71551a623711cd170d6fde16a"

  url "https://github.com/OpenHub-Store/GitHub-Store/releases/download/v#{version}/GitHub-Store-#{version}-#{arch}.dmg"
  name "GitHub Store"
  desc "Cross-platform app store for GitHub releases"
  homepage "https://github.com/OpenHub-Store/GitHub-Store"

  livecheck do
    url :url
    strategy :github_latest
  end

  auto_updates false
  depends_on macos: :big_sur

  app "GitHub-Store.app"

  uninstall quit: "zed.rainxch.githubstore"

  zap trash: [
    "~/Library/Application Support/GitHub-Store",
    "~/Library/Caches/GitHub-Store",
    "~/Library/Logs/GitHub-Store",
    "~/Library/Preferences/zed.rainxch.githubstore.plist",
    "~/Library/Saved Application State/zed.rainxch.githubstore.savedState",
  ]

  caveats <<~EOS
    GitHub Store is not yet signed with an Apple Developer ID.
    macOS Gatekeeper will block it from launching with a "damaged" or
    "cannot be opened" error.

    To allow the app to launch, run:

      xattr -dr com.apple.quarantine "#{appdir}/GitHub-Store.app"

    This step is required after each install or upgrade until the app is
    signed and notarized.
  EOS
end
