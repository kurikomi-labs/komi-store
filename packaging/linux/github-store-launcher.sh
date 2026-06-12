#!/usr/bin/env bash
#
# GitHub Store Linux launcher — native-launcher bypass.
#
# The jpackage-generated native launcher (bin/GitHub-Store) crashes inside
# glibc setenv() on glibc >= 2.42 (Fedora 43, Ubuntu 26.04, Debian 13, Bazzite)
# before the JVM is even mapped. The fault is in jvmLauncherStartJvm -> setenv,
# independent of environment contents and Linux distribution. See GH#563.
#
# We sidestep the broken native launcher entirely by reading the jpackage .cfg
# and invoking the bundled JVM directly — exactly what the Flatpak build already
# does in production. This script lives at <app-root>/bin/ and discovers the
# runtime, classpath, main class, and JVM options relative to its own location,
# so it works identically inside an AppImage mount, /opt/github-store (deb / rpm
# / Arch), or the raw Compose app-image directory.

set -eo pipefail

SELF="$(readlink -f "${BASH_SOURCE[0]:-$0}")"
BINDIR="$(dirname "$SELF")"
ROOTDIR="$(dirname "$BINDIR")"
APPDIR="$ROOTDIR/lib/app"
RUNTIME="$ROOTDIR/lib/runtime"
JAVA="$RUNTIME/bin/java"

# Compose strips native commands from the bundled runtime, so some packages ship
# no bin/java. Fall back to a system JVM so the app still launches (GH#746).
if [ ! -x "$JAVA" ]; then
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
  else
    JAVA="$(command -v java || true)"
  fi
fi

if [ -z "$JAVA" ] || [ ! -x "$JAVA" ]; then
  echo "github-store: no bundled or system Java 21+ runtime found" >&2
  exit 1
fi

CFG="$(find "$APPDIR" -maxdepth 1 -name '*.cfg' 2>/dev/null | head -n1 || true)"
if [ -z "$CFG" ] || [ ! -f "$CFG" ]; then
  echo "github-store: launcher .cfg not found under $APPDIR" >&2
  exit 1
fi

# jpackage .cfg uses $APPDIR / $ROOTDIR / $BINDIR tokens in paths and options.
expand() {
  local v="$1"
  v="${v//\$APPDIR/$APPDIR}"
  v="${v//\$ROOTDIR/$ROOTDIR}"
  v="${v//\$BINDIR/$BINDIR}"
  printf '%s' "$v"
}

classpath=""
modulepath=""
mainclass=""
mainmodule=""
javaopts=()
section=""

while IFS= read -r line || [ -n "$line" ]; do
  line="${line%$'\r'}"
  case "$line" in
    \[*\]) section="$line"; continue ;;
    ''|'#'*) continue ;;
  esac
  case "$section" in
    '[Application]')
      case "$line" in
        app.classpath=*)
          entry="$(expand "${line#app.classpath=}")"
          if [ -z "$classpath" ]; then classpath="$entry"; else classpath="$classpath:$entry"; fi
          ;;
        app.modulepath=*)
          entry="$(expand "${line#app.modulepath=}")"
          if [ -z "$modulepath" ]; then modulepath="$entry"; else modulepath="$modulepath:$entry"; fi
          ;;
        app.mainclass=*)  mainclass="${line#app.mainclass=}" ;;
        app.mainmodule=*) mainmodule="${line#app.mainmodule=}" ;;
      esac
      ;;
    '[JavaOptions]')
      case "$line" in
        java-options=*) javaopts+=("$(expand "${line#java-options=}")") ;;
      esac
      ;;
  esac
done < "$CFG"

cd "$APPDIR"

launch_args=("${javaopts[@]}")
if [ -n "$mainmodule" ]; then
  [ -n "$modulepath" ] && launch_args+=(--module-path "$modulepath")
  [ -n "$classpath" ] && launch_args+=(-cp "$classpath")
  launch_args+=(-m "$mainmodule")
elif [ -n "$classpath" ] && [ -n "$mainclass" ]; then
  launch_args+=(-cp "$classpath" "$mainclass")
else
  echo "github-store: could not resolve main class / module from $CFG" >&2
  exit 1
fi

exec "$JAVA" "${launch_args[@]}" "$@"
