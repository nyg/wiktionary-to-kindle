# wiktionary-to-kindle

Converts a set of Wiktionary entries into a MOBI dictionary usable by a Kindle.

## How it works

1. A Wiktionary [dump](https://dumps.wikimedia.org/backup-index.html) is downloaded.
2. [JWKTL](https://github.com/dkpro/dkpro-jwktl) is used to parse the downloaded XML and to create a database of the results.
3. Some Java code iterates on the wanted entries and generates a text file in which each line has the following format: `word<TAB>definition`.
4. [tab2opf](https://github.com/nyg/tab2opf) is used to convert the text file into a set of OPF and HTML files.
5. [KindleGen](https://www.amazon.com/gp/feature.html?ie=UTF8&docId=1000765211) is used to convert the above OPF and HTML files to a MOBI eBook that can be used as a dictionary by a Kindle.

## Generated dictionary files

* [English-English, 96MB](http://www.mediafire.com/file/uib98cjr19d0ddt/lexicon_en_en.mobi)
* [French-English, 27MB](http://www.mediafire.com/file/c3v5aijgp4q5ge3/lexicon_fr_en.mobi)
* [Greek-English, 6MB](http://www.mediafire.com/file/2nccw6ni32k4gmf/lexicon_gr_en.mobi)

## Getting it to work

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

### 3. Download the Wiktionary dump

Download the latest English Wiktionary dump. In the following command, the `en` and `latest` arguments are the defaults so they are not needed. Note that the specified language should be parsable by JWKTL (currently it only supports `en`, `de`, `ru`). To specify another date use the `YYYYMMDD` format. The dump downloaded is `pages-articles.xml.bz2`.

```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar download en latest
```

### 4. Parse the dump

The dump must now be parsed using the following command (as mentioned above, `en` and `latest` are not needed).

```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar parse en latest
```

### 5. Generate the dictionary file

Time has now come to generate the dictionary text file. As said before, the default language is `en` but here it is possible to select only the entries of a particular language. For example, if we want only the Greek entries (`el`) of the English Wiktionary, the following command is to be used:

```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar generate el
```

### 6. Generate an OPF file from the dictionary file

The dictionary file has been generated in `dictionaries/lexicon.txt`. To convert it into an OPF file, execute the commands below. Python 3 is required. The `-s` and `-t` options are the source and target languages respectively.

```sh
cd dictionaries
python ../scripts/tab2opf/tab2opf.py -s el -t en lexicon.txt
```

### 7. Convert the OPF into a MOBI eBook

Convert the OPF file into a MOBI eBook using KindleGen.

```sh
# Linux
../scripts/kindlegen_linux/kindlegen lexicon.opf

# macOS
../scripts/kindlegen_mac/kindlegen lexicon.opf

# Windows
..\scriptgs\kindlegen_windows\kindlegen.exe lexicon.opf
```

### 8. Upload the file to your Kindle

If all went well, you should now have the `lexicon.mobi` file in your possession. You can either send it to your Kindle via its Kindle email address, or drag and drop it as you would with another eBook.

## TODO

1. Rewrite tab2opf in Java (?)
2. tab2opf should output EPUB v2 or v3 if supported by kindlegen
	* Finding how to properly structure the OPF entries (inflected terms, etc.).
3. Convert Wiktionary templates into HTML or find a HTML Wiktionary dump
4. Improving JWKTL's parsing abilities (template support, etc.).

## Screenshots

![Choosing your new default dictionary](https://i.imgur.com/aXAbTbx.jpg)
![Proof that it works](https://i.imgur.com/q3Tdxjo.jpg)
