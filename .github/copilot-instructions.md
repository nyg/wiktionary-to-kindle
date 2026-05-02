# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` is a Java CLI tool that converts Wiktionary data into Kindle-compatible MOBI dictionaries. The pipeline is: **download** → **generate** → **kindlegen** (external binary).

Data source: [kaikki.org](https://kaikki.org) pre-extracted JSONL (`raw-wiktextract-data.jsonl.gz`), produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) from the English Wiktionary with all Lua templates fully expanded.

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
java -jar target/wiktionary-to-kindle-1.0.0.jar download   # or: dl

# Generate dictionaries/lexicon.txt filtered by ISO 639-1 lang code
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
- **`edu.self.w2k.lexicon`** — `TsvLexiconWriter`, `TsvLexiconReader`, `LexiconEntry`; writer/reader interfaces
- **`edu.self.w2k.opf`** — `KindleOpfGenerator`, `OpfGenerator` interface
- **`edu.self.w2k.parse`** — `JsonlDictionaryParser`, `DictionaryParser` interface
- **`edu.self.w2k.render`** — `HtmlDefinitionRenderer`, `DefinitionRenderer` interface
- **`edu.self.w2k.model`** — Jackson-annotated POJOs: `WiktionaryEntry`, `WiktionarySense`, `WiktionaryExample`

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data.jsonl.gz` from kaikki.org |
| `dictionaries/` | `lexicon.txt` (output of `generate`) and final OPF/MOBI files |
| `scripts/kindlegen_*/` | Platform-specific KindleGen binaries |

### Dictionary Output Format

`GenerateCommand` writes `dictionaries/lexicon.txt` where each line is:

```
word<TAB><ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`. Entries with no renderable glosses (e.g. `form_of`-only) are skipped.

## Key Conventions

- **CLI** uses [picocli](https://picocli.info/). `CLI.java` is the root `@Command`; `Download` and `Generate` are inner static subcommand classes that wire collaborators and delegate to the service-layer command classes.
- **Service classes** (`DownloadCommand`, `GenerateCommand`) use Lombok `@RequiredArgsConstructor` and are independent of picocli — they can be constructed directly in tests.
- **Logging** uses SLF4J 2.x with Logback Classic. `@Slf4j` (Lombok) is used on all classes.
- **Jackson** is used for JSONL parsing. All model POJOs carry `@JsonIgnoreProperties(ignoreUnknown = true)`. The `ObjectReader` is reused across all lines for efficiency.
- **Download** uses `java.net.http.HttpClient` with timeouts and an atomic `.part` file rename to avoid corrupt downloads on failure.
- **Path constants** (`DUMP_FILE`, `LEXICON_FILE`, `DICTIONARIES_DIR`) are defined as `static final` in `CLI.java` — single source of truth.
