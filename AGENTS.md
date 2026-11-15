# Agent Instructions

## Project Overview

`wiktionary-to-kindle` converts Wiktionary data into Kindle-compatible MOBI dictionaries. The pipeline is **download** → **generate** (which fetches `kindling-cli` on first run, then runs `kindling-cli build`).

Two front-ends share one service layer:

- a **JavaFX desktop app** (`edu.self.w2k.gui.Launcher`), distributed as a jpackage bundle with an embedded Java runtime — the primary way users run this;
- a **picocli CLI** (`edu.self.w2k.CLI`), shipped as a cross-platform fat JAR for Linux and scripted use.

The service layer is UI-free: anything that needs JavaFX belongs in `gui`.

Data source: [kaikki.org](https://kaikki.org) JSONL dumps, produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) with all Lua templates expanded. One dump per Wiktionary language edition.

## Read first

- **`gui` package, FXML or CSS** → [`docs/gui.md`](docs/gui.md): language pickers, themes, per-desktop window conventions, the log console.
- **`scripts/package.sh`, the shade config, any workflow, a release asset** → [`docs/packaging.md`](docs/packaging.md): jlink and jpackage gotchas, what CI covers, how a release is cut.
- **Form-of folding** → [`docs/form-of-folding.md`](docs/form-of-folding.md).
- **Order of a run** → [`docs/program-flow.md`](docs/program-flow.md).

## Build & Test

Requires Java 25 and Apache Maven.

```sh
mvn package          # compiles, runs tests, produces the fat JAR
mvn javafx:run       # runs the desktop app
scripts/package.sh   # jlink + jpackage bundle for the host platform → target/dist
```

`MainFxmlLoadTest` needs a graphics toolkit and self-skips without one; CI runs the suite under `xvfb-run`.

The version comes from `${project.version}` via a filtered `application.properties`, read through `AppVersion`. Resource filtering is scoped to that one file: filtering all of `src/main/resources` corrupts `icon.png`.

Commits follow Conventional Commits; `git-cliff` generates the changelog from them via `cliff.toml`.

## CLI

```sh
java -jar target/wiktionary-to-kindle-<version>.jar download [DUMP_LANG]             # alias dl, default en
java -jar target/wiktionary-to-kindle-<version>.jar generate <DUMP_LANG> <WORD_LANG>  # alias gen
```

`DUMP_LANG` is the Wiktionary edition to read, `WORD_LANG` the `lang_code` to keep. `download` skips an edition that already has a dump and exits 1 when the transfer fails. `generate` takes the latest dump for the edition and exits 1 when none matches. It accepts `--kindling-version` and `--kindling-cli`; both commands accept `--dumps-dir`, and `generate` also `--dictionaries-dir`.

## Architecture

### Packages

All under `edu.self.w2k`:

- root — `CLI`: the picocli root command, with inner `Download` and `Generate` classes that wire collaborators and delegate to the service layer
- `gui` — the JavaFX front-end. `Launcher` is a plain `main` and must stay one: if it extends `Application`, a classpath launch fails with "JavaFX runtime components are missing"
- `pipeline` — `DictionaryPipeline`, the download-then-generate flow, JavaFX-free and injectable
- `progress` — `ProgressListener` (`Stage`, `TOTAL_UNKNOWN`, `NOOP`), `CountingInputStream`
- `config` — `AppInfo`, `AppPaths`, `Preferences`, `AppTheme`, `AppVersion`, `LanguageCatalog` (the bundled fallback language lists)
- `kaikki` — `KaikkiCatalog` fetches and caches kaikki's edition and per-edition language lists; `LanguageCodeResolver` maps kaikki language names to `lang_code`. JavaFX-free, and the CLI never touches it
- `dump` — `DumpCatalog`, `DumpFile`: list and delete dumps
- `command` — `DownloadCommand`, `GenerateCommand`: the service orchestrators, independent of picocli and JavaFX, each with an `execute()` returning the produced path beside `Command.run()`
- `download`, `parse`, `render`, `write`, `write.opf`, `kindling` — one pipeline stage each, behind `DumpDownloader`, `DictionaryParser`, `DefinitionRenderer` and `DictionaryWriter`
- `model` — `LexiconEntry`, plus the Jackson records that map kaikki JSON

### Data directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz` from kaikki.org |
| `dictionaries/` | Final `.mobi` dictionary files |
| `dictionaries/intermediate/{base name}/` | One working directory per dictionary, holding the `.opf`, `-N.html`, `-toc.ncx` and `-cover.jpg` that build it |

Both front-ends read the same `preferences.properties`, so they share one dumps folder and one dictionaries folder, defaulting under `AppPaths.defaultDataDir()` (`~/Documents/wiktionary-to-kindle`). The CLI's `--dumps-dir` and `--dictionaries-dir` override them for one invocation; `CLI.Download.dumpsDir(Preferences)` and the matching pair on `CLI.Generate` are the single resolution point, and take the loaded preferences as an argument so tests never touch the real config file.

Paths are absolute. A bundled `.app` launches with `cwd=/`, so a relative path would resolve at the filesystem root. The `Preferences` compact constructor calls `toAbsolutePath().normalize()` on all three paths, which covers a hand-edited properties file and text typed into `PreferencesDialog` alike, since the dialog constructs the record directly. Keep the check there, and let downstream code take the paths as given.

`AppPaths` resolves four roles, all named `AppInfo.SLUG`. Unix-likes, **macOS included**, follow the XDG Base Directory spec; Windows gets sibling directories under `%LOCALAPPDATA%\wiktionary-to-kindle\`:

| Role | Unix | Windows | Holds |
|------|------|---------|-------|
| `configDir()` | `$XDG_CONFIG_HOME` → `~/.config` | `…\Config` | `preferences.properties` |
| `cacheDir()` | `$XDG_CACHE_HOME` → `~/.cache` | `…\Cache` | `kindling/<version>/<asset>`, `catalog/` |
| `stateDir()` | `$XDG_STATE_HOME` → `~/.local/state` | `…\State` | `logs/app.log` |
| `defaultDataDir()` | `$XDG_DOCUMENTS_DIR` → `~/Documents` | `%USERPROFILE%\Documents` | `dumps/`, `dictionaries/` |

The data dir sits under Documents rather than `$XDG_DATA_HOME` on purpose: dumps are multi-gigabyte and the `.mobi` has to be found and copied to a Kindle. It still resolves that folder the XDG way: the `XDG_DOCUMENTS_DIR` env var, then the same key in `$XDG_CONFIG_HOME/user-dirs.dirs` (a leading `$HOME` is expanded, any parse failure falls through), then `~/Documents`.

A relative value in any of these variables, `HOME` included, is ignored with a warning and treated as unset, as the XDG spec requires. `AppPaths.absolutePath` is the check, and it guards the Unix branch only: `Path.isAbsolute()` answers for the filesystem the JVM runs on, and would call a Windows `C:\local` relative on the Linux CI runner.

### Entry rendering

`HtmlDefinitionRenderer` takes the edition and turns one `WiktionaryEntry` into one HTML block, each part present only when the dump carries it:

```
<div><i>{part of speech}, {m. f. n. pl. …}</i> {IPA}</div>
<ol><li><i>({labels})</i> {gloss}<ul><li>{example} — <i>{translation}</i></li></ul><ol>{sub-senses}</ol></li>…</ol>
<div>&#160;</div>
<div>[{etymology}]</div>
<div>&#160;</div>
<div>≈ {synonyms}</div><div>≠ {antonyms}</div>
<div>&#160;</div>
<div><i>Forms:</i></div><ul><li>{tags}: {article} {form}</li>…</ul>
```

The four sections are separated by `<div>&#160;</div>`: a Kindle popup ignores CSS margins and stacks one `<div>` flush against the next, so a blank line has to be a line.

An entry with a single top-level sense gets a `<div>` in place of the one-item `<ol>`. An entry with no renderable gloss returns `Optional.empty()` and is skipped. Every piece of dump text goes through `HtmlText.escape`, which XML-escapes it and turns line breaks into `"; "`. The block is read in the edition's language, so captions are signs (`[…]`, `≈`, `≠`) rather than English words.

Each helper in `render` exists because kaikki's data needs it:

- **`SenseOutline`** — kaikki flattens a sub-sense into its own sense whose `glosses` list the parent's gloss first. The outline rebuilds the tree, keyed on gloss text; a flat rendering repeats every parent once per child.
- **`LabelSource`** — where a sense's labels (register, region, domain) come from depends on the edition. `en` already writes them as the prefix of each `raw_glosses` text, so that text replaces the gloss. `el` repeats every normalised tag in `raw_tags`, in Greek, so only `raw_tags` is shown. Every other edition keeps in `raw_tags` only what wiktextract did not recognise, so `raw_tags`, `tags` and `topics` are joined; the normalised ones are English whatever the edition. `form-of`, `alt-of`, `no-gloss` and `error-lua-exec` are structural and never shown.
- **`PartOfSpeech`** — `pos_title` is the edition's own name for it, minus a trailing homograph number. `en`, `el`, `ru` and `simple` carry none, so `el` and `ru` have a small table and the rest spell out kaikki's code.
- **`Headline`** — gender and number come from the entry's `tags`. The English edition has none at entry level and repeats them on each sense, so the tags every glossed sense shares are used instead. The first IPA is shown; an edition that leaves it bare gets slashes.
- **`Examples`** — the `MAX_PER_SENSE` shortest of a sense, in their order, since a popup is a few lines tall and some entries carry ten long quotations. `bold_text_offsets` count code points, as Python does, not UTF-16 units.
- **`Etymology`** — `etymology_text` is one string in `en`, `el` and `simple`, and `etymology_texts` a list everywhere else; the model accepts both. The English edition prints an "Etymology tree" of short lines ahead of the prose, which is dropped. Lines are kept and joined with `<br/>`, and a wiki list marker (`*`, `#`, `:`) at the start of one becomes `•`.

The English and Greek editions list rows tagged `inflection-template`, `table-tags` or `class` among an entry's `forms`. They name the inflection table's template, so they are kept out of the forms table and the lookup index.

The model records normalise a missing list to an empty one in their compact constructors, so the renderer never null-checks a list. Each keeps a constructor with its pre-enrichment arity, and Lombok `@With` adds the newer fields one at a time in tests.

Entries whose renderable senses all carry a `form_of` reference (Latin *suis* → "Datif pluriel de suus.") are flagged form-of-only through `RenderedEntry.formOfLemmas()`.

### Grouping and writing

`GenerateCommand` groups entries into a `TreeMap<String, List<LexiconEntry>>` in memory using `normaliseKey()`, then runs two post-passes before handing the map to the `DictionaryWriter`:

1. `foldFormOfEntries()` folds form-of-only lookup keys into their lemma's inflection index. On Kindle an exact headword match shadows the `<idx:iform>` index, so this makes an inflected-form lookup resolve straight to the lemma entry (issue #56). Folding is all-or-nothing per key: the key is dropped only when every entry under it is form-of with a lemma present in the map; a homograph with its own meaning, a missing lemma or a chain keeps all its entries.
2. `filterFormsCollidingWithHeadwords()` drops any inflection form whose normalised text still exists as a headword key.

`HtmlChapterRenderer` builds one MobiPocket HTML document per chunk of at most 10 000 entries, preserving Amazon's `<idx:entry>`/`<idx:orth>` markup and the `xmlns:mbp`/`xmlns:idx` declarations. Entries sharing a key are stacked under the first one's headword, with a line of em dashes between them. The line is drawn with characters because kindling ends an entry's lookup span at the first `<hr/>` after its start: an `<hr/>` anywhere inside an entry hides everything below it from the popup. A key groups spellings that differ only in case, so an entry spelled differently from the one above it gets its own bold line.

**Every output file is named from `DictionaryTitles.baseName(src, trg)`** → `w2k-dictionary-{src}-{trg}`, lowercased, the NCX and the cover included: several dictionaries share one output directory, and a fixed `toc.ncx` or `cover.jpg` would be overwritten by the next run. The `W2K` prefix (`AppInfo.DICTIONARY_PREFIX`) also leads the `<dc:title>`, the only thing Kindle shows in its dictionary settings list. The title names each code through `LanguageCatalog.displayNameFor`, as the pickers do, so a `name.<code>` override in `kaikki-editions.properties` reaches both.

`OpfDictionaryWriter` writes the `-N.html` chunks, a `-toc.ncx`, a `-cover.jpg` and the `.opf` (OPF 2.0, with `<DictionaryInLanguage>` / `<DictionaryOutLanguage>` in `<x-metadata>`), and returns the OPF path. `<DictionaryOutLanguage>` takes `Editions.languageOf(edition)`: `simple` names the Simple English site, and kindling-cli rejects it as a BCP47 code. `KindlingDictionaryConverter` composes it with the binary from `KindlingCliResolver.resolve()` and runs `kindling-cli build <opf> -o <mobi>`.

**The side-artefacts go in a sub-folder, the `.mobi` does not.** `IntermediateFiles.dirFor` puts them under `dictionaries/intermediate/w2k-dictionary-{src}-{trg}/`, one directory per dictionary, which keeps two editions' runs apart and makes cleanup a single recursive delete. kindling-cli resolves the OPF's hrefs relative to the OPF, so it never sees the difference. `Preferences.deleteIntermediateFiles` (default `false`) drops the working directory once the `.mobi` exists, and `IntermediateFiles.delete` removes the `intermediate` parent too when that leaves it empty. The delete runs **only after a zero exit code** and never fails the run: after a kindling-cli failure the files are the only way to diagnose it, and after a success a cleanup error is an annoyance, not a broken result.

`KindlingCliResolver.resolve()` tries, in order: the explicit `--kindling-cli` override → a PATH probe (`which`/`where`) → the cached binary at `<AppPaths.cacheDir()>/kindling/<version>/<assetName>` (SHA-256 verified) → a download through `KindlingDownloader`. The downloader fetches from GitHub Releases, verifies SHA-256 against the digests pinned in `kindling-release.properties` (or the GitHub API `digest` field for a non-default version), renames atomically and marks the file executable.

## Key Conventions

- **Naming**: the app has exactly two names, both in `AppInfo`. `DISPLAY_NAME` (`Wiktionary to Kindle`) is the window title, jpackage `--name`, `.app`/`.exe` and Scoop shortcut. `SLUG` (`wiktionary-to-kindle`) is the artifactId, CLI command, directories, release assets and brew/scoop package. Any third spelling is a bug. `--mac-package-name` is left unset so `CFBundleName` falls back to `--name`.
- **Progress** flows through `ProgressListener`, constructor-injected with a `NOOP` default, so each collaborator keeps its listener-free constructor. Emissions are throttled to roughly every 4 MB: the dump is millions of lines. Download progress needs `BodyHandlers.ofInputStream` plus a manual copy loop, because `ofFile` offers neither a byte callback nor a cancellation point. Parse progress counts the *compressed* stream, since a gzip member's uncompressed size is unknowable up front.
- **Cancellation** takes two mechanisms: interrupting the worker covers the download and parse loops, but the kindling stage blocks in `Process.waitFor()`, so `PipelineTask` tracks the process and destroys it in `cancelled()`.
- **Logging** uses SLF4J 2.x with Logback Classic, through Lombok's `@Slf4j` on every class. `logback.xml` configures the CLI console only; the GUI adds its appenders programmatically, so CLI output is untouched.
- **Subprocesses**: `KindlingDictionaryConverter.defaultRunner()` pumps merged stdout/stderr through SLF4J. Keep it that way: a windowed app has no terminal, so `inheritIO()` output vanishes.
- **Jackson**: model records carry `@JsonIgnoreProperties(ignoreUnknown = true)`, and the `ObjectMapper` is configured with `Nulls.AS_EMPTY`. One `ObjectReader` is reused across all lines.
- **Parser streaming**: `JsonlDictionaryParser.parse()` returns a lazy `Stream<WiktionaryEntry>` over `BufferedReader.lines()`. Callers close it with try-with-resources.
- **Download** uses `java.net.http.HttpClient` and an atomic `.part` rename, so a failed transfer leaves no corrupt dump. A HEAD request (30 s timeout) reads `last-modified` and `content-length` first, so the target filename and the skip-if-already-downloaded check resolve before any body transfer. The GET carries a 6 h ceiling, because a request-level timeout bounds the *whole* exchange and dumps are multi-gigabyte. The URL is per edition: `en` → `/dictionary/`, others → `/{lang}wiktionary/`.
- **Dump discovery** is filename-based: `DumpCatalog.latestFor` globs the dumps dir for `raw-wiktextract-data-{lang}-` and takes the lexicographically latest name, which ISO dates sort correctly. It deliberately bypasses `DumpFile.parse`, so a dump named `-unknown` (kaikki omitted `last-modified`) stays usable even though the dumps pane cannot list it.
- **kaikki language names resolve to a `lang_code` or are dropped, never guessed.** `JsonlDictionaryParser` filters on an exact `lang_code` match, so a wrong code silently yields an empty dictionary. `LanguageCodeResolver` looks in the bundled `kaikki-language-codes.properties` first, then `Locale.getDisplayLanguage` in the edition's own locale.
- **`LanguageCodeResolver.normalise` keeps letters in every script.** It strips accents through NFD and drops what is neither a letter nor a digit. An ASCII-only filter normalises every Chinese, Cyrillic, Japanese, Korean and Thai name to the empty string, and loses Latin letters NFD does not reduce (`język włoski`, `Tiếng Quảng Đông`).
- **The bundled properties files are UTF-8, so they are read through a `Reader`.** `Properties.load(InputStream)` decodes ISO-8859-1 whatever the file is, and some kaikki codes are not ASCII (`solrésol`, `bété`). `LanguageCodeResolver` and `LanguageCatalog` pass an explicit UTF-8 `InputStreamReader`. `Preferences` deliberately does not: it writes and reads its file with the `Properties` escaping rules, so it round-trips any character already, and switching one side alone breaks existing files.
- **Regenerating the alias table**: read the first JSONL line of each per-language dump, parse it as JSON and take the *top-level* `lang_code`; a nested one under `translations` matches first in a text search and is wrong. An HTTP `Range: bytes=0-400000` is enough. When the dump's filename does not match its directory, follow the `.jsonl` link in the language's `index.html`.
