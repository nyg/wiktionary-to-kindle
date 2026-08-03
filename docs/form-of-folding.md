# Form-of folding

How the generator handles inflected-form entries so that looking up a word like Latin
*excipit* on a Kindle resolves to the full *excipio* entry instead of a dead end
([issue #56](https://github.com/nyg/wiktionary-to-kindle/issues/56)).

## The problem

Kaikki dumps contain inflected forms as their own top-level entries whose senses are all
`form-of` glosses:

```json
{"word": "suis", "lang_code": "la", "pos": "adj",
 "senses": [{"glosses": ["Datif pluriel de suus."],
             "form_of": [{"word": "suus"}], "tags": ["form-of"]}]}
```

The lemma entry (*suus*) separately lists its inflections in `forms[]`, which the generator
emits as `<idx:iform>` aliases — kindling places headwords and iforms in one flat
orthographic index, where an iform is simply an alias label pointing at its lemma's text
position.

Kindle lookup resolves the **first matching index label**. When an inflected form exists
both as its own headword and as an iform on the lemma, the headword wins, so the reader
gets the dead-end "Datif pluriel de suus." card and can never reach the lemma. (Anchor
links are not an alternative: kindling documents that they are disabled in the lookup
popup.)

## The fix: `GenerateCommand.foldFormOfEntries()`

After grouping entries by normalised lookup key, a post-pass folds **form-of-only keys**
into their lemma's inflection index:

- An entry is *form-of-only* when every renderable sense carries a `form_of` reference
  (detected by `HtmlDefinitionRenderer`, exposed as `RenderedEntry.formOfLemmas()`).
- A key is folded when **every** entry under it is form-of-only, each with at least one
  lemma that exists in the dictionary (and a word usable as a lookup key). The key's
  entries are removed and each entry's word is registered as an inflection form on its
  lemma(s). Lookup then falls through to the iform alias and lands on the full lemma entry.

### Folding is all-or-nothing per key

If *anything* under a key must stay — a homograph with its own meaning, or a form-of entry
whose lemma is absent — the key keeps **all** its entries. A partial fold would gain
nothing, because the surviving headword still shadows the inflection index, and it would
silently delete the folded entries' definitions.

The existing `filterFormsCollidingWithHeadwords()` pass runs afterwards and still strips
any iform whose text remains a headword key.

## Behaviour by case

Counts are from the Latin subset of the 2026-05-02 fr dump (99 653 lookup keys).

| Case | Example | Result |
|---|---|---|
| All entries under the key are form-of, lemma present (62 %) | *excipit* → *excipio* | Folded: no standalone entry; lookup resolves to the full lemma entry |
| Form + standalone homograph under one key (3 %) | *abactus* (noun "cattle-rustling" + participle of *abigo*) | Kept intact: the card shows both definitions combined ("…; Participe passé de abigo.") |
| One entry mixing form-of and independent senses | *cautus* (participle of *caveo* + "prudent") | Never flagged form-of-only; stays a regular headword with all glosses |
| Form-of whose lemma is missing | *petit* → *petere* (absent) | Kept as a dead-end card — better than nothing |
| Chain: form → intermediate that is itself form-of-only | — | Kept: folding into another dead end would not help |
| Form of **two** different lemmas | *mari* → *mare* and *mas* | Folded; the word becomes an iform on *both* lemmas. The firmware resolves the first matching label, so the popup lands on one of them (see limitation below) |

## Known limitations

- **Multi-lemma forms surface one lemma.** *mari* is indexed under both *mare* and *mas*,
  but a lookup resolves a single label — kindling's firmware-faithful simulator shows it
  lands on *mare*. The relation to *mas* is not visible from the popup.
- **The inflection gloss disappears for folded words.** Looking up *excipit* shows the
  full *excipio* entry, not "third-person singular present of excipio". The visible
  *Forms:* table on the lemma (non-verbs, ≤ 30 forms) partially compensates. This matches
  how commercial Kindle dictionaries behave.

## Verifying

`kindling-cli lookup` simulates the on-device index search against a built MOBI. A folded
form must resolve to the same text position as its lemma:

```sh
kindling-cli lookup dictionaries/w2k-dictionary-la-fr.mobi excipit
# "excipit" resolves (exact headword/alias) at text position 5866033
kindling-cli lookup dictionaries/w2k-dictionary-la-fr.mobi excipio
# "excipio" resolves (exact headword/alias) at text position 5866033  ← same entry
```
