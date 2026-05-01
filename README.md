# wiktionary-to-kindle

Converts a set of Wiktionary entries into a MOBI dictionary usable by a Kindle.

## How it works

1. The [kaikki.org](https://kaikki.org) pre-extracted Wiktionary JSONL dump is downloaded. It is produced weekly from the English Wiktionary by [wiktextract](https://github.com/tatuylonen/wiktextract) and includes all languages with Lua templates fully expanded.
2. Some Java code streams the compressed JSONL, filters entries for the chosen language, and generates a text file in which each line has the following format: `word<TAB>definition`.
3. [tab2opf](https://github.com/nyg/tab2opf) is used to convert the text file into a set of OPF and HTML files.
4. [KindleGen](https://www.amazon.com/gp/feature.html?ie=UTF8&docId=1000765211) is used to convert the OPF and HTML files to a MOBI eBook that can be used as a dictionary by a Kindle.

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

### 1. Clone the repository & the `tab2opf` submodule

```sh
git clone https://github.com/nyg/wiktionary-to-kindle.git
git submodule update --init --recursive
```

### 2. Build the project

[Apache Maven](https://maven.apache.org) is required.

```sh
mvn package
```

### 3. Download the kaikki.org dump

Downloads `raw-wiktextract-data.jsonl.gz` (~1–2 GB compressed) from kaikki.org to `dumps/`. If the file already exists, the download is skipped.

```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar download
```

### 4. Generate the dictionary file

Filter entries for the language of your choice using its [ISO 639-1](https://en.wikipedia.org/wiki/ISO_639-1) code (e.g. `el` for Greek, `fr` for French, `de` for German). The dump is streamed and decompressed on the fly — no extra disk space needed.

```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el
```

### 5. Generate an OPF file from the dictionary file

The dictionary file has been generated in `dictionaries/lexicon.txt`. To convert it into an OPF file, execute the commands below. Python 3 is required. The `-s` and `-t` options are the source and target languages respectively.

```sh
cd dictionaries
python ../scripts/tab2opf/tab2opf.py -s el -t en -o "Greek–English Dictionary" lexicon.txt
```

### 6. Convert the OPF into a MOBI eBook

Convert the OPF file into a MOBI eBook using KindleGen.

```sh
# Linux
../scripts/kindlegen_linux/kindlegen dictionary-el-en.opf

# macOS
../scripts/kindlegen_mac/kindlegen dictionary-el-en.opf

# Windows
..\scriptgs\kindlegen_windows\kindlegen.exe dictionary-el-en.opf
```

### 7. Upload the file to your Kindle

If all went well, you should now have the `dictionary-el-en.mobi` file in your possession. You can either send it to your Kindle via its Kindle email address, or drag and drop it as you would with another eBook.

## Known limitations

* Entries that are purely `form_of` / `alt_of` references (no `glosses` in the kaikki data) are skipped. Kindle lookups for some inflected forms may therefore not find an entry. Base forms are always included.

## TODO

1. Rewrite tab2opf in Java (?)
2. tab2opf should output EPUB v2 or v3 if supported by kindlegen
	* Finding how to properly structure the OPF entries (inflected terms, etc.).

## Screenshots

![Choosing your new default dictionary](https://i.imgur.com/aXAbTbx.jpg)
![Proof that it works](https://i.imgur.com/q3Tdxjo.jpg)
