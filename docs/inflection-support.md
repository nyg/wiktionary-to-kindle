# Inflection support (declined / conjugated word forms)

## Why

The kaikki.org Wiktionary dumps include a `forms` array per entry containing every
inflected form of the lemma — plurals, declension cases, conjugations, gender-agreement
forms. Before this feature, `wiktionary-to-kindle` silently dropped that data via
`@JsonIgnoreProperties(ignoreUnknown = true)` on `WiktionaryEntry`, so a Kindle reader
who tapped an inflected form (e.g. `συντρόφους`, the accusative plural of `σύντροφος`)
got nothing back.

## What changed

1. **Lookup index for every form.** Each entry's `<idx:orth>` now carries an
   `<idx:infl>` block listing every form found in the dump as `<idx:iform>` children.
   Kindling indexes both the headword and every iform into a single orthographic
   index, so any form resolves to the lemma at lookup time. No 255-form cap (kindling
   removed kindlegen's old limit).
2. **Visible paradigm table for non-verb entries.** After the definitions, the entry
   HTML renders a small `<ul>` listing each form with its abbreviated grammatical
   tags and (when present) its article. Useful for nouns / adjectives / pronouns
   where the paradigm is small enough to scan at a glance.
3. **Verbs skip the visible table.** Greek / Romance / Slavic verbs have 50–200+
   forms; the table would dwarf the definition. Verbs still get the lookup index.
4. **Safety net.** Even for non-verb POS the visible table is skipped when
   `forms.size() > 30`, catching pathological edge cases or unfamiliar POS values.
5. **équiv-pour cross-references filtered.** Forms whose `source` field contains
   `équiv-pour` are dropped entirely. These are gender-equivalent **separate lemmas**
   (e.g. `συντρόφισσα` is its own noun, not an inflection of `σύντροφος`); they have
   their own standalone Wiktionary entries already.

## Markup produced

Before:

```xml
<idx:entry name="word" scriptable="yes">
    <idx:orth value="apple"><b>apple</b></idx:orth>
    <ol><li><span>A round fruit.</span></li></ol>
</idx:entry>
```

After (noun, with visible forms table):

```xml
<idx:entry name="word" scriptable="yes">
    <idx:orth value="σύντροφος">
        <b>σύντροφος</b>
        <idx:infl>
            <idx:iform value="σύντροφοι"/>
            <idx:iform value="συντρόφου"/>
            <idx:iform value="συντρόφων"/>
            <idx:iform value="σύντροφο"/>
            <idx:iform value="συντρόφους"/>
            <idx:iform value="σύντροφε"/>
        </idx:infl>
    </idx:orth>
    <ol><li><span>Compagnon.</span></li>...</ol>
    <p><i>Forms:</i></p>
    <ul>
      <li>pl. nom.: οι σύντροφοι</li>
      <li>sg. gen.: του συντρόφου</li>
      <li>pl. gen.: των συντρόφων</li>
      <li>sg. acc.: τον σύντροφο</li>
      <li>pl. acc.: τους συντρόφους</li>
      <li>sg. voc.: σύντροφε</li>
    </ul>
</idx:entry>
```

After (verb, no visible table — iform still emitted):

```xml
<idx:entry name="word" scriptable="yes">
    <idx:orth value="έχω">
        <b>έχω</b>
        <idx:infl>
            <idx:iform value="είχα"/>
            <idx:iform value="είχες"/>
            ... (many more)
        </idx:infl>
    </idx:orth>
    <ol><li><span>Avoir.</span></li></ol>
</idx:entry>
```

The `<idx:infl>` element sits **inside** `<idx:orth>` per Amazon's Kindle Publishing
Guidelines and kindling's parser expectations.

## Lookup behaviour on Kindle

When two entries claim the same form, Kindle's Publishing Guidelines specify
**exact `<idx:orth value="…">` matches outrank `<idx:iform value="…">` matches**.
So if a form is also a standalone headword in the dump, the standalone entry wins
at lookup time. We always emit the iform anyway: the cost is a few extra bytes, and
it acts as a safety net against firmware quirks. There is no rich Kindle UI for the
reader to disambiguate between matches — the popup card shows one entry only.

## Code map

| Responsibility | File |
|---|---|
| Form data model | `model/WiktionaryForm.java` |
| Entry data model with `pos` + `forms` | `model/WiktionaryEntry.java` |
| Pipeline transport with iform list | `model/LexiconEntry.java` |
| Renderer return type | `render/RenderedEntry.java` |
| Tag abbreviation (singular → sg., etc.) | `render/InflectionTagAbbreviator.java` |
| HTML rendering + équiv-pour filter + verb gate | `render/HtmlDefinitionRenderer.java` |
| `<idx:infl>` / `<idx:iform>` XML emission | `write/opf/HtmlChapterRenderer.java` |
| End-to-end glue | `command/GenerateCommand.java` |

## Out of scope

- Per-language inflection rules. Kindling supports `--inflection-rules` files for
  morphological generation; we don't generate them. Every iform is one explicitly
  present in the kaikki dump.
- Merging gender-equivalent entries (e.g. unifying σύντροφος and συντρόφισσα into
  one composite entry). They remain separate; the équiv-pour filter just keeps the
  lemma boundary clean in the iform index.
- POS-aware truncated verb tables (e.g. showing only principal parts). Verbs
  currently get no visible table at all.
