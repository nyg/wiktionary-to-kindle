#!/usr/bin/env bash
#
# Builds a self-contained desktop bundle with jlink + jpackage.
#
# One script for all three platforms: GitHub's windows runners provide bash, and the per-OS
# differences here are small enough that two scripts would drift.
#
# Usage:
#   scripts/package.sh [dmg|app-image|msi|deb]
#
# Defaults to the natural installer type for the host OS. Run `mvn package` first.
set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

readonly APP_NAME="Wiktionary to Kindle"
readonly MAIN_CLASS="edu.self.w2k.gui.Launcher"
readonly BUNDLE_ID="io.github.nyg.wiktionary-to-kindle"
readonly VENDOR="nyg"

readonly BUILD_DIR="target/package"
readonly RUNTIME_DIR="$BUILD_DIR/runtime"
readonly INPUT_DIR="$BUILD_DIR/input"
readonly JAVAFX_MODS_DIR="$BUILD_DIR/javafx-mods"
readonly OUTPUT_DIR="target/dist"

# ──────────────────────────────────────────────────────────────────────────────
# Runtime modules
#
# The JDK set was computed with:
#   jdeps --print-module-deps --ignore-missing-deps target/wiktionary-to-kindle-*.jar
# which yields java.base, java.desktop, java.naming, java.net.http, java.scripting, java.sql.
#
# Four more are added that static analysis cannot see, because they are reached through service
# loading or resource lookup rather than bytecode references:
#
#   jdk.crypto.ec    TLS key agreement. Without it every HTTPS request fails at handshake, so
#                    neither the kaikki.org dump nor the kindling-cli download would work.
#   jdk.localedata   Language display names outside the root locale. Without it the language
#                    pickers and generated titles degrade to bare uppercase codes.
#   java.logging     logback's java.util.logging integration.
#   jdk.unsupported  sun.misc.Unsafe, which Jackson reaches for reflectively.
#
# java.xml is not listed because java.sql requires it transitively; logback's XML config needs it.
# ──────────────────────────────────────────────────────────────────────────────
readonly JDK_MODULES="java.base,java.desktop,java.naming,java.net.http,java.scripting,java.sql,java.logging,jdk.crypto.ec,jdk.localedata,jdk.unsupported"
readonly JAVAFX_MODULES="javafx.controls,javafx.fxml"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

# Override with MVN=... ; falls back to the Maven Daemon when plain mvn is absent.
resolve_mvn() {
  if [[ -n "${MVN:-}" ]]; then
    echo "$MVN"
  elif command -v mvn >/dev/null 2>&1; then
    echo "mvn"
  elif command -v mvnd >/dev/null 2>&1; then
    echo "mvnd"
  elif [[ -x "$HOME/.local/bin/mvnd" ]]; then
    echo "$HOME/.local/bin/mvnd"
  else
    die "neither mvn nor mvnd found on PATH"
  fi
}
readonly MVN_CMD="$(resolve_mvn)"

detect_default_type() {
  case "$(uname -s)" in
    Darwin) echo "dmg" ;;
    Linux) echo "deb" ;;
    MINGW* | MSYS* | CYGWIN*) echo "app-image" ;;
    *) die "unsupported platform: $(uname -s)" ;;
  esac
}

readonly PACKAGE_TYPE="${1:-$(detect_default_type)}"

case "$(uname -s)" in
  MINGW* | MSYS* | CYGWIN*) readonly PATH_SEPARATOR=";" ;;
  *) readonly PATH_SEPARATOR=":" ;;
esac

resolve_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]]; then
    echo "$JAVA_HOME"
  elif [[ -x /usr/libexec/java_home ]]; then
    /usr/libexec/java_home
  else
    die "JAVA_HOME is not set and could not be detected"
  fi
}

# Taken from the built JAR's filename rather than from Maven.
#
# `help:evaluate -DforceStdout` prints nothing under the Maven Daemon, so it would work in CI and
# fail locally. The JAR is required here anyway, and its name is the authoritative record of what is
# actually being packaged.
project_version() {
  local base
  base="$(basename "$1")"
  base="${base#wiktionary-to-kindle-}"
  echo "${base%.jar}"
}

# jpackage rejects a -SNAPSHOT suffix. Between releases the pom holds X.Y.Z-SNAPSHOT, so strip it.
app_version() {
  local version="$1"
  echo "${version%-SNAPSHOT}"
}

find_app_jar() {
  local jar
  jar=$(find target -maxdepth 1 -name 'wiktionary-to-kindle-*.jar' ! -name '*original*' ! -name '*sources*' -print | head -1)
  [[ -n "$jar" ]] || die "no shaded JAR in target/ — run 'mvn package' first"
  echo "$jar"
}

# Only the platform-classified JavaFX artifacts are real modules; the unclassified ones are ~300-byte
# stubs. Selecting by the presence of module-info.class rather than by filename keeps this correct if
# OpenJFX ever changes its naming, and stops a stub shadowing the real module on the module path.
collect_javafx_modules() {
  rm -rf "$JAVAFX_MODS_DIR"
  mkdir -p "$JAVAFX_MODS_DIR"

  local staging="$BUILD_DIR/javafx-staging"
  rm -rf "$staging"
  "$MVN_CMD" -q --no-transfer-progress dependency:copy-dependencies \
    -DoutputDirectory="$staging" \
    -DincludeGroupIds=org.openjfx \
    -Dmdep.stripVersion=false

  # `candidate` is declared local: bash scopes variables dynamically, so an undeclared loop variable
  # here would assign into the caller's scope and clobber main's $jar.
  local kept=0 listing candidate
  for candidate in "$staging"/*.jar; do
    [[ -e "$candidate" ]] || continue
    # Captured into a variable rather than piped into `grep -q`: grep exits on the first match,
    # which SIGPIPEs unzip, and under `set -o pipefail` that makes the whole test look false.
    listing="$(unzip -l "$candidate" 2>/dev/null || true)"
    if [[ "$listing" == *module-info.class* ]]; then
      cp "$candidate" "$JAVAFX_MODS_DIR/"
      kept=$((kept + 1))
    fi
  done
  rm -rf "$staging"

  # One module per JavaFX artifact: base, graphics, controls, fxml.
  [[ "$kept" -ge 4 ]] || die "expected at least 4 modular JavaFX jars, found $kept"
  log "collected $kept modular JavaFX jar(s)"
}

build_runtime() {
  local java_home="$1"
  rm -rf "$RUNTIME_DIR"

  log "jlink: building runtime image"
  "$java_home/bin/jlink" \
    --module-path "$java_home/jmods$PATH_SEPARATOR$JAVAFX_MODS_DIR" \
    --add-modules "$JDK_MODULES,$JAVAFX_MODULES" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress zip-6 \
    --output "$RUNTIME_DIR"
}

# Guards against the two failures that would otherwise only surface once the app is installed: a
# runtime missing the TLS provider, and one missing locale data.
verify_runtime() {
  log "verifying runtime image"
  local probe="$BUILD_DIR/RuntimeProbe.java"
  cat > "$probe" <<'JAVA'
public class RuntimeProbe {

    /**
     * Loads without initialising: a JavaFX Control's static initialiser demands a live toolkit, which
     * a packaging check has no display for. Resolving the class is what proves the module is present.
     */
    private static void requireClass(String name) throws Exception {
        Class.forName(name, false, ClassLoader.getSystemClassLoader());
    }

    public static void main(String[] args) throws Exception {
        requireClass("javafx.fxml.FXMLLoader");
        requireClass("javafx.scene.control.ComboBox");
        requireClass("javax.imageio.ImageIO");
        requireClass("edu.self.w2k.gui.Launcher");
        requireClass("edu.self.w2k.CLI");

        String greek = java.util.Locale.forLanguageTag("el").getDisplayLanguage(java.util.Locale.ENGLISH);
        if (greek.isBlank() || greek.equalsIgnoreCase("el")) {
            throw new IllegalStateException("locale data missing: 'el' resolved to '" + greek + "'");
        }
        if (java.util.Locale.getISOLanguages().length < 150) {
            throw new IllegalStateException("ISO language list truncated");
        }

        javax.net.ssl.SSLContext.getDefault().createSSLEngine();
        boolean ec = java.util.Arrays.stream(java.security.Security.getProviders())
                .flatMap(p -> p.getServices().stream())
                .anyMatch(s -> "KeyAgreement".equals(s.getType()) && s.getAlgorithm().startsWith("ECDH"));
        if (!ec) {
            throw new IllegalStateException("no ECDH KeyAgreement provider: HTTPS would fail");
        }
        System.out.println("runtime probe OK (el=" + greek + ")");
    }
}
JAVA

  # Compiled with the full JDK and then run inside the trimmed image: java's source-file mode needs
  # jdk.compiler, which a runtime image has no business shipping.
  local classes="$BUILD_DIR/probe-classes"
  rm -rf "$classes"
  mkdir -p "$classes"
  "$java_home/bin/javac" -nowarn -d "$classes" "$probe"

  "$RUNTIME_DIR/bin/java" \
    --add-modules "$JAVAFX_MODULES" \
    -cp "$classes$PATH_SEPARATOR$(find_app_jar)" \
    RuntimeProbe || die "runtime image is incomplete — see the failure above"
}

stage_input() {
  local jar="$1"
  rm -rf "$INPUT_DIR"
  mkdir -p "$INPUT_DIR"
  # The shaded JAR already bundles every dependency except JavaFX, which lives in the runtime image,
  # so the input directory is a single file.
  cp "$jar" "$INPUT_DIR/"
}

icon_argument() {
  case "$(uname -s)" in
    Darwin) [[ -f assets/icon.icns ]] && echo "--icon assets/icon.icns" ;;
    MINGW* | MSYS* | CYGWIN*) [[ -f assets/icon.ico ]] && echo "--icon assets/icon.ico" ;;
    *) [[ -f assets/icon.png ]] && echo "--icon assets/icon.png" ;;
  esac
}

platform_arguments() {
  case "$(uname -s)" in
    Darwin)
      # --mac-package-name is the menu-bar name and must stay within 16 characters, which the full
      # app name exceeds.
      echo "--mac-package-identifier $BUNDLE_ID --mac-package-name WiktionaryKindle"
      ;;
    MINGW* | MSYS* | CYGWIN*)
      if [[ "$PACKAGE_TYPE" == "msi" ]]; then
        echo "--win-menu --win-dir-chooser --win-per-user-install --win-shortcut"
      fi
      ;;
    *)
      echo "--linux-shortcut"
      ;;
  esac
}

build_package() {
  local java_home="$1" jar="$2" version="$3"
  rm -rf "$OUTPUT_DIR"
  mkdir -p "$OUTPUT_DIR"

  log "jpackage: building $PACKAGE_TYPE $version"

  # shellcheck disable=SC2046,SC2086
  "$java_home/bin/jpackage" \
    --type "$PACKAGE_TYPE" \
    --name "$APP_NAME" \
    --app-version "$version" \
    --vendor "$VENDOR" \
    --dest "$OUTPUT_DIR" \
    --input "$INPUT_DIR" \
    --main-jar "$(basename "$jar")" \
    --main-class "$MAIN_CLASS" \
    --runtime-image "$RUNTIME_DIR" \
    --java-options "--add-modules=$JAVAFX_MODULES" \
    --java-options "-XX:MaxRAMPercentage=75" \
    --java-options "-Dfile.encoding=UTF-8" \
    $(icon_argument) \
    $(platform_arguments)
}

main() {
  local java_home jar raw_version version
  java_home="$(resolve_java_home)"
  jar="$(find_app_jar)"
  raw_version="$(project_version "$jar")"
  version="$(app_version "$raw_version")"

  [[ -n "$version" ]] || die "could not determine the project version"

  log "packaging $APP_NAME $version ($PACKAGE_TYPE) from $(basename "$jar")"
  mkdir -p "$BUILD_DIR"

  collect_javafx_modules
  build_runtime "$java_home"
  verify_runtime
  stage_input "$jar"
  build_package "$java_home" "$jar" "$version"

  log "done:"
  ls -la "$OUTPUT_DIR"
}

main "$@"
