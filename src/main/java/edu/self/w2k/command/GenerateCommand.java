package edu.self.w2k.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.parse.DictionaryParser;
import edu.self.w2k.render.DefinitionRenderer;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import edu.self.w2k.write.DictionaryWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GenerateCommand implements Command {

    private final DictionaryParser parser;
    private final DefinitionRenderer renderer;
    private final DictionaryWriter writer;
    private final Path dumpFile;
    private final Path outputDir;
    private final String srcLang;
    private final String trgLang;
    private final String title;

    @Override
    public void run() throws IOException {
        log.info("Using dump: {}", dumpFile);
        TreeMap<String, List<LexiconEntry>> grouped = new TreeMap<>();
        AtomicLong count = new AtomicLong();

        try (Stream<LexiconEntry> stream = parser.parse(dumpFile, srcLang)
                .flatMap(e -> renderer.render(e)
                        .map(r -> new LexiconEntry(e.word(), r.html(), r.inflectionForms(), r.formOfLemmas()))
                        .stream())) {
            stream.forEach(e -> {
                String key = normaliseKey(e.word());
                if (!key.isEmpty()) {
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
                    count.incrementAndGet();
                }
            });
        }

        log.info("Done. {} entries grouped into {} unique keys for srcLang={}, trgLang={}", count.get(), grouped.size(), srcLang, trgLang);

        foldFormOfEntries(grouped);
        filterFormsCollidingWithHeadwords(grouped);

        writer.write(grouped, srcLang, trgLang, title, outputDir);
    }

    /**
     * Folds "form-of-only" lookup keys (e.g. Latin <i>suis</i>, whose only senses are "Datif pluriel
     * de suus.") into their lemma's inflection index. On Kindle an exact headword match shadows the
     * {@code <idx:iform>} index, so keeping these as standalone headwords makes them dead ends. When
     * every entry under a key is a form-of entry with at least one lemma present in the dictionary,
     * the whole key is dropped and each entry's word registered as an inflection form on its
     * lemma(s), so a lookup resolves straight to the full lemma entry.
     * <p>
     * Folding is all-or-nothing per key: if anything under the key must stay — a homograph with its
     * own meaning, or a form-of entry whose lemma is absent — the key keeps all its entries. A
     * partial fold would gain nothing (the surviving headword still shadows the inflection index)
     * and would silently delete the folded entries' definitions. See docs/form-of-folding.md.
     */
    static void foldFormOfEntries(TreeMap<String, List<LexiconEntry>> grouped) {
        // keys backed by at least one entry with an independent meaning; only these can absorb forms
        // (folding into a form-of-only group would just move the dead end around)
        Set<String> realKeys = new HashSet<>();
        for (Map.Entry<String, List<LexiconEntry>> group : grouped.entrySet()) {
            if (group.getValue().stream().anyMatch(e -> e.formOfLemmas().isEmpty())) {
                realKeys.add(group.getKey());
            }
        }

        Map<String, Set<String>> extraIforms = new HashMap<>();
        long folded = 0;

        Iterator<Map.Entry<String, List<LexiconEntry>>> it = grouped.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<LexiconEntry>> group = it.next();
            String ownKey = group.getKey();
            List<LexiconEntry> entries = group.getValue();

            List<List<String>> lemmaKeysPerEntry = new ArrayList<>(entries.size());
            boolean allFoldable = true;
            for (LexiconEntry e : entries) {
                if (e.formOfLemmas().isEmpty() || !HtmlDefinitionRenderer.isUsableLookupKey(e.word().strip())) {
                    allFoldable = false;
                    break;
                }
                List<String> lemmaKeys = e.formOfLemmas().stream()
                        .map(GenerateCommand::normaliseKey)
                        .filter(key -> !key.equals(ownKey) && realKeys.contains(key))
                        .toList();
                if (lemmaKeys.isEmpty()) {
                    allFoldable = false;
                    break;
                }
                lemmaKeysPerEntry.add(lemmaKeys);
            }
            if (!allFoldable) {
                continue;
            }

            for (int i = 0; i < entries.size(); i++) {
                String word = entries.get(i).word().strip();
                for (String lemmaKey : lemmaKeysPerEntry.get(i)) {
                    extraIforms.computeIfAbsent(lemmaKey, k -> new LinkedHashSet<>()).add(word);
                }
            }
            folded += entries.size();
            it.remove();
        }

        for (Map.Entry<String, Set<String>> extra : extraIforms.entrySet()) {
            // target groups always survive: they contain a non-form-of entry, which is never removed
            List<LexiconEntry> entries = grouped.get(extra.getKey());
            LexiconEntry first = entries.getFirst();
            Set<String> merged = new LinkedHashSet<>(first.inflectionForms());
            merged.addAll(extra.getValue());
            if (merged.size() != first.inflectionForms().size()) {
                entries.set(0, new LexiconEntry(first.word(), first.definition(), List.copyOf(merged), first.formOfLemmas()));
            }
        }

        log.info("Folded {} form-of entries into their lemma's inflection index", folded);
    }

    /**
     * Language-agnostic post-pass: drop any inflection form whose normalised text already exists
     * as a headword key in the grouped map. This removes gender-equivalent / cross-reference forms
     * (e.g. fr <i>ingénieure</i> listed under <i>ingénieur</i> while also having its own entry)
     * from the Kindle iform lookup index without depending on per-Wiktionary template markers like
     * {@code équiv-pour}. The visible "Forms:" table in each entry's HTML body is untouched, so
     * readers still see the full paradigm under the lemma.
     */
    static void filterFormsCollidingWithHeadwords(TreeMap<String, List<LexiconEntry>> grouped) {
        for (List<LexiconEntry> entries : grouped.values()) {
            for (int i = 0; i < entries.size(); i++) {
                LexiconEntry e = entries.get(i);
                List<String> forms = e.inflectionForms();
                if (forms.isEmpty()) {
                    continue;
                }
                List<String> kept = new ArrayList<>(forms.size());
                for (String form : forms) {
                    if (!grouped.containsKey(normaliseKey(form))) {
                        kept.add(form);
                    }
                }
                if (kept.size() != forms.size()) {
                    entries.set(i, new LexiconEntry(e.word(), e.definition(), List.copyOf(kept), e.formOfLemmas()));
                }
            }
        }
    }

    /**
     * Normalises a word into a Kindle lookup key: lowercase, strip, replace {@code "} with {@code '},
     * and escape {@code <}/{@code >} for the Kindle index.
     */
    static String normaliseKey(String word) {
        return word
                .replace('"', '\'')
                .replace("<", "\\<")
                .replace(">", "\\>")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
