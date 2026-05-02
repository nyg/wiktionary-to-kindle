# Program Flow

## Download command

```mermaid
sequenceDiagram
    actor User
    participant CLI
    participant DownloadCommand
    participant KaikkiDumpDownloader
    participant kaikkiorg as kaikki.org
    participant FileSystem

    User->>CLI: java -jar ... download [lang] (or d)
    CLI->>Download: call()
    Download->>DownloadCommand: run()
    DownloadCommand->>KaikkiDumpDownloader: download()
    KaikkiDumpDownloader->>FileSystem: exists(raw-wiktextract-data-{lang}.jsonl.gz)?

    alt file already exists
        FileSystem-->>KaikkiDumpDownloader: true
        KaikkiDumpDownloader-->>DownloadCommand: skip
    else file not present
        FileSystem-->>KaikkiDumpDownloader: false
        KaikkiDumpDownloader->>kaikkiorg: GET /{lang}wiktionary/raw-wiktextract-data.jsonl.gz
        kaikkiorg-->>KaikkiDumpDownloader: 200 OK
        KaikkiDumpDownloader->>FileSystem: write .part file
        KaikkiDumpDownloader->>FileSystem: rename .part to jsonl.gz
        KaikkiDumpDownloader-->>DownloadCommand: done
    end

    DownloadCommand-->>Download: done
    Download-->>CLI: 0 (exit code)
    CLI-->>User: exit
```

## Generate command

```mermaid
sequenceDiagram
    actor User
    participant CLI
    participant GenerateCommand
    participant JsonlDictionaryParser
    participant HtmlDefinitionRenderer
    participant KindleOpfGenerator
    participant FileSystem

    User->>CLI: java -jar ... generate lang
    CLI->>Generate: call()
    Generate->>GenerateCommand: run()

    GenerateCommand->>JsonlDictionaryParser: parse(dumpFile, lang)
    JsonlDictionaryParser->>FileSystem: open raw-wiktextract-data-{lang}.jsonl.gz
    FileSystem-->>JsonlDictionaryParser: JSONL stream
    Note over JsonlDictionaryParser: filter by lang_code (lazy)
    JsonlDictionaryParser-->>GenerateCommand: Stream of WiktionaryEntry

    loop for each WiktionaryEntry
        GenerateCommand->>HtmlDefinitionRenderer: render(senses)
        HtmlDefinitionRenderer-->>GenerateCommand: Optional<HTML string>
        Note over GenerateCommand: skip entries with empty Optional<br/>normalise key, group into TreeMap
    end

    GenerateCommand->>KindleOpfGenerator: generate(grouped, lang, title, outputDir)

    loop for each chunk of up to 10000 entries
        KindleOpfGenerator->>FileSystem: write dictionary HTML file
    end

    KindleOpfGenerator->>FileSystem: write dictionary OPF file
    KindleOpfGenerator-->>GenerateCommand: done
    GenerateCommand-->>Generate: done
    Generate-->>CLI: 0 (exit code)
    CLI-->>User: exit

    Note over User,FileSystem: Manual step
    User->>FileSystem: kindlegen dictionary.opf
    FileSystem-->>User: dictionary.mobi
```
