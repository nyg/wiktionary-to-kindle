# Copilot Instructions

## Project Overview

`wiktionary-to-kindle` is a Java CLI tool that converts Wiktionary XML dumps into Kindle-compatible MOBI dictionaries. The pipeline is: **download** → **parse** → **generate** → **tab2opf** (Python, submodule) → **kindlegen** (external binary).

## Build

Requires Java 21 and Apache Maven. Produces a fat JAR via `maven-shade-plugin`.

```sh
mvn package
# Output: target/wiktionary-to-kindle-1.0.0.jar
```

There are no tests and no linter configured.

## Running the JAR

The entry point is `edu.self.w2k.CLI`. Commands:

```sh
# Download Wiktionary dump (defaults: lang=en, date=latest)
java -jar target/wiktionary-to-kindle-1.0.0.jar download [lang] [date]

# Parse XML dump into JWKTL database (stored in db/)
java -jar target/wiktionary-to-kindle-1.0.0.jar parse [lang] [date]

# Generate dictionaries/lexicon.txt from the database
java -jar target/wiktionary-to-kindle-1.0.0.jar generate [lang]
```

## Architecture

### Package Structure

- **`edu.self.w2k`** — application code
  - `CLI` — argument parsing and command dispatch
  - `Sandbox` — scratch class for ad-hoc JWKTL experimentation (not production)
- **`edu.self.w2k.util`** — utility classes (`DumpUtil`, `WiktionaryUtil`, `URLUtil`)
- **`de.tudarmstadt.ukp.jwktl`** — **local patches to the JWKTL library** (see below)

### JWKTL Patches

The `src/main/java/de/tudarmstadt/ukp/jwktl/` tree contains source files that **override classes from the `dkpro-jwktl` Maven dependency**. Because `maven-shade-plugin` merges all classes into one JAR, the local versions take precedence over the dependency's versions at runtime. These patches add template parsing support (`ENTemplateHandler`, `LabelTemplateParser`, `UXTemplateParser`, `QualifierTemplateParser`) that the upstream library lacks.

When modifying JWKTL behavior, edit these local override files rather than the dependency.

### Data Directories

| Directory | Purpose |
|-----------|---------|
| `dumps/`  | Downloaded `.bz2` archives and extracted `.xml` files |
| `db/`     | JWKTL BerkeleyDB database (output of `parse`) |
| `dictionaries/` | `lexicon.txt` (output of `generate`) and final OPF/MOBI files |
| `scripts/tab2opf/` | Python submodule for OPF generation |
| `scripts/kindlegen_*/` | Platform-specific KindleGen binaries |

### Dictionary Output Format

`WiktionaryUtil.generateDictionary` writes `dictionaries/lexicon.txt` where each line is:

```
word<TAB><ol><li><span>gloss</span><ul><li>example</li></ul></li>...</ol>
```

Gloss and example text is XML-escaped with `StringEscapeUtils.escapeXml10`; internal newlines are replaced with `"; "`.

## Key Conventions

- **Utility classes** are `final` with a private constructor that throws `RuntimeException` to prevent instantiation.
- **Logging** uses `java.util.logging.Logger` (JUL) throughout — not SLF4J or Log4j.
- **CLI argument parsing** is positional and minimal: index 0 = action, 1 = lang (default `"en"`), 2 = date (default `"latest"`). Missing args are silently swallowed via `ArrayIndexOutOfBoundsException`.
- **XML entity limits** are disabled at startup (`entityExpansionLimit=0`) to handle large Wiktionary dumps without parser errors.
- The `OVERWRITE_EXISTING_DB = true` constant in `DumpUtil` means re-running `parse` always replaces the existing database.
- Language codes passed to `generate` must be valid JWKTL codes (e.g., `el`, `fr`, `en`); JWKTL itself only parses `en`, `de`, and `ru` dump formats.
