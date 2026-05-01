# Project Review: `wiktionary-to-kindle`

**Date:** 2026-05-01  
**Reviewers:** Claude Sonnet 4.6 · GPT-5.4 (independent reviews, findings merged)  
**Baseline:** All 37 tests pass. The tool works correctly on the happy path.

---

## Executive Summary

The codebase is compact, readable, and has solid test coverage for its core happy-path logic. Dependency choices are sensible and the streaming design for JSONL extraction is efficient. However, several real bugs affect output correctness and robustness, and one category of issue (XML injection) produces **confirmed malformed output that KindleGen will likely reject**. The README is also materially outdated.

Priority areas for action:

1. **Fix XML injection** — unescaped headwords, keys, and metadata in generated HTML/OPF produce invalid XML.
2. **Fix silent failure propagation** — one bad JSON line silently aborts generation; OPF proceeds from stale/partial output regardless of success.
3. **Fix exit codes** — the CLI always exits 0, making failure invisible to scripts and CI.
4. **Update the README** — `tab2opf` is no longer used; the current pipeline is Java-only.

---

## Findings by Dimension

### 1. Architecture & Design

#### **Major** — `generateOpf` always runs even when `generateDictionary` fails
*Raised by: both models*

`CLI.java` lines 35–36:
```java
WiktionaryUtil.generateDictionary(srcLang);  // swallows IOException; returns void
generateOpf(srcLang, trgLang, title);        // unconditionally runs
```
`generateDictionary` catches `IOException` internally and returns `void` with no success signal. If dictionary generation fails (corrupt dump, disk full, bad JSON line), `generateOpf` still runs. It will silently re-read a stale `dictionaries/lexicon.txt` from a previous run, producing wrong output with no diagnostic — or fail later with a cryptic error if the file doesn't exist.

**Fix:** Have `generateDictionary` return a boolean or re-throw. In `CLI.main`, gate `generateOpf` on success.

---

#### **Minor** — Lexicon path hardcoded in two disconnected places
*Raised by: Claude Sonnet 4.6*

`CLI.java` line 14 and `WiktionaryUtil.java` line 35 both hardcode `"dictionaries/lexicon.txt"` independently. If one changes, the two pipeline steps silently decouple.

**Fix:** Define the constant in one place (e.g., `CLI.LEXICON_FILE`) and pass it as a parameter to `generateDictionary`.

---

#### **Suggestion** — All-static `@UtilityClass` design limits extensibility
*Raised by: GPT-5.4*

Hardcoded relative paths and static calls are fine for a small CLI, but they make alternate input/output paths, testing, and future extension harder. If the tool grows, consider a small config object and instance-based services.

---

### 2. Code Quality

#### **Minor** — Mixed NIO / legacy I/O in `WiktionaryUtil`
*Raised by: Claude Sonnet 4.6*

`WiktionaryUtil.java` line 54 uses `new FileWriter(outputFile.toFile(), ...)` while the rest of the method uses NIO (`Files.newInputStream`). `Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)` is idiomatic and consistent with `OpfUtil`.

---

#### **Minor** — `String.matches()` used for string equality
*Raised by: Claude Sonnet 4.6*

`CLI.java` line 31: `action.matches("dl|download")` uses regex semantics for a literal equality check. It works, but reads as confusing to Java developers who expect `matches` to be anchored regex. Prefer `"dl".equals(action) || "download".equals(action)`.

---

### 3. Error Handling

#### **Major** — A single malformed JSON line silently aborts the entire run
*Raised by: both models (confirmed empirically)*

`WiktionaryUtil.java` line 59:
```java
WiktionaryEntry entry = reader.readValue(line);
```
`readValue` throws `JsonProcessingException` on malformed input. This escapes the `while` loop, closes the partially-written output file, and returns to the `catch` block in `generateDictionary`. For a 1–2 GB dump, processing stops wherever the bad line appears; entries after it are not written. The user sees only "Failed to generate dictionary" with no count of what was actually written.

**Fix:** Wrap `reader.readValue(line)` in a per-line `try-catch`, log the bad line, and `continue` to the next entry.

---

#### **Major** — Partial output files are written directly to their final path
*Raised by: GPT-5.4*

`WiktionaryUtil.generateDictionaryToFile` and all files in `OpfUtil.writeHtmlFile` / `writeOpfFile` write directly to their final destination. A mid-run failure leaves partial files that a subsequent step — or the next run — may silently consume. (`DumpUtil.download` correctly uses a `.part` file and atomic rename; the same pattern should be applied elsewhere.)

**Fix:** Write to a `*.tmp` file and atomically move to the final path only on success.

---

#### **Major** — CLI always exits with status code 0, even on failure
*Raised by: GPT-5.4*

All error paths log a message and return, never calling `System.exit(1)`. Scripts, CI pipelines, or Makefiles cannot detect failure.

**Fix:** Propagate failure to `main()` and call `System.exit(1)` on any unrecoverable error.

---

#### **Major** — Output directories are assumed to exist
*Raised by: GPT-5.4*

`DumpUtil`, `WiktionaryUtil`, and `OpfUtil` all assume `dumps/` and `dictionaries/` exist. Running the JAR from a fresh checkout or after deleting these directories produces `NoSuchFileException` rather than a clear message.

**Fix:** Call `Files.createDirectories(...)` for each output directory before writing.

---

#### **Minor** — `HttpClient` is not closed
*Raised by: Claude Sonnet 4.6*

`DumpUtil.java` lines 37–43: `HttpClient` implements `Closeable` since Java 21 but is never closed, leaking its internal thread pool until JVM exit. For a short-lived CLI this is harmless, but it is technically a resource leak.

**Fix:** Use a `try-with-resources` block around the `HttpClient`.

---

### 4. Security — XML Injection (confirmed bugs)

> All three findings below produce **confirmed malformed output** that KindleGen may reject outright. They share the same root cause: values from user input or the Wiktionary data are interpolated directly into XML/HTML using `String.formatted()` without escaping.

---

#### **Critical** — `displayTerm` inserted unescaped into HTML element content
*Raised by: both models (confirmed)*

`OpfUtil.java` line 212, inside the `<strong>%s</strong>` template:
```java
.formatted(key, displayTerm, combinedDef)
```
`displayTerm` is the raw term from the lexicon. Words like `"R&B"`, `"M&Ms"`, `"<verb>"` contain XML-special characters. Confirmed: the output contains `<strong>R&B</strong>` — an unescaped `&`.

**Fix:** `StringEscapeUtils.escapeXml10(displayTerm)` (the import already exists in `WiktionaryUtil`; add it to `OpfUtil`).

---

#### **Critical** — `key` in `idx:orth value=` XML attribute is not properly escaped
*Raised by: both models (confirmed)*

`OpfUtil.normaliseKey` (lines 122–129) replaces `<` with `\<` and `>` with `\>`, but these are **not** valid XML escape sequences. Worse, `&` is not escaped at all. Confirmed live: `value="r&b"` is emitted — invalid XML that strict parsers will reject. `\<` is also not a valid XML attribute escape.

**Fix:** After applying the structural normalisations in `normaliseKey`, run `StringEscapeUtils.escapeXml10()` on the result before placing it in the attribute.

---

#### **Major** — User-supplied `title` and lang codes injected unescaped into OPF XML
*Raised by: both models (confirmed)*

`OpfUtil.writeOpfFile` line 241 interpolates `title`, `srcLang`, and `trgLang` directly into XML element content. A legitimate title like `"Greek & English Dictionary"` produces invalid XML. More severely, a title of `"Valid</dc:title><dc:creator>INJECTED"` produces a valid-looking but structurally broken OPF document — confirmed live.

**Fix:** Escape `title`, `srcLang`, and `trgLang` with `StringEscapeUtils.escapeXml10()` before `.formatted()`.

---

### 5. Performance

#### **Major** — Entire lexicon loaded into RAM before any HTML is written
*Raised by: both models*

`OpfUtil.readLexicon` materialises the full lexicon into `TreeMap<String, List<String[]>>` before `writeHtmlFiles` is called. For large languages (English, German), the `lexicon.txt` can be hundreds of megabytes; the in-memory representation will be larger. On machines with a default JVM heap, this may cause OOM errors.

**Fix options:**
1. *(Minimal)* Add a note to the README recommending `java -Xmx2g -jar ...` for large languages.
2. *(Structural)* Sort entries during `WiktionaryUtil.generateDictionaryToFile` so `readLexicon` can stream directly to the HTML writer without holding everything in memory.

---

#### **Minor** — `exSb` StringBuilder allocated on every gloss regardless of whether examples exist
*Raised by: Claude Sonnet 4.6*

`WiktionaryUtil.java` lines 115–129: `new StringBuilder("<ul>")` and the closing tag append are executed for every gloss. Most senses have no examples. This is wasteful allocation at scale.

**Fix:** Lazily initialise `exSb` only when the first non-blank example is encountered.

---

### 6. Testing

#### **Major** — No tests cover the XML injection bugs (S-1, S-2, S-3)
*Raised by: both models*

The existing `buildDefinition_xmlEscaping` test verifies gloss escaping, but no test passes a term containing `&` or `<` through `writeEntry` or `writeOpfFile`. The three confirmed injection bugs above are entirely invisible to the test suite.

**Fix:** Add `OpfUtilTest` cases:
- `writeEntry_ampersandInTermIsEscaped` — term `"R&B"` → HTML contains `R&amp;B`
- `writeOpfFile_ampersandInTitleIsEscaped` — title `"A & B"` → OPF contains `A &amp; B`
- `normaliseKey_specialCharsEscapedForXml` — key containing `&` is properly escaped

---

#### **Major** — No test covers malformed-JSON abort behavior
*Raised by: both models*

There is no test verifying whether a bad JSON line aborts processing or is skipped. Without this, fixing the per-line error handling (Finding E-1) has no regression safety net.

---

#### **Minor** — `writeGzipJsonl` test helper duplicated
*Raised by: Claude Sonnet 4.6*

An identical helper exists in both `WiktionaryUtilTest.java` and `FullPipelineTest.java`. Extract to a shared `TestUtil` class.

---

### 7. Dependencies

#### **Minor** — Java 25 compile target narrows portability without clear justification
*Raised by: GPT-5.4*

`pom.xml` sets `maven.compiler.release=25`. The code uses features available since Java 21 (text blocks, `List.of`, `SequencedCollection.getFirst`). Requiring Java 25 narrows contributor/user portability.

**Fix:** Lower `maven.compiler.release` to the minimum actually required, or document why 25 is intentional.

---

#### **Suggestion** — `commons-text` used solely for `StringEscapeUtils.escapeXml10()`
*Raised by: Claude Sonnet 4.6*

A full Apache Commons Text artifact is on the classpath only to call one static method. After fixing the XML injection bugs by also using it in `OpfUtil`, the dependency becomes better justified. If removing it is desired, a simple inline replacement:
```java
str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
   .replace("\"", "&quot;").replace("'", "&apos;")
```
covers all XML 1.0 attribute and text escaping needs.

---

### 8. Documentation

#### **Major** — README describes `tab2opf` as a required step, but it is no longer used
*Raised by: both models*

`README.md` lines 9–10 state:
> 3. [tab2opf](https://github.com/nyg/tab2opf) is used to convert the text file into a set of OPF and HTML files.

`OpfUtil` now replaces `tab2opf` entirely. A user following the README will look for a Python dependency and a separate step that no longer exist. The "How it works" section and step-by-step instructions both need updating to reflect the current Java-only pipeline.

**Fix:** Update the README to describe what `java -jar ... generate <lang>` actually produces, removing all `tab2opf` references.

---

### 9. Additional Bugs

#### **Minor** — Examples duplicated under each gloss string within a single sense
*Raised by: Claude Sonnet 4.6*

`WiktionaryUtil.buildDefinition` (lines 104–135): for a sense with `glosses: ["A", "B"]` and `examples: [{"text":"ex1"}]`, examples are fetched once per gloss, producing:
```html
<li><span>A</span><ul><li>ex1</li></ul></li>
<li><span>B</span><ul><li>ex1</li></ul></li>
```
In kaikki.org data, multiple glosses within a sense are hierarchical refinements that all share the same examples. Duplicating them is likely unintentional.

**Fix:** Fetch examples at the sense level (outside the per-gloss loop) and append the `<ul>` only once per sense.

---

## Summary Table

| ID | File | Severity | Issue |
|----|------|----------|-------|
| S-1 | `OpfUtil.java:212` | **Critical** | `displayTerm` unescaped in HTML → `R&B` confirmed malformed |
| S-2 | `OpfUtil.java:122–129, 212` | **Critical** | `key` in XML attribute: `&` unescaped, `\<`/`\>` not valid escapes — confirmed |
| S-3 | `OpfUtil.java:241` | **Major** | `title`/lang codes injected unescaped into OPF — XML injection confirmed |
| A-1 | `CLI.java:35–36` | **Major** | `generateOpf` runs unconditionally even after `generateDictionary` failure |
| E-1 | `WiktionaryUtil.java:59` | **Major** | Single malformed JSON line aborts entire run; partial output written silently |
| E-2 | `CLI.java`, `WiktionaryUtil.java` | **Major** | CLI always exits with status 0; failure invisible to scripts/CI |
| E-3 | `DumpUtil.java`, `WiktionaryUtil.java`, `OpfUtil.java` | **Major** | Partial files written directly to final path; no atomic move on failure |
| E-4 | `DumpUtil.java`, `WiktionaryUtil.java`, `OpfUtil.java` | **Major** | Output directories assumed to exist; `NoSuchFileException` on fresh run |
| P-1 | `OpfUtil.java:58–61` | **Major** | Full lexicon loaded into RAM; OOM risk for large languages |
| T-1 | *(missing)* | **Major** | No tests for XML injection via term, key, or title |
| T-2 | *(missing)* | **Major** | No test for malformed-JSON abort / per-line skip behavior |
| Doc-1 | `README.md:9–10` | **Major** | `tab2opf` step documented but no longer used |
| A-2 | `CLI.java:14`, `WiktionaryUtil.java:35` | Minor | Lexicon path hardcoded in two disconnected places |
| Q-1 | `WiktionaryUtil.java:54` | Minor | `FileWriter` + `.toFile()` instead of `Files.newBufferedWriter` |
| Q-2 | `CLI.java:31` | Minor | `String.matches()` used for literal equality |
| E-5 | `DumpUtil.java:37–43` | Minor | `HttpClient` not closed (resource leak) |
| P-2 | `WiktionaryUtil.java:115` | Minor | `exSb` allocated on every gloss even when no examples exist |
| T-3 | `WiktionaryUtilTest.java`, `FullPipelineTest.java` | Minor | `writeGzipJsonl` helper duplicated |
| D-1 | `pom.xml` | Minor | Java 25 target higher than necessary |
| D-2 | `pom.xml:29–32` | Suggestion | `commons-text` used for one method; could be replaced inline |
| B-1 | `WiktionaryUtil.java:104–135` | Minor | Examples duplicated under each gloss within a single sense |

---

## Conclusion & Recommended Fix Order

### Immediate (output correctness — KindleGen may reject current output)
1. **S-1, S-2:** Escape `displayTerm` and `key` in `OpfUtil.writeEntry`. One-liner fix using `StringEscapeUtils.escapeXml10()`, already imported in the project.
2. **S-3:** Escape `title`, `srcLang`, `trgLang` in `OpfUtil.writeOpfFile`. Same fix.
3. **T-1:** Add `OpfUtilTest` cases to lock in the escaping fixes.

### High priority (silent failure modes)
4. **E-1:** Wrap `reader.readValue(line)` in a per-line `try-catch` with `continue`.
5. **A-1:** Signal `generateDictionary` failure back to `CLI.main`; gate OPF generation.
6. **E-2:** Exit non-zero on any unrecoverable error.
7. **E-3:** Write outputs to `*.tmp` files and atomically rename on success.

### Normal priority
8. **Doc-1:** Update README to describe the Java-only pipeline.
9. **E-4:** Create output directories before writing.
10. **P-1:** Document or fix large-language memory usage.

### Low priority / housekeeping
11. Minor code-quality and test improvements (A-2, Q-1, Q-2, E-5, P-2, T-2, T-3, D-1, B-1).
