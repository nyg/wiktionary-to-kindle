# wiktionary-to-kindle

Converts a set of Wiktionary entries to a .mobi dictionary usable by a Kindle.

## How it works

1. A Wiktionary [dump](https://dumps.wikimedia.org/backup-index.html) is downloaded.
2. [JWKTL](https://github.com/dkpro/dkpro-jwktl) is used to parse the downloaded XML and to create a database of the results.
3. Some Java code iterates of the wanted entries and generates a text file in which each line has the following format: `word<TAB>definition`.
4. [tab2opf](https://github.com/apeyser/tab2opf) is used to convert the text file into a set of OPF and HTML files.
5. [KindleGen](https://www.amazon.com/gp/feature.html?ie=UTF8&docId=1000765211) is used to convert the above OPF and HTML files to a MOBI eBook that can be used as a dictionary by a Kindle.