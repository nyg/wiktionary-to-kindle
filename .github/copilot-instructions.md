# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` is a Java CLI tool that converts Wiktionary data into Kindle-compatible MOBI dictionaries. The pipeline is: **download** → **generate** → **kindlegen** (external binary).

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
# Download kaikki.org dump to dumps/ (skips if already exists)
java -jar target/wiktionary-to-kindle-1.0.0.jar download        # English (default)
java -jar target/wiktionary-to-kindle-1.0.0.jar download fr     # French edition
# d is a short alias for download

# Generate Kindle dictionary files filtered by ISO 639-1 lang code
java -jar target/wiktionary-to-kindle-1.0.0.jar generate [lang]

# Help / version
java -jar target/wiktionary-to-kindle-1.0.0.jar --help
java -jar target/wiktionary-to-kindle-1.0.0.jar --version
```

## Architecture

### Package Structure

- **`edu.self.w2k`** — `CLI` — picocli root command + inner `Download` / `Generate` subcommand wiring classes
- **`edu.self.w2k.command`** — `DownloadCommand`, `GenerateCommand` — service orchestrators; `Command` interface
- **`edu.self.w2k.download`** — `KaikkiDumpDownloader` (HttpClient-based), `DumpDownloader` interface
- **`edu.self.w2k.lexicon`** — `LexiconEntry` record
- **`edu.self.w2k.opf`** — `KindleOpfGenerator`, `OpfGenerator` interface
- **`edu.self.w2k.parse`** — `JsonlDictionaryParser`, `DictionaryParser` interface
- **`edu.self.w2k.render`** — `HtmlDefinitionRenderer`, `DefinitionRenderer` interface
- **`edu.self.w2k.model`** — Jackson-annotated records: `WiktionaryEntry`, `WiktionarySense`, `WiktionaryExample`

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data-{lang}.jsonl.gz` from kaikki.org |
| `dictionaries/` | Final OPF/MOBI files |
| `scripts/kindlegen_*/` | Platform-specific KindleGen binaries |

### Dictionary Output Format

`HtmlDefinitionRenderer` renders a `List<WiktionarySense>` into an HTML string:

```
<ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`. Entries with no renderable glosses (e.g. `form_of`-only) return `Optional.empty()` and are skipped.

`GenerateCommand` groups entries into a `TreeMap<String, List<LexiconEntry>>` in memory using `normaliseKey()`, then passes the map to `KindleOpfGenerator`.

## Key Conventions

- **CLI** uses [picocli](https://picocli.info/). `CLI.java` is the root `@Command`; `Download` and `Generate` are inner static subcommand classes that wire collaborators and delegate to the service-layer command classes.
- **Service classes** (`DownloadCommand`, `GenerateCommand`) use Lombok `@RequiredArgsConstructor` and are independent of picocli — they can be constructed directly in tests.
- **Logging** uses SLF4J 2.x with Logback Classic. `@Slf4j` (Lombok) is used on all classes.
- **Jackson** is used for JSONL parsing. Model records carry `@JsonIgnoreProperties(ignoreUnknown = true)`. `ObjectMapper` is configured with `Nulls.AS_EMPTY` so missing collection fields default to empty lists. The `ObjectReader` is reused across all lines for efficiency.
- **Parser streaming**: `JsonlDictionaryParser.parse()` returns a lazy `Stream<WiktionaryEntry>` backed by a `BufferedReader.lines()` pipeline. Callers must close the stream (use try-with-resources).
- **Download** uses `java.net.http.HttpClient` with timeouts and an atomic `.part` file rename to avoid corrupt downloads on failure. URL is computed per lang: `en` → `/dictionary/`, others → `/{lang}wiktionary/`.
- **Dump file path**: computed by `CLI.dumpFile(lang)` → `dumps/raw-wiktextract-data-{lang}.jsonl.gz`.
