# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` is a Java CLI tool that converts Wiktionary data into Kindle-compatible MOBI dictionaries. The pipeline is: **download** → **generate** (fetches `kindling-cli` on first run, then runs `kindling-cli build`).

Data source: [kaikki.org](https://kaikki.org) pre-extracted JSONL dumps, produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) with all Lua templates fully expanded. One dump per Wiktionary language edition.

## Build & Test

Requires Java 25 and Apache Maven. Produces a fat JAR via `maven-shade-plugin`.

```sh
mvn package          # compiles, runs tests, produces fat JAR
# Output: target/wiktionary-to-kindle-1.0.0.jar
```

JUnit 5 tests live in `src/test/java/edu/self/w2k/`.

## Running the JAR

The entry point is `edu.self.w2k.CLI`. Commands:

```sh
# Download kaikki.org dump to dumps/ (skips if a dump for that lang already exists)
java -jar target/wiktionary-to-kindle-1.0.0.jar download        # English (default)
java -jar target/wiktionary-to-kindle-1.0.0.jar download fr     # French edition
# dl is a short alias for download

# Generate Kindle dictionary from a downloaded dump.
# DUMP_LANG = which Wiktionary edition to read; WORD_LANG = ISO 639-1 filter.
# The latest dump matching DUMP_LANG in dumps/ is auto-discovered.
java -jar target/wiktionary-to-kindle-1.0.0.jar generate <DUMP_LANG> <WORD_LANG>
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en                        # auto-downloads kindling-cli on first run
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --kindling-version vX.Y.Z
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --kindling-cli /usr/local/bin/kindling-cli
# gen is a short alias for generate

# Help / version
java -jar target/wiktionary-to-kindle-1.0.0.jar --help
java -jar target/wiktionary-to-kindle-1.0.0.jar --version
```

## Architecture

### Package Structure

- **`edu.self.w2k`** — `CLI` — picocli root command + inner `Download` / `Generate` subcommand wiring classes
- **`edu.self.w2k.command`** — `DownloadCommand`, `GenerateCommand` — service orchestrators; `Command` interface
- **`edu.self.w2k.download`** — `KaikkiDumpDownloader` (HttpClient-based), `DumpDownloader` interface
- **`edu.self.w2k.write`** — `DictionaryWriter` interface, `DictionaryTitles` utility
- **`edu.self.w2k.write.opf`** — `OpfDictionaryWriter` (emits chunked `.html` + `.opf`), `HtmlChapterRenderer`
- **`edu.self.w2k.kindling`** — `KindlingDictionaryConverter` (composes `OpfDictionaryWriter` + kindling binary), `KindlingCliResolver` (override → PATH → cache → download), `KindlingDownloader` (GitHub releases API + SHA-256 verify), `KindlingPlatform` enum, `KindlingRelease` (loads the pinned version + per-platform digests from `src/main/resources/kindling-release.properties`), `XdgCachePaths`, `KindlingException`
- **`edu.self.w2k.parse`** — `JsonlDictionaryParser`, `DictionaryParser` interface
- **`edu.self.w2k.render`** — `HtmlDefinitionRenderer`, `DefinitionRenderer` interface
- **`edu.self.w2k.model`** — `LexiconEntry` plus Jackson-annotated records: `WiktionaryEntry`, `WiktionarySense`, `WiktionaryExample`, `WiktionaryForm`, `WiktionaryFormOf`

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz` from kaikki.org |
| `dictionaries/` | Final `.mobi` dictionary files, plus side-artefacts `.opf` and `-N.html` |

### Dictionary Output Format

`HtmlDefinitionRenderer` renders a `List<WiktionarySense>` into an HTML string:

```
<ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`. Entries with no renderable glosses return `Optional.empty()` and are skipped. Entries whose renderable senses all carry a `form_of` reference (e.g. Latin *suis* → "Datif pluriel de suus.") are flagged as form-of-only via `RenderedEntry.formOfLemmas()`.

`GenerateCommand` groups entries into a `TreeMap<String, List<LexiconEntry>>` in memory using `normaliseKey()`, then runs two post-passes before handing the map to the `DictionaryWriter`:

1. `foldFormOfEntries()` — drops each form-of-only entry whose lemma exists in the map and registers the entry's word as an inflection form on the lemma instead. On Kindle an exact headword match shadows the `<idx:iform>` index, so this makes an inflected-form lookup resolve straight to the full lemma entry (issue #56). Entries whose lemma is absent (or whose word is unusable as a lookup key) are kept as-is.
2. `filterFormsCollidingWithHeadwords()` — drops any inflection form whose normalised text still exists as a headword key.

`HtmlChapterRenderer` builds one MobiPocket HTML document (≤ 10 000 entries per chunk) preserving Amazon's `<idx:entry>`/`<idx:orth>` markup and `xmlns:mbp`/`xmlns:idx` namespace declarations.

`OpfDictionaryWriter` chunks the entry map, writes `dictionary-{src}-{trg}-N.html` files, then writes a `dictionary-{src}-{trg}.opf` OPF 2.0 manifest (with `<DictionaryInLanguage>` / `<DictionaryOutLanguage>` in `<x-metadata>`). Returns the OPF path.

`KindlingDictionaryConverter` composes `OpfDictionaryWriter` with a `kindling-cli` binary. It calls `KindlingCliResolver.resolve()` to obtain the binary, then runs `kindling-cli build <opf> -o <mobi>`. Output: `dictionaries/dictionary-{src}-{trg}.mobi` (plus `.opf` and `.html` side-artefacts).

`KindlingCliResolver.resolve()` tries in order: explicit `--kindling-cli` override → PATH probe (`which`/`where`) → cached binary at `<XdgCachePaths.kindlingCacheDir()>/<version>/<assetName>` (SHA-256 verified) → download via `KindlingDownloader`. `KindlingDownloader` fetches from GitHub Releases, verifies SHA-256 against the pinned digests from `kindling-release.properties` (loaded by `KindlingRelease`), or the GitHub API `digest` field for non-default versions, renames atomically, and marks the file executable.

## Key Conventions

- **CLI** uses [picocli](https://picocli.info/). `CLI.java` is the root `@Command`; `Download` and `Generate` are inner static subcommand classes that wire collaborators and delegate to the service-layer command classes.
- **Service classes** (`DownloadCommand`, `GenerateCommand`) use Lombok `@RequiredArgsConstructor` and are independent of picocli — they can be constructed directly in tests.
- **Logging** uses SLF4J 2.x with Logback Classic. `@Slf4j` (Lombok) is used on all classes.
- **Jackson** is used for JSONL parsing. Model records carry `@JsonIgnoreProperties(ignoreUnknown = true)`. `ObjectMapper` is configured with `Nulls.AS_EMPTY` so missing collection fields default to empty lists. The `ObjectReader` is reused across all lines for efficiency.
- **Parser streaming**: `JsonlDictionaryParser.parse()` returns a lazy `Stream<WiktionaryEntry>` backed by a `BufferedReader.lines()` pipeline. Callers must close the stream (use try-with-resources).
- **Download** uses `java.net.http.HttpClient` and an atomic `.part` file rename to avoid corrupt downloads on failure. A HEAD request (30 s timeout) reads `last-modified` and `content-length` first, so the target filename and the skip-if-already-downloaded check resolve before any body transfer; the GET that follows carries a deliberately generous 6 h ceiling because dumps are multi-gigabyte (a request-level timeout bounds the *whole* exchange, not just the headers). URL is computed per lang: `en` → `/dictionary/`, others → `/{lang}wiktionary/`.
- **Dump file path**: dumps are named `dumps/raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz`. `generate` resolves the file via `CLI.findLatestDump(lang)`, which globs the dumps dir for that prefix and picks the lexicographically latest filename (ISO date format sorts correctly). If no dump matches, generate exits 1.
