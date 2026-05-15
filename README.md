# wiktionary-to-kindle

Converts a set of Wiktionary entries into a MOBI dictionary usable by a Kindle.

## How it works

1. A [kaikki.org](https://kaikki.org) pre-extracted Wiktionary JSONL dump is downloaded for the desired language edition. Dumps are produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) and include all languages with Lua templates fully expanded.
2. The compressed JSONL is streamed and filtered by language. Each entry's senses are rendered into an HTML definition string, its inflected forms are collected as Kindle lookup targets, and the result is grouped in-memory by normalised key.
3. Chunked MobiPocket HTML files and an OPF manifest are written to `dictionaries/`.
4. On first run, [kindling-cli](https://github.com/ciscoriordan/kindling) is downloaded automatically and cached under `~/.cache/wiktionary-to-kindle/kindling/` (Linux/macOS) or `%LOCALAPPDATA%\wiktionary-to-kindle\Cache\kindling\` (Windows). The binary is SHA-256 verified before use.
5. `kindling-cli build` converts the OPF to a `.mobi` Kindle dictionary in `dictionaries/`.

## Inflected forms

Wiktionary entries on kaikki include a `forms` array listing every inflected form of the lemma — plurals, declension cases, conjugations, gender-agreement forms. `wiktionary-to-kindle` exposes those forms in two ways:

* **Lookup index** — every form becomes a tap-to-lookup target on Kindle via `<idx:iform>` markup. Looking up `συντρόφους` (the accusative plural) resolves to the lemma `σύντροφος`. Emitted for every part of speech, including verbs.
* **Visible paradigm table** — for non-verb entries, a small table of the form `{tag-abbrev}: {article} {form}` is appended to the definition. Verb entries skip the table (Greek, Romance and Slavic verbs have 50–200+ forms, which would dwarf the definition). A `forms.size() > 30` safety net also skips the table for pathological non-verb cases.

Gender-equivalent cross-references (e.g. `συντρόφισσα` listed as a feminine equivalent of `σύντροφος`, or `ingénieure` listed under `ingénieur`) are filtered out of the **lookup index** by a language-agnostic post-pass: any form whose normalised text already exists as a standalone headword in the same dump is dropped from the iform list, so long-press on `συντρόφισσα` resolves to its own entry instead of being shadowed by the lemma. The **visible "Forms:" table** in each entry's HTML body is untouched — readers still see the full paradigm (e.g. `ingénieure`, `ingénieurs`) under the lemma.

See [`docs/inflection-support.md`](docs/inflection-support.md) for the full design.

## Examples of generated dictionaries

* [English-English (96MB)](http://www.mediafire.com/file/uib98cjr19d0ddt/lexicon_en_en.mobi)
* [French-English (27MB)](http://www.mediafire.com/file/c3v5aijgp4q5ge3/lexicon_fr_en.mobi)
* [Greek-English (6MB)](http://www.mediafire.com/file/2nccw6ni32k4gmf/lexicon_gr_en.mobi)

## Helpful documentation

* [International Digital Publishing Forum](http://idpf.org)
* [EPUB 2 standard](http://idpf.org/epub/201)
* [EPUB 3 standard](https://www.w3.org/community/epub3/)
* [EPUB Dictionaries and Glossaries 1.0](http://idpf.org/epub/dict/)
* [EPUB – Wikipedia](https://en.wikipedia.org/wiki/EPUB)
* [Creating Dictionaries – Kindle Publishing Guidelines](https://kdp.amazon.com/en_US/help/topic/G2HXJS944GL88DNV)

## How to generate your own dictionary

### 1. Clone the repository

```sh
git clone https://github.com/nyg/wiktionary-to-kindle.git
```

### 2. Build the project

[Apache Maven](https://maven.apache.org) and Java 25 are required.

```sh
mvn package
```

### 3. Download the kaikki.org dump

Downloads `raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz` (~1–2 GB compressed for English) from kaikki.org to `dumps/`. If a dump for that language already exists, the download is skipped. The default language is English (`en`).

```sh
# Download English edition (default)
java -jar target/wiktionary-to-kindle-1.0.0.jar download

# Download a specific edition (e.g. French)
java -jar target/wiktionary-to-kindle-1.0.0.jar download fr

# dl is a short alias for download
java -jar target/wiktionary-to-kindle-1.0.0.jar dl fr

# Show help
java -jar target/wiktionary-to-kindle-1.0.0.jar --help
java -jar target/wiktionary-to-kindle-1.0.0.jar download --help
```

### 4. Generate the dictionary

Filter entries for the language of your choice using its [ISO 639-1](https://en.wikipedia.org/wiki/ISO_639-1) code (e.g. `el` for Greek, `fr` for French, `de` for German). The dump is streamed and decompressed on the fly — no extra disk space needed.

On first run, `kindling-cli` is downloaded automatically from GitHub Releases and cached under `~/.cache/wiktionary-to-kindle/kindling/` (Linux/macOS) or `%LOCALAPPDATA%\wiktionary-to-kindle\Cache\kindling\` (Windows). Subsequent runs use the cached binary.

```sh
# Produces dictionaries/dictionary-el-en.mobi (plus .opf and .html side-artefacts)
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en

# Pin a specific kindling release (default: v0.14.5)
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --kindling-version v0.14.5

# Use a pre-installed kindling-cli binary (skips download)
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --kindling-cli /usr/local/bin/kindling-cli

# Show help
java -jar target/wiktionary-to-kindle-1.0.0.jar generate --help
```

### 5. Upload the file to your Kindle

Send `dictionaries/dictionary-el-en.mobi` to your device via its Kindle email address, or drag and drop it as you would with any eBook.

## Screenshots

![Choosing your new default dictionary](https://i.imgur.com/aXAbTbx.jpg)
![Proof that it works](https://i.imgur.com/q3Tdxjo.jpg)
