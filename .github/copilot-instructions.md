# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` converts Wiktionary data into Kindle-compatible MOBI dictionaries. The pipeline is: **download** → **generate** (fetches `kindling-cli` on first run, then runs `kindling-cli build`).

Two front-ends share one service layer:

- a **JavaFX desktop app** (`edu.self.w2k.gui.Launcher`), distributed as a jpackage bundle with an embedded Java runtime — the primary way users run this;
- the original **picocli CLI** (`edu.self.w2k.CLI`), still shipped as a cross-platform fat JAR for Linux and scripted use.

The service layer under `command`, `download`, `parse`, `render`, `write` and `kindling` is UI-free and shared by both. Keep it that way: anything that needs JavaFX belongs in `gui`.

Data source: [kaikki.org](https://kaikki.org) pre-extracted JSONL dumps, produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) with all Lua templates fully expanded. One dump per Wiktionary language edition.

## Build & Test

Requires Java 25 and Apache Maven.

```sh
mvn package          # compiles, runs tests, produces the fat JAR
mvn javafx:run       # runs the desktop app
scripts/package.sh   # jlink + jpackage bundle for the host platform → target/dist
```

JUnit 5 tests live in `src/test/java/edu/self/w2k/`. `MainFxmlLoadTest` needs a graphics toolkit and self-skips without one; CI runs the suite under `xvfb-run`.

The version comes from `${project.version}` via a filtered `application.properties`, read through `AppVersion`. Resource filtering is scoped to that one file — filtering all of `src/main/resources` would corrupt `icon.png`.

## Running the CLI

The CLI entry point is `edu.self.w2k.CLI`; the GUI's is `edu.self.w2k.gui.Launcher`.

```sh
# Download kaikki.org dump to the dumps folder (skips if a dump for that lang already exists)
java -jar target/wiktionary-to-kindle-<version>.jar download        # English (default)
java -jar target/wiktionary-to-kindle-<version>.jar download fr     # French edition
java -jar target/wiktionary-to-kindle-<version>.jar download fr --dumps-dir ./dumps
# dl is a short alias for download

# Generate Kindle dictionary from a downloaded dump.
# DUMP_LANG = which Wiktionary edition to read; WORD_LANG = ISO 639-1 filter.
# The latest dump matching DUMP_LANG in the dumps folder is auto-discovered.
java -jar target/wiktionary-to-kindle-<version>.jar generate <DUMP_LANG> <WORD_LANG>
java -jar target/wiktionary-to-kindle-<version>.jar generate el en --dumps-dir ./dumps --dictionaries-dir ./dictionaries
java -jar target/wiktionary-to-kindle-<version>.jar generate el en --kindling-version vX.Y.Z
java -jar target/wiktionary-to-kindle-<version>.jar generate el en --kindling-cli /usr/local/bin/kindling-cli
# gen is a short alias for generate

java -jar target/wiktionary-to-kindle-<version>.jar --help
java -jar target/wiktionary-to-kindle-<version>.jar --version
```

`download` exits 1 when the transfer fails. The CLI reads the **same** `preferences.properties` as the GUI, so both front-ends share one dumps folder and one dictionaries folder; `--dumps-dir` / `--dictionaries-dir` override it per invocation.

## Architecture

### Package Structure

- **`edu.self.w2k`** — `CLI` — picocli root command + inner `Download` / `Generate` subcommand wiring classes, and `BuildVersion` (reports the filtered build version)
- **`edu.self.w2k.gui`** — JavaFX front-end. `Launcher` (plain `main`, **must not** extend `Application` — a classpath launch otherwise fails with "JavaFX runtime components are missing"), `App` (FXML load, appender install), `MainViewModel` (state and derivations; `javafx.base` only, so unit-testable without a toolkit), `MainController` (bindings only), `PipelineService`/`PipelineTask` (threading + cancellation), `UiLogAppender`, `ProgressSnapshot`, `PreferencesDialog`, `ByteSizes`, `LanguageConverter` (resolves typed text against the languages a picker currently offers, `null` for anything else), `WordLanguageConverter` (same, appending the sense count), `ComboBoxFilter` (type-to-filter on the pickers), `SystemTheme` (AtlantaFX Cupertino on macOS, following the system colour scheme). FXML and CSS in `src/main/resources/edu/self/w2k/gui/`
- **`edu.self.w2k.pipeline`** — `DictionaryPipeline` — the combined download-then-generate flow, JavaFX-free and injectable so it stays testable
- **`edu.self.w2k.progress`** — `ProgressListener` (`Stage`, `TOTAL_UNKNOWN`, `NOOP`), `CountingInputStream`. Progress is constructor-injected everywhere, so each collaborator keeps its pre-existing constructor arity
- **`edu.self.w2k.config`** — `AppInfo` (the app's display name, kebab-case slug and dictionary prefix, as the single source for all three), `AppPaths` (config, cache, state and data dirs), `Preferences` (properties file), `LanguageCatalog` (the bundled fallback dropdown lists), `AppVersion`
- **`edu.self.w2k.kaikki`** — `KaikkiCatalog` (fetches and caches kaikki's edition and per-edition language lists), `KaikkiHtml` (pure HTML parsing), `KaikkiLanguage`, `LanguageCodeResolver` (kaikki language name → `lang_code`), `PageFetcher`. JavaFX-free, so scraping stays out of `gui`; the CLI never touches it
- **`edu.self.w2k.dump`** — `DumpCatalog`, `DumpFile` — lists/deletes dumps; `CLI.findLatestDump` delegates to `latestFor`
- **`edu.self.w2k.command`** — `DownloadCommand`, `GenerateCommand` — service orchestrators; `Command` interface. Both expose an `execute()` returning the produced path alongside the interface's `void run()`
- **`edu.self.w2k.download`** — `KaikkiDumpDownloader` (HttpClient-based), `DumpDownloader` interface, `DownloadResult`
- **`edu.self.w2k.write`** — `DictionaryWriter` interface, `DictionaryTitles` utility
- **`edu.self.w2k.write.opf`** — `OpfDictionaryWriter` (emits chunked `.html` + `.opf`), `HtmlChapterRenderer`
- **`edu.self.w2k.kindling`** — `KindlingDictionaryConverter` (composes `OpfDictionaryWriter` + kindling binary), `KindlingCliResolver` (override → PATH → cache → download), `KindlingDownloader` (GitHub releases API + SHA-256 verify), `KindlingPlatform` enum, `KindlingRelease` (loads the pinned version + per-platform digests from `src/main/resources/kindling-release.properties`), `KindlingException`
- **`edu.self.w2k.parse`** — `JsonlDictionaryParser`, `DictionaryParser` interface
- **`edu.self.w2k.render`** — `HtmlDefinitionRenderer`, `DefinitionRenderer` interface
- **`edu.self.w2k.model`** — `LexiconEntry` plus Jackson-annotated records: `WiktionaryEntry`, `WiktionarySense`, `WiktionaryExample`, `WiktionaryForm`, `WiktionaryFormOf`

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz` from kaikki.org |
| `dictionaries/` | Final `.mobi` dictionary files, plus side-artefacts `.opf`, `-N.html`, `-toc.ncx` and `-cover.jpg` |

Both front-ends resolve them the same way: absolute, from `Preferences`, defaulting under `AppPaths.defaultDataDir()` (`~/Documents/wiktionary-to-kindle`). A bundled `.app` launches with `cwd=/`, so relative paths would resolve at the filesystem root — this is not a stylistic choice. The `Preferences` compact constructor enforces it, calling `toAbsolutePath().normalize()` on all three paths, so a relative value from a hand-edited properties file or from text typed into `PreferencesDialog` is resolved once, at the boundary, and never reaches a collaborator. Do not re-normalise downstream, and do not weaken it to a `load()`-only check — the dialog constructs the record directly.

The CLI additionally accepts `--dumps-dir` and `--dictionaries-dir`, overriding the preferences for that invocation. `CLI.Download.dumpsDir(Preferences)` and the matching pair on `CLI.Generate` are the single resolution point, and take the loaded preferences as an argument so they stay testable without touching the real config file. Up to 2.0.3 the CLI was CWD-relative via `CLI.DUMPS_DIR` / `CLI.DICTIONARIES_DIR` and never read preferences; both constants are gone, as is the `KaikkiDumpDownloader(String)` constructor that hardcoded `Path.of("dumps")`.

`AppPaths` resolves four roles, all named `AppInfo.SLUG`. Unix-likes — **macOS included** — follow the XDG Base Directory spec; Windows gets sibling directories under `%LOCALAPPDATA%\wiktionary-to-kindle\`:

| Role | Unix | Windows | Holds |
|------|------|---------|-------|
| `configDir()` | `$XDG_CONFIG_HOME` → `~/.config` | `…\Config` | `preferences.properties` |
| `cacheDir()` | `$XDG_CACHE_HOME` → `~/.cache` | `…\Cache` | `kindling/<version>/<asset>` |
| `stateDir()` | `$XDG_STATE_HOME` → `~/.local/state` | `…\State` | `logs/app.log` |
| `defaultDataDir()` | `$XDG_DOCUMENTS_DIR` → `~/Documents` | `%USERPROFILE%\Documents` | `dumps/`, `dictionaries/` |

The data dir is under Documents rather than `$XDG_DATA_HOME` on purpose — see the note above — but resolves that folder the XDG way: the `XDG_DOCUMENTS_DIR` env var, then the same key in `$XDG_CONFIG_HOME/user-dirs.dirs` (a leading `$HOME` is expanded, any parse failure falls through), then `~/Documents`.

A relative value in any of these variables — `HOME` included — is ignored with a warning and treated as unset, as the XDG spec requires. `AppPaths.absolutePath` is the check; it guards the Unix branch only, because `Path.isAbsolute()` answers for the filesystem the JVM runs on and would call a Windows `C:\local` relative on the Linux CI runner. The Windows variables are outside the spec anyway.

### Dictionary Output Format

`HtmlDefinitionRenderer` renders a `List<WiktionarySense>` into an HTML string:

```
<ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`. Entries with no renderable glosses return `Optional.empty()` and are skipped. Entries whose renderable senses all carry a `form_of` reference (e.g. Latin *suis* → "Datif pluriel de suus.") are flagged as form-of-only via `RenderedEntry.formOfLemmas()`.

`GenerateCommand` groups entries into a `TreeMap<String, List<LexiconEntry>>` in memory using `normaliseKey()`, then runs two post-passes before handing the map to the `DictionaryWriter`:

1. `foldFormOfEntries()` — folds form-of-only lookup keys into their lemma's inflection index. On Kindle an exact headword match shadows the `<idx:iform>` index, so this makes an inflected-form lookup resolve straight to the full lemma entry (issue #56). Folding is all-or-nothing per key: only when every entry under the key is form-of with a lemma present in the map is the key dropped (words registered as iforms on their lemma(s), multi-lemma forms on all of them); otherwise — homograph with its own meaning, missing lemma, chain — the key keeps all its entries. See `docs/form-of-folding.md`.
2. `filterFormsCollidingWithHeadwords()` — drops any inflection form whose normalised text still exists as a headword key.

`HtmlChapterRenderer` builds one MobiPocket HTML document (≤ 10 000 entries per chunk) preserving Amazon's `<idx:entry>`/`<idx:orth>` markup and `xmlns:mbp`/`xmlns:idx` namespace declarations.

**Every output file is named from `DictionaryTitles.baseName(src, trg)`** → `w2k-dictionary-{src}-{trg}`, lowercased. The `W2K` prefix (from `AppInfo.DICTIONARY_PREFIX`) also leads the `<dc:title>`, which is the only thing Kindle shows in its dictionary settings list. The NCX and cover are named from the same stem rather than the fixed `toc.ncx` / `cover.jpg` they once used: several dictionaries share one output directory, so fixed names had each run overwrite the previous run's side-artefacts.

`OpfDictionaryWriter` chunks the entry map, writes `w2k-dictionary-{src}-{trg}-N.html` files, a `-toc.ncx` navigation map and a `-cover.jpg`, then writes a `w2k-dictionary-{src}-{trg}.opf` OPF 2.0 manifest (with `<DictionaryInLanguage>` / `<DictionaryOutLanguage>` in `<x-metadata>`). Returns the OPF path.

`KindlingDictionaryConverter` composes `OpfDictionaryWriter` with a `kindling-cli` binary. It calls `KindlingCliResolver.resolve()` to obtain the binary, then runs `kindling-cli build <opf> -o <mobi>`. Output: `dictionaries/w2k-dictionary-{src}-{trg}.mobi` (plus the side-artefacts above).

`KindlingCliResolver.resolve()` tries in order: explicit `--kindling-cli` override → PATH probe (`which`/`where`) → cached binary at `<AppPaths.cacheDir()>/kindling/<version>/<assetName>` (SHA-256 verified) → download via `KindlingDownloader`. `KindlingDownloader` fetches from GitHub Releases, verifies SHA-256 against the pinned digests from `kindling-release.properties` (loaded by `KindlingRelease`), or the GitHub API `digest` field for non-default versions, renames atomically, and marks the file executable.

## Key Conventions

- **Naming**: the app has exactly two names, both in `AppInfo` — `DISPLAY_NAME` (`Wiktionary to Kindle`: window title, jpackage `--name`, `.app`/`.exe`, Scoop shortcut) and `SLUG` (`wiktionary-to-kindle`: artifactId, CLI command, directories, release assets, brew/scoop package). Never introduce a third spelling. `WiktionaryToKindle` and `WiktionaryKindle` both existed once and were removed; `--mac-package-name` is deliberately left unset so `CFBundleName` falls back to `--name`.
- **CLI** uses [picocli](https://picocli.info/). `CLI.java` is the root `@Command`; `Download` and `Generate` are inner static subcommand classes that wire collaborators and delegate to the service-layer command classes.
- **Service classes** (`DownloadCommand`, `GenerateCommand`) are independent of picocli and of JavaFX — they can be constructed directly in tests.
- **Progress** flows through `ProgressListener`, constructor-injected with a `NOOP` default. Emissions are throttled (roughly every 4 MB); the dump is millions of lines, so per-item reporting would swamp any listener. Download progress needs `BodyHandlers.ofInputStream` plus a manual copy loop — `ofFile` offers neither a byte callback nor a cancellation point. Parse progress counts the *compressed* stream, since a gzip member's uncompressed size is unknowable up front.
- **Cancellation** takes two mechanisms: interrupting the worker covers the download and parse loops, but the kindling stage blocks in `Process.waitFor()`, so `PipelineTask` tracks the process and destroys it in `cancelled()`.
- **Dumps table cell values** are wired with explicit accessor lambdas, never `PropertyValueFactory`. That factory introspects JavaBean names (`langProperty()`, then `getLang()`); `DumpFile` is a record exposing `lang()`, so it matches nothing, returns a null cell value and renders a blank column — with no compile error and no runtime exception. `MainFxmlLoadTest.should_render_a_value_in_every_dumps_column` locks this in.
- **Language pickers are editable, but not free text.** `ComboBoxFilter` puts a `FilteredList` behind each one, so typing narrows the drop-down (code prefix or name substring, case-insensitively) and text matching nothing leaves an empty list. The value is only ever set from `LanguageConverter.fromString`, which resolves against the languages that picker currently offers and returns `null` for anything else; a null value disables Start via `MainViewModel.startable`. That is what keeps issue #83 fixed — a code kaikki does not serve can be *typed*, but never *selected*, so it cannot reach the downloader and fail at the HTTP 404. Do not restore the old `orElseGet(() -> Language.of(code))` fallback in the converter; it is the whole hole. Note also that clearing the value makes the skin rewrite the editor via `converter.toString(null)`, so `ComboBoxFilter` guards every text reaction re-entrantly and restores the text and caret — without that, a keystroke matching nothing erases itself as it is typed. `ComboBoxFilter.sourceOf` exists because `getItems()` is the filtered view and writing to it throws; background refreshes must go through it.
- **Both picker lists** come from kaikki at runtime via `KaikkiCatalog`, refreshed on a virtual thread and cached for 7 days under `AppPaths.cacheDir()/catalog`; `kaikki-editions.properties` and `LanguageCatalog.wordLanguages()` are only the offline fallback. Every failure degrades silently at `debug` — worst case must equal the pre-fetch behaviour, never a dialog or an empty dropdown.
- **The word language list is scoped to the selected edition.** Offering all ~184 ISO 639-1 codes against, say, dewiktionary's 60 languages meant most choices produced an empty dictionary after a multi-gigabyte download. `LanguageCodeResolver` maps kaikki's localised names to the dump's `lang_code` — the bundled `kaikki-language-codes.properties` first, then `Locale.getDisplayLanguage` in the edition's own locale. **Unmatched names are dropped, never guessed**, because `JsonlDictionaryParser` filters on an exact `lang_code` match and a wrong code silently yields an empty dictionary. Regenerate the alias table by reading each per-language dump's first JSONL line, as its header describes — parse that line as JSON and take the *top-level* `lang_code`, since a nested one under `translations` will otherwise match first and be wrong.
- **The theme is a preference; the platform only picks its default.** `AppTheme` (in `config`, so `Preferences` stays JavaFX-free for the CLI) has two values, `JAVAFX` and `CUPERTINO`, persisted as `theme=javafx|cupertino`; `AppTheme.defaultFor(osName)` returns `CUPERTINO` on macOS and `JAVAFX` everywhere else, since Cupertino is a macOS look but an explicit choice is still the user's to make. An unreadable or unknown value falls back to that default rather than failing. `SystemTheme.install(scene, choice)` takes `MainController.themeChoice()` — `preferencesProperty().map(Preferences::theme)` — so pressing OK in Preferences restyles the open window with no restart and nothing to wire in `onPreferences`. Under `CUPERTINO` it applies `CupertinoLight`/`CupertinoDark` from `Platform.getPreferences().getColorScheme()` and re-applies from a listener on `colorSchemeProperty()`, following System Settings live; under `JAVAFX` it restores `Application.STYLESHEET_MODENA` **and removes `app-atlantafx.css` from the scene**, which is not optional — its rules name AtlantaFX tokens that resolve to nothing under Modena. Any failure degrades at `debug` to the untouched default, same rule as the kaikki fetches. `MainController.onPreferences` copies the main scene's stylesheets onto the dialog pane, which otherwise inherits the user agent stylesheet but none of the app's own classes. `app.css` is written against Modena tokens and always loaded; `app-atlantafx.css` restates the handful of rules whose tokens AtlantaFX does not define — notably `-fx-selection-bar`, which has no AtlantaFX equivalent and would leave selected log lines unpainted — and is added to the scene only when the theme is installed. Keep new colours out of `app.css` for that reason: a token from one theme is an unresolved lookup in the other. The same sheet re-states the row metrics, which were measured against Modena: Cupertino pads a drop-down cell by 6px top and bottom, so `app.css`'s `-fx-fixed-cell-size: 24px` cut every language name in half, and it sizes list rows at `3em`, which made log lines 34.5px instead of 16px. `MainFxmlLoadTest.should_keep_list_rows_compact_and_unclipped_under_the_cupertino_theme` locks both in by measuring real cells; it also needs `Platform.setImplicitExit(false)`, since it shows a window and closing the last one would take the toolkit down for every later test. The shade config also filters `module-info.class`, since atlantafx-base is an explicit module and its descriptor would otherwise land at the root of the fat JAR and make it claim to be `atlantafx.base`.
- **Logging** uses SLF4J 2.x with Logback Classic. `@Slf4j` (Lombok) is used on all classes. `logback.xml` configures the CLI console only; the GUI adds `UiLogAppender` and a file appender **programmatically**, so CLI output is untouched. `UiLogAppender` buffers into a bounded queue the UI drains in batches — never one `Platform.runLater` per event, which would freeze the window. The drain `Timeline` parks itself after ~2 s idle and `UiLogAppender.setWakeListener` restarts it, so an idle window is not held awake by a permanently running animation.
- **Subprocesses**: `KindlingDictionaryConverter.defaultRunner()` pumps merged stdout/stderr through SLF4J. Do not switch it back to `inheritIO()` — a windowed app has no terminal, so the output would vanish.
- **Jackson** is used for JSONL parsing. Model records carry `@JsonIgnoreProperties(ignoreUnknown = true)`. `ObjectMapper` is configured with `Nulls.AS_EMPTY` so missing collection fields default to empty lists. The `ObjectReader` is reused across all lines for efficiency.
- **Parser streaming**: `JsonlDictionaryParser.parse()` returns a lazy `Stream<WiktionaryEntry>` backed by a `BufferedReader.lines()` pipeline. Callers must close the stream (use try-with-resources).
- **Download** uses `java.net.http.HttpClient` and an atomic `.part` file rename to avoid corrupt downloads on failure. A HEAD request (30 s timeout) reads `last-modified` and `content-length` first, so the target filename and the skip-if-already-downloaded check resolve before any body transfer; the GET that follows carries a deliberately generous 6 h ceiling because dumps are multi-gigabyte (a request-level timeout bounds the *whole* exchange, not just the headers). URL is computed per lang: `en` → `/dictionary/`, others → `/{lang}wiktionary/`.
- **Dump file path**: dumps are named `dumps/raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz`. `generate` resolves the file via `CLI.findLatestDump(lang, dumpsDir)` → `DumpCatalog.latestFor`, which globs the dumps dir for that prefix and picks the lexicographically latest filename (ISO date format sorts correctly). If no dump matches, generate exits 1. Discovery stays filename-based rather than going through `DumpFile.parse`, so a dump named `-unknown` (kaikki omitted `last-modified`) is still usable even though it cannot be listed in the dumps pane.

## Packaging

`scripts/package.sh` runs jlink then jpackage; one bash script covers all platforms, since Windows runners provide bash. Things that will bite if changed carelessly:

- **The JDK module list** was computed with `jdeps --print-module-deps`, plus four modules static analysis cannot see: `jdk.crypto.ec` (without it every HTTPS request fails at handshake), `jdk.localedata` (without it language names degrade to bare uppercase codes), `java.logging` and `jdk.unsupported`. Both of the first two fail *silently*, which is why the script links a probe into the fresh image and runs it there.
- **JavaFX is excluded from the shaded JAR** (`artifactSet` exclude in the shade config). JavaFX resolves to platform-classified native artifacts, so bundling it would tie the CLI JAR to whichever OS built it.
- **Only classified JavaFX jars are real modules**; the unclassified ones are ~300-byte stubs that shadow them on the module path. The script selects by presence of `module-info.class`.
- **`Launcher` must not extend `Application`** — see the `gui` package note above.
- Use `$PATH_SEPARATOR`, not a literal `:`, in module paths and classpaths; Windows uses `;`.

## CI & Release

- `java-ci.yaml` — `build` runs `mvn verify` under `xvfb-run` on Ubuntu; `package` runs jlink+jpackage on macOS and Windows, so a packaging regression is caught in a PR rather than at release time.
- `release.yml` — `workflow_dispatch` with a bump choice. Bumps via `maven-release-plugin` (hence the `-SNAPSHOT` version and `<scm>` block), builds the DMG, Scoop ZIP and portable JAR, publishes, then dispatches to `nyg/homebrew-tap` and pushes a manifest to `nyg/scoop-bucket`. `RELEASE_TOKEN` needs write access to all three repos.
- **Release asset names** are `wiktionary-to-kindle-<version>-<os>-<arch>.<ext>` — `…-macos-arm64.dmg`, `…-windows-x64-scoop.zip`, and the plain `wiktionary-to-kindle-<version>.jar` (cross-platform, so no os/arch suffix). Every reference interpolates `VERSION` from a step-level `env`: the rename step, the 7z output, `gh release create`, both `sha256sum` inputs and the Scoop manifest url. Renaming an asset means updating the Homebrew cask in `nyg/homebrew-tap` too, and only **after** a release has published under the new name — the Scoop bucket regenerates itself.
- `scripts/package.sh` derives the version by stripping the `wiktionary-to-kindle-` prefix off the built JAR's filename, so that literal must keep matching the pom's artifactId.
- Conventional commits; `git-cliff` generates the changelog from them via `cliff.toml`.
