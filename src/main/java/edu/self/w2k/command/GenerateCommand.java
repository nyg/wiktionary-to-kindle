package edu.self.w2k.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.parse.DictionaryParser;
import edu.self.w2k.render.DefinitionRenderer;
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
                        .map(r -> new LexiconEntry(e.word(), r.html(), r.inflectionForms()))
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

        filterFormsCollidingWithHeadwords(grouped);

        writer.write(grouped, srcLang, trgLang, title, outputDir);
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
                    entries.set(i, new LexiconEntry(e.word(), e.definition(), List.copyOf(kept)));
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
