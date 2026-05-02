# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` is a Java CLI tool that converts Wiktionary data into Kindle-compatible EPUB (and optionally MOBI) dictionaries. The pipeline is: **download** → **generate** (→ **ebook-convert** for MOBI, optional).

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
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --format mobi   # requires Calibre
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --format mobi \
  --ebook-convert /Applications/calibre.app/Contents/MacOS/ebook-convert
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
- **`edu.self.w2k.write`** — `DictionaryWriter` interface, `OutputFormat` enum (`EPUB`, `MOBI`), `DictionaryTitles` utility
- **`edu.self.w2k.write.epub`** — `EpubDictionaryWriter`, `XhtmlChapterRenderer`, `OpfPostProcessor`
- **`edu.self.w2k.write.mobi`** — `MobiDictionaryWriter` (composes `EpubDictionaryWriter` + `EbookConverter`)
- **`edu.self.w2k.convert`** — `EbookConverter` interface, `CalibreEbookConverter`, `EbookConverterNotFoundException`
- **`edu.self.w2k.parse`** — `JsonlDictionaryParser`, `DictionaryParser` interface
- **`edu.self.w2k.render`** — `HtmlDefinitionRenderer`, `DefinitionRenderer` interface
- **`edu.self.w2k.model`** — `LexiconEntry` plus Jackson-annotated records: `WiktionaryEntry`, `WiktionarySense`, `WiktionaryExample`

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz` from kaikki.org |
| `dictionaries/` | Final `.epub` (and `.mobi`) dictionary files |

### Dictionary Output Format

`HtmlDefinitionRenderer` renders a `List<WiktionarySense>` into an HTML string:

```
<ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`. Entries with no renderable glosses (e.g. `form_of`-only) return `Optional.empty()` and are skipped.

`GenerateCommand` groups entries into a `TreeMap<String, List<LexiconEntry>>` in memory using `normaliseKey()`, then passes the map to the `DictionaryWriter`.

`XhtmlChapterRenderer` builds one XHTML chapter document (≤ 10 000 entries per chunk) preserving Amazon's `<idx:entry>`/`<idx:orth>` markup and `xmlns:mbp`/`xmlns:idx` namespace declarations, so the EPUB chapters work as Kindle lookup dictionary entries.

`EpubDictionaryWriter` assembles chapters into an EPUB via `epub4j-core`, then `OpfPostProcessor` injects the `<x-metadata><DictionaryInLanguage>/<DictionaryOutLanguage>` block into the OPF (required for Kindle dictionary mode). Output: `dictionaries/dictionary-{src}-{trg}.epub`.

`MobiDictionaryWriter` wraps `EpubDictionaryWriter` and pipes its output through `CalibreEbookConverter` (`ebook-convert`). Both `.epub` and `.mobi` are kept in `dictionaries/`.

`CalibreEbookConverter.findEbookConvert(Optional<Path>)` resolves the `ebook-convert` binary: explicit override → `which`/`where` on PATH → OS-specific common install paths (`/Applications/calibre.app/…`, `/usr/bin/…`, `C:\Program Files\Calibre2\…`). Throws `EbookConverterNotFoundException` if not found.

## Key Conventions

- **CLI** uses [picocli](https://picocli.info/). `CLI.java` is the root `@Command`; `Download` and `Generate` are inner static subcommand classes that wire collaborators and delegate to the service-layer command classes.
- **Service classes** (`DownloadCommand`, `GenerateCommand`) use Lombok `@RequiredArgsConstructor` and are independent of picocli — they can be constructed directly in tests.
- **Logging** uses SLF4J 2.x with Logback Classic. `@Slf4j` (Lombok) is used on all classes.
- **Jackson** is used for JSONL parsing. Model records carry `@JsonIgnoreProperties(ignoreUnknown = true)`. `ObjectMapper` is configured with `Nulls.AS_EMPTY` so missing collection fields default to empty lists. The `ObjectReader` is reused across all lines for efficiency.
- **Parser streaming**: `JsonlDictionaryParser.parse()` returns a lazy `Stream<WiktionaryEntry>` backed by a `BufferedReader.lines()` pipeline. Callers must close the stream (use try-with-resources).
- **Download** uses `java.net.http.HttpClient` with timeouts and an atomic `.part` file rename to avoid corrupt downloads on failure. URL is computed per lang: `en` → `/dictionary/`, others → `/{lang}wiktionary/`.
- **Dump file path**: dumps are named `dumps/raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz`. `generate` resolves the file via `CLI.findLatestDump(lang)`, which globs the dumps dir for that prefix and picks the lexicographically latest filename (ISO date format sorts correctly). If no dump matches, generate exits 1.
