# wiktionary-to-kindle

Converts a set of Wiktionary entries into an EPUB dictionary (or MOBI via Calibre) usable by a Kindle.

## How it works

1. A [kaikki.org](https://kaikki.org) pre-extracted Wiktionary JSONL dump is downloaded for the desired language edition. Dumps are produced weekly by [wiktextract](https://github.com/tatuylonen/wiktextract) and include all languages with Lua templates fully expanded.
2. The compressed JSONL is streamed and filtered by language. Each entry's senses are rendered into an HTML definition string and the result is grouped in-memory by normalised key.
3. By default, a single `.epub` file is generated in `dictionaries/`. The EPUB embeds Amazon's `<idx:entry>`/`<idx:orth>` markup and an `<x-metadata>` block, so it is both a valid EPUB and a real Kindle lookup dictionary when converted to MOBI.
4. Optionally, pass `--format mobi` to also produce a `.mobi` via [Calibre's](https://calibre-ebook.com) `ebook-convert`. Both `.epub` and `.mobi` are kept in `dictionaries/`.

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

```sh
# Default: produces dictionaries/dictionary-el-en.epub
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en

# Show help
java -jar target/wiktionary-to-kindle-1.0.0.jar generate --help
```

### 5. (Optional) Convert to MOBI via Calibre

Install [Calibre](https://calibre-ebook.com/download) and pass `--format mobi`. Both `.epub` and `.mobi` are written to `dictionaries/`.

```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --format mobi

# Explicit path to ebook-convert if it is not on your PATH
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el en --format mobi \
  --ebook-convert /Applications/calibre.app/Contents/MacOS/ebook-convert
```

### 6. Upload the file to your Kindle

Send `dictionary-el-en.mobi` (or `.epub` for newer Kindles) to your device via its Kindle email address, or drag and drop it as you would with any eBook.

## Screenshots

![Choosing your new default dictionary](https://i.imgur.com/aXAbTbx.jpg)
![Proof that it works](https://i.imgur.com/q3Tdxjo.jpg)
