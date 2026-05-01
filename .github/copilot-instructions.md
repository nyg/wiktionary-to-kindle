# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` is a Java CLI tool that converts Wiktionary data into Kindle-compatible MOBI dictionaries. The pipeline is: **download** → **generate** → **tab2opf** (Python, submodule) → **kindlegen** (external binary).

Data source: [kaikki.org](https://kaikki.org) pre-extracted JSONL (`raw-wiktextract-data.jsonl.gz`), produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) from the English Wiktionary with all Lua templates fully expanded.

## Build & Test

Requires Java 21 and Apache Maven. Produces a fat JAR via `maven-shade-plugin`.

```sh
mvn package          # compiles, runs tests, produces fat JAR
# Output: target/wiktionary-to-kindle-1.0.0.jar
```

JUnit 5 unit tests live in `src/test/java/edu/self/w2k/WiktionaryUtilTest.java`.

## Running the JAR

The entry point is `edu.self.w2k.CLI`. Commands:

```sh
# Download kaikki.org dump to dumps/ (skips if already exists)
java -jar target/wiktionary-to-kindle-1.0.0.jar download

# Generate dictionaries/lexicon.txt filtered by ISO 639-1 lang code
java -jar target/wiktionary-to-kindle-1.0.0.jar generate [lang]
```

## Architecture

### Package Structure

- **`edu.self.w2k`** — application code
  - `CLI` — argument parsing and command dispatch
- **`edu.self.w2k.util`** — utility classes
  - `DumpUtil` — downloads `raw-wiktextract-data.jsonl.gz` from kaikki.org using `HttpClient`
  - `WiktionaryUtil` — streams the JSONL.gz, filters by `lang_code`, writes `lexicon.txt`
- **`edu.self.w2k.model`** — Jackson-annotated POJOs
  - `WiktionaryEntry` — `word`, `lang_code`, `senses`
  - `WiktionarySense` — `glosses`, `examples`
  - `WiktionaryExample` — `text`

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `raw-wiktextract-data.jsonl.gz` from kaikki.org |
| `dictionaries/` | `lexicon.txt` (output of `generate`) and final OPF/MOBI files |
| `scripts/tab2opf/` | Python submodule for OPF generation |
| `scripts/kindlegen_*/` | Platform-specific KindleGen binaries |

### Dictionary Output Format

`WiktionaryUtil.generateDictionary` writes `dictionaries/lexicon.txt` where each line is:

```
word<TAB><ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`. Entries with no renderable glosses (e.g. `form_of`-only) are skipped.

## Key Conventions

- **Utility classes** are `final` with a private constructor that throws `RuntimeException` to prevent instantiation.
- **Logging** uses `java.util.logging.Logger` (JUL) throughout — not SLF4J or Log4j.
- **CLI argument parsing** is positional and minimal: index 0 = action, 1 = lang (default `"en"`). Missing args are silently swallowed via `ArrayIndexOutOfBoundsException`.
- **Jackson** is used for JSONL parsing. All model POJOs carry `@JsonIgnoreProperties(ignoreUnknown = true)` and null-safe getters; the `ObjectReader` is reused across all lines for efficiency.
- **Download** uses `java.net.http.HttpClient` with timeouts and an atomic `.part` file rename to avoid corrupt downloads on failure.
