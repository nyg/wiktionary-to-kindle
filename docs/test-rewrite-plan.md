# Test Suite Rewrite Plan

## Context

The current test suite in `src/test/java/edu/self/w2k/` was written using only JUnit Jupiter primitives (`assertEquals`, `assertTrue`, `assertThrows`), with hand-written stub classes instead of a mocking framework. Several test files (`GenerateCommandTest`, `JsonlDictionaryParserTest`, `OpfDictionaryWriterTest`) read or write real files end-to-end and behave more like integration tests than unit tests.

The goal is a **complete rewrite** producing a uniform, modern unit-test suite that:
- uses Mockito for mocks (no hand-written stubs);
- uses AssertJ for fluent assertions;
- treats each production class in isolation (collaborators mocked or replaced with `@TempDir` fixtures for filesystem-bound classes);
- follows a strict naming + structure convention (`should_..._when_...` + `// Given // When // Then` blocks).

This plan is to be implemented by Sonnet 4.6.

## Hard constraints (from the user)

1. **Mockito** with `@ExtendWith(MockitoExtension.class)` — no manual stubs.
2. The variable holding the class under test must be named **`unit`**.
3. **AssertJ** for all assertions, using current idioms (`assertThat(...)`, chained matchers like `containsExactly`, `hasSize`, `isInstanceOf`, `extracting`, etc.).
4. **JUnit 6** (`junit-jupiter` 6.0.3 — already on the classpath).
5. Test method name pattern: **`should_<expectation>_when_<condition>`**.
6. Each test body is split with **`// Given`**, **`// When`**, **`// Then`** comments.
7. Tests should be **straightforward** — no clever helper hierarchies.
8. **Happy path + main alternative path only** — no exhaustive branch coverage.
9. **Never use `var`** — always declare explicit types.
10. Unit tests only — no integration or e2e tests.

## Dependencies to add to `pom.xml`

Add to `<properties>` block:
```xml
<assertj-core.version>3.27.7</assertj-core.version>
<mockito.version>5.21.0</mockito.version>
```
(use the latest stable releases at implementation time)

Add to `<dependencies>` block (both `<scope>test</scope>`):
```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>${assertj-core.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>${mockito.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>${mockito.version}</version>
    <scope>test</scope>
</dependency>
```

## Production refactor required

`src/main/java/edu/self/w2k/download/KaikkiDumpDownloader.java` currently builds its `HttpClient` inside `download()`, making the method untestable without real network. Mirror the pattern already used in `KindlingDownloader` (public constructor + package-private constructor for tests):

- Keep the public constructor `public KaikkiDumpDownloader(String lang)` that internally builds the default `HttpClient`.
- Add a package-private constructor `KaikkiDumpDownloader(String lang, HttpClient httpClient)` and store the client in a final field.
- Have `download()` use that field instead of building a fresh client.

Also (recommended) extract `DUMPS_DIR` so it can be passed in for tests, **or** scope file-system writes inside an injectable `Path baseDir` second constructor parameter. Simplest: accept a `Path dumpsDir` alongside `HttpClient` in the package-private constructor.

No other production changes are required.

## Files to delete

Delete every existing test file under `src/test/java/edu/self/w2k/`:

```
src/test/java/edu/self/w2k/command/GenerateCommandTest.java
src/test/java/edu/self/w2k/kindling/KindlingCliResolverTest.java
src/test/java/edu/self/w2k/kindling/KindlingDictionaryConverterTest.java
src/test/java/edu/self/w2k/kindling/KindlingDownloaderTest.java
src/test/java/edu/self/w2k/kindling/KindlingPlatformTest.java
src/test/java/edu/self/w2k/kindling/XdgCachePathsTest.java
src/test/java/edu/self/w2k/parse/JsonlDictionaryParserTest.java
src/test/java/edu/self/w2k/render/HtmlDefinitionRendererTest.java
src/test/java/edu/self/w2k/write/DictionaryTitlesTest.java
src/test/java/edu/self/w2k/write/opf/HtmlChapterRendererTest.java
src/test/java/edu/self/w2k/write/opf/OpfDictionaryWriterTest.java
```

## Skipped from testing (deliberate)

These classes have no logic worth covering in a unit suite:

- `CLI` — picocli wiring + `main`. Hard-codes `Path.of("dumps")`; leave as integration territory.
- `KindlingException` — exception class with two delegating constructors.
- `KindlingRelease` — static data holder.
- All records: `LexiconEntry`, `WiktionaryEntry`, `WiktionarySense`, `WiktionaryExample`.
- All interfaces: `Command`, `DumpDownloader`, `DictionaryParser`, `DefinitionRenderer`, `DictionaryWriter`, `HttpFetcher`.
- `KindlingDictionaryConverter.defaultRunner()` — wraps `ProcessBuilder` directly; not unit-testable without spawning a process.

## Test class template

Every new test file follows this exact skeleton (no exceptions, no var):

```java
package edu.self.w2k.<package>;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class <ClassName>Test {

    @Mock
    private <Collaborator> collaborator;

    @InjectMocks
    private <ClassName> unit;          // when constructor injection works

    // OR, when collaborators must be wired manually (e.g. value params):
    // private <ClassName> unit;
    // @BeforeEach void setUp() { unit = new <ClassName>(...); }

    @Test
    void should_<expectation>_when_<condition>() {
        // Given
        ...

        // When
        ...

        // Then
        assertThat(...)...;
    }
}
```

When the production class is package-private (e.g. `HtmlChapterRenderer`) the test must live in the matching package and stay package-private.

## New test files (per package)

### `src/test/java/edu/self/w2k/command/`

#### `DownloadCommandTest.java`
- Collaborators: `@Mock DumpDownloader downloader;` `@InjectMocks DownloadCommand unit;`
- `should_invoke_downloader_when_run_is_called` — call `unit.run()`; verify `downloader.download()` was called exactly once.

#### `GenerateCommandTest.java`
- Collaborators: `@Mock DictionaryParser parser; @Mock DefinitionRenderer renderer; @Mock DictionaryWriter writer;` plus `@TempDir Path tmp;` for the dump path argument (file does not need real content because the parser is mocked).
- `unit` built manually in `@BeforeEach` because `GenerateCommand` has many primitive params.
- `should_group_entries_and_write_dictionary_when_run` — happy path:
  - stub `parser.parse(any(), eq(srcLang))` to return `Stream.of(new WiktionaryEntry("Apple", srcLang, List.of(...)), new WiktionaryEntry("apple", srcLang, ...), new WiktionaryEntry("banana", srcLang, ...))`;
  - stub `renderer.render(any())` to return `Optional.of("<def>")`;
  - call `unit.run()`;
  - capture the `TreeMap<String, List<LexiconEntry>>` argument passed to `writer.write(...)` via `ArgumentCaptor`;
  - assert: keys are normalised + sorted (`assertThat(captured).containsKeys("apple", "banana")`); duplicate keys merged into a list (`assertThat(captured.get("apple")).hasSize(2)`); writer received the right `srcLang`, `trgLang`, `title`, `outputDir`.
- `should_skip_entries_when_renderer_returns_empty` — alternative path:
  - stub renderer to return `Optional.empty()` for one of two entries;
  - assert the captured map only contains the surviving entry.

For `GenerateCommand.normaliseKey(String)` (package-private static, in the same test class as `@Nested` block or two extra `@Test` methods):
- `should_lowercase_and_strip_when_normalising_key` — `"  Hello  "` → `"hello"`.
- `should_replace_quotes_and_escape_angle_brackets_when_normalising_key` — `"\"<a>\""` → `"'\\<a\\>'"`.

### `src/test/java/edu/self/w2k/download/`

#### `KaikkiDumpDownloaderTest.java`
- Collaborators: `@Mock HttpClient httpClient;` `@TempDir Path tmp;` (passed as the dumps directory through the new package-private constructor).
- `should_save_dump_with_date_when_download_succeeds` — happy path:
  - mock an `HttpResponse<Path>` returning status 200, header `last-modified: Fri, 01 May 2026 10:00:00 GMT` (**note**: JDK 25 `RFC_1123_DATE_TIME` does not parse leniently when the day-of-week mismatches the date, so use the correct weekday — May 1 2026 is a Friday), body path = part file (pre-create the part file in `tmp`);
  - use `doAnswer` to write an empty byte array to `partPath` (with `Files.write`) and return the mock response — `Files.createFile` would fail if `BodyHandlers.ofFile` already opened the file on JDK 25;
  - run `unit.download()`;
  - assert: `tmp.resolve("raw-wiktextract-data-en-2026-05-01.jsonl.gz")` exists; the `.part` file no longer exists.
- `should_keep_existing_dump_when_target_already_exists` — alternative path:
  - pre-create the destination file `raw-wiktextract-data-en-2026-05-01.jsonl.gz` in `tmp`;
  - run `download()`;
  - assert: the existing file is untouched and any `.part` file has been cleaned up.

For `KaikkiDumpDownloader.buildUrl(String)` (package-private static):
- `should_use_dictionary_path_when_lang_is_english` — `buildUrl("en")` ends with `/dictionary/raw-wiktextract-data.jsonl.gz`.
- `should_use_lang_wiktionary_path_when_lang_is_other` — `buildUrl("fr")` ends with `/frwiktionary/raw-wiktextract-data.jsonl.gz`.

### `src/test/java/edu/self/w2k/kindling/`

#### `KindlingPlatformTest.java`
- No mocks; pure static `detect(osName, arch)`.
- `should_return_linux_x64_when_os_is_linux_amd64` — `detect("Linux", "amd64")` returns `LINUX_X64` and `assetName()` is `"kindling-cli-linux"`.
- `should_throw_when_platform_is_unsupported` — `detect("SunOS", "sparc")` throws `KindlingException` (use `assertThatThrownBy`).

#### `XdgCachePathsTest.java`
- No mocks; pure static `kindlingCacheDir(env, osName)`.
- `should_use_xdg_cache_home_when_set_on_unix` — env `XDG_CACHE_HOME=/c`, `os.name="Linux"` → `/c/wiktionary-to-kindle/kindling`.
- `should_fallback_to_home_when_xdg_is_unset_on_unix` — env `HOME=/h`, no `XDG_CACHE_HOME` → `/h/.cache/wiktionary-to-kindle/kindling`.
- `should_use_localappdata_when_set_on_windows` — env `LOCALAPPDATA=C:\local`, `os.name="Windows 10"` → `C:\local\wiktionary-to-kindle\Cache\kindling`.

#### `KindlingDownloaderTest.java`
- Collaborators: `@Mock HttpFetcher fetcher;` `unit` built manually with the package-private `KindlingDownloader(HttpFetcher)` constructor; `@TempDir Path destDir;`.
- `should_download_and_return_final_path_when_sha256_matches` — happy path:
  - choose payload bytes, compute their SHA-256 with `MessageDigest`, build a release with `KindlingPlatform.LINUX_X64` whose digest equals that hash (use `KindlingRelease.DEFAULT_VERSION` to hit the `DEFAULT_ASSETS` branch — but its hardcoded digest will not match arbitrary bytes, so the test must use a non-default version and stub `fetcher.getString` to return JSON containing the matching digest);
  - stub `fetcher.getFile(any(), any())` to write payload bytes to the part path and return it;
  - call `unit.download(...)`;
  - assert: returned path equals `destDir.resolve(platform.assetName())`; file exists; part file gone.
- `should_throw_when_sha256_mismatch` — alternative:
  - stub fetcher to return bytes that don't match;
  - assert `assertThatThrownBy(() -> unit.download(...)).isInstanceOf(KindlingException.class).hasMessageContaining("SHA-256 mismatch")`;
  - assert the downloaded file was deleted.

#### `KindlingCliResolverTest.java`
- Collaborators: `@Mock KindlingDownloader downloader;` `@Mock Function<String, Optional<Path>> pathProbe;` `@Mock BiFunction<String, KindlingPlatform, String> digestProvider;` `@TempDir Path cache;`
- `unit` constructed manually using the package-private 5-arg constructor in `@BeforeEach`. The override and version are per-test (use a setter helper or rebuild `unit`).
- `should_return_override_when_override_is_executable` — happy path A:
  - create an executable file in `tmp`, build `unit` with `Optional.of(executable)`;
  - call `resolve()`;
  - assert returned path equals the executable; verify the downloader was never called.
- `should_return_path_binary_when_found_on_path` — happy path B:
  - stub `pathProbe.apply("kindling-cli")` to return an executable path;
  - assert that path is returned; downloader never called.
- `should_download_when_cache_is_empty` — alternative:
  - stub `pathProbe.apply(...)` to return `Optional.empty()`;
  - stub `downloader.download(any(), any(), any())` to return a path under `cache`;
  - assert the returned path; verify `downloader.download(...)` called once.

(`KindlingCliResolver.resolveFromCacheOrDownload` reads `XdgCachePaths.kindlingCacheDir()` directly, which depends on real env vars. The third test should rely on the override / PATH branches OR set `XDG_CACHE_HOME` via `System.setProperty` is not possible — XDG comes from `System.getenv()`. **Workaround**: in the third test, leave the cache empty so the production goes straight to the downloader (which is mocked) — we don't need to control where the cache lives, only that the file isn't there. If tests on contributors' machines could find a real cached binary, gate by mocking `pathProbe` and asserting `downloader.download` was invoked irrespective of the actual cache path.)

#### `KindlingDictionaryConverterTest.java`
- Collaborators: `@Mock OpfDictionaryWriter opfWriter; @Mock KindlingCliResolver resolver; @Mock ProcessRunner runner;` `@InjectMocks KindlingDictionaryConverter unit;` `@TempDir Path outputDir;`
- `should_run_kindling_cli_and_return_mobi_path_when_write` — happy path:
  - stub `opfWriter.write(any(), eq("en"), eq("fr"), eq("Title"), eq(outputDir))` to return `outputDir.resolve("dictionary-en-fr.opf")`;
  - stub `resolver.resolve()` to return `outputDir.resolve("kindling-cli")`;
  - stub `runner.run(anyList())` to return 0;
  - call `unit.write(new TreeMap<>(), "en", "fr", "Title", outputDir)`;
  - assert returned path equals `outputDir.resolve("dictionary-en-fr.mobi")`;
  - capture the runner's `List<String> command` and assert it contains `"build"`, the OPF path, `"-o"`, the MOBI path.
- `should_throw_io_exception_when_runner_returns_non_zero_exit_code` — alternative:
  - stub `runner.run(anyList())` to return 1;
  - `assertThatThrownBy(...).isInstanceOf(IOException.class).hasMessageContaining("exit")`.

### `src/test/java/edu/self/w2k/parse/`

#### `JsonlDictionaryParserTest.java`
- Collaborators: `@TempDir Path tmp;` plus `JsonlDictionaryParser unit = new JsonlDictionaryParser();` (no Mockito needed beyond the extension annotation, but apply it for consistency).
- Helper: write a small gzipped JSONL file into `tmp` from a string literal containing 2-3 entries.
- `should_return_only_entries_matching_lang_when_parsing` — happy path: dump contains one `el` and one `en` entry; parse with `lang="el"`; collect stream; assert `extracting(WiktionaryEntry::word).containsExactly("γεια")`.
- `should_skip_entries_with_blank_word_when_parsing` — alternative: dump contains an entry with empty word; assert it is filtered out.

### `src/test/java/edu/self/w2k/render/`

#### `HtmlDefinitionRendererTest.java`
- `HtmlDefinitionRenderer unit = new HtmlDefinitionRenderer();`
- `should_render_html_with_glosses_and_examples_when_senses_have_content` — happy: build `WiktionarySense` records directly (no Jackson), call `unit.render(...)`, assert HTML contains `<ol>`, `<li><span>...`, an `<ul>` with example items, ends with `</ol>`.
- `should_return_empty_when_no_glosses_have_content` — alternative: senses with empty/blank/null glosses → `assertThat(unit.render(...)).isEmpty()`.

(Replace the third existing escape-test by folding XML escaping + newline replacement into the happy-path assertion using AssertJ's `contains(...)` chained matcher: `assertThat(html).contains("&amp;").contains("; ").doesNotContain("\n");`)

### `src/test/java/edu/self/w2k/write/`

#### `DictionaryTitlesTest.java`
- No mocks; pure static `autoTitle(srcLang, trgLang)`.
- `should_use_display_names_when_lang_codes_are_known` — `autoTitle("el","en")` → `"Modern Greek–English Dictionary"` (or whatever JDK 25 returns for `el`; assert via `contains("Greek")` and `contains("English")` and `contains("–")` to avoid JDK locale flakiness).
- `should_uppercase_when_lang_code_is_unknown` — `autoTitle("xx","yy")` → starts with `"XX"` ends with `"Dictionary"`.

### `src/test/java/edu/self/w2k/write/opf/`

#### `HtmlChapterRendererTest.java` (package-private)
- No mocks.
- `should_render_kindle_idx_entries_when_called` — happy: pass a 2-entry list; assert the resulting `String` (`new String(bytes, UTF_8)`) contains `xmlns:idx`, `xmlns:mbp`, `<idx:entry name="word"`, `<idx:orth value="apple">`, `<b>apple</b>`.
- `should_combine_definitions_with_semicolons_when_multiple_entries_share_key` — alternative: pass a single key with two `LexiconEntry` values; assert the combined HTML contains `def1; def2`.

#### `OpfDictionaryWriterTest.java`
- `OpfDictionaryWriter unit = new OpfDictionaryWriter();` `@TempDir Path tmp;`
- `should_write_opf_html_and_return_opf_path_when_called` — happy: 3 entries → assert returned path is `tmp.resolve("dictionary-en-fr.opf")`; assert that file exists; read contents and assert they contain `<DictionaryInLanguage>en</DictionaryInLanguage>` and `<DictionaryOutLanguage>fr</DictionaryOutLanguage>`; assert `dictionary-en-fr-0.html` was also written.
- `should_chunk_html_files_when_entries_exceed_chapter_limit` — alternative: build a `TreeMap` with `HtmlChapterRenderer.ENTRIES_PER_CHAPTER + 1` entries (10 001); assert `dictionary-en-fr-0.html` and `dictionary-en-fr-1.html` both exist.

## AssertJ idioms to apply (avoid old-JUnit habits)

- `assertThat(map).containsKeys("apple")` instead of `assertTrue(map.containsKey(...))`.
- `assertThat(list).hasSize(2).extracting(LexiconEntry::word).containsExactly("a", "b")`.
- `assertThat(html).contains(...).doesNotContain(...)` chained.
- `assertThatThrownBy(() -> unit.x()).isInstanceOf(KindlingException.class).hasMessageContaining("SHA-256")`.
- `assertThat(path).exists().isRegularFile()`.
- `assertThat(optional).isPresent().contains(value)` / `.isEmpty()`.

## Mockito idioms to apply

- `@Mock` + `@InjectMocks` whenever the target's constructor lets Mockito wire collaborators.
- For services with many primitive parameters (e.g. `GenerateCommand`), drop `@InjectMocks` and instantiate manually in `@BeforeEach`.
- Use `ArgumentCaptor` for verifying complex arguments (e.g. the `TreeMap` passed to `DictionaryWriter.write`).
- Prefer `verify(mock).method(...)` over `verify(mock, times(1)).method(...)` (defaults to once).
- Avoid `verifyNoMoreInteractions` unless it adds value — keeps tests resilient to refactors.
- Don't use `@Spy` unless absolutely required; this codebase has no place that needs it.

## Implementation order

1. Update `pom.xml` (Mockito + AssertJ properties + dependencies).
2. Refactor `KaikkiDumpDownloader` to inject `HttpClient` (and `Path dumpsDir`) via package-private constructor.
3. Delete every existing test file listed above.
4. Add new test files in this order (so each layer's tests compile against already-mocked dependencies — order doesn't strictly matter since collaborators are mocked, but it helps locally validate progress):
   1. Pure-logic / static-only: `KindlingPlatformTest`, `XdgCachePathsTest`, `DictionaryTitlesTest`, `HtmlChapterRendererTest`, `HtmlDefinitionRendererTest`.
   2. Mockito-only: `DownloadCommandTest`, `KindlingDictionaryConverterTest`, `KindlingDownloaderTest`, `KindlingCliResolverTest`, `GenerateCommandTest`.
   3. `@TempDir`-based: `JsonlDictionaryParserTest`, `OpfDictionaryWriterTest`, `KaikkiDumpDownloaderTest`.
5. Run `mvn test` after each batch and fix compile/test failures before continuing.

## Verification

```sh
mvn clean test          # all tests must pass
mvn package             # full build with shade still works
```

End-to-end smoke check (manual, optional, not part of the unit suite):
```sh
java -jar target/wiktionary-to-kindle-1.0.0.jar download fr
java -jar target/wiktionary-to-kindle-1.0.0.jar generate fr en
```

## Critical files to reference during implementation

| File | Purpose |
|------|---------|
| `pom.xml` | add Mockito + AssertJ deps |
| `src/main/java/edu/self/w2k/download/KaikkiDumpDownloader.java` | add package-private constructor |
| `src/main/java/edu/self/w2k/kindling/KindlingDownloader.java` | reference pattern for HttpFetcher injection |
| `src/main/java/edu/self/w2k/kindling/KindlingCliResolver.java` | reference for the package-private testing constructor pattern |
| `src/main/java/edu/self/w2k/command/GenerateCommand.java` | the orchestrator whose flow the captor-based test exercises |
| `src/main/java/edu/self/w2k/write/opf/HtmlChapterRenderer.java` | package-private — test must live in the same package |
