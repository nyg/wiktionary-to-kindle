package edu.self.w2k.opf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows the creation of an OPF dictionary and its writing to disk.
 *
 * <pre>
 * Dictionary d = new Dictionary("my_dict", "el", "en");
 * d.setWords(lemmaAndMeaningsList);
 * d.build(); // writes to disk
 * </pre>
 */
public class Dictionary {

    private static final Logger LOG = Logger.getLogger(Dictionary.class.getName());

    private static final int MAX_ENTRIES = 10000;

    private BufferedWriter pageWriter;

    private final String name;
    private final String sourceLanguage;
    private final String targetLanguage;

    private final List<Word> words = new ArrayList<>();
    private final List<String> manifestItems = new ArrayList<>();
    private final List<String> spineItems = new ArrayList<>();

    public Dictionary(String name, String sourceLanguage, String targetLanguage) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public void addWord(Word word) {
        words.add(word);
    }

    public void setWords(List<Word> words) {
        words.clear();
        words.addAll(words);
    }

    public void build() {

        /* Iterate over definitions and create the HTML files. */

        int counter = 0, pageIndex = 0;
        for (Word wd : words) {

            pageIndex = counter / MAX_ENTRIES;
            String entry = getEntry(wd.getLemma(), wd.getMeanings());
            writePage(counter / MAX_ENTRIES, entry);

            counter++;

            if (counter % MAX_ENTRIES == 0) {
                endPage(pageIndex);
            }
        }

        // close last page if necessary
        if (pageWriter != null) {
            endPage(pageIndex);
        }

        /* Write the OPF file. */

        try (BufferedWriter opfWriter = new BufferedWriter(new FileWriter("dictionaries/" + name + ".opf"))) {

            for (String line : DictionaryUtil.OPF_MAIN) {

                line.replace(DictionaryUtil.DICT_NAME_PH, name);
                line.replace(DictionaryUtil.SOURCE_LANG_PH, sourceLanguage);
                line.replace(DictionaryUtil.TARGET_LANG_PH, targetLanguage);

                opfWriter.write(line);

                if (line.contains(DictionaryUtil.MANIFEST_TAG)) {
                    for (String item : manifestItems) {
                        opfWriter.write(item);
                    }
                }

                if (line.contains(DictionaryUtil.SPINE_TAG)) {
                    for (String item : spineItems) {
                        opfWriter.write(item);
                    }
                }
            }
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private String getEntry(String word, String definition) {
        return DictionaryUtil.OPF_ENTRY.replace(DictionaryUtil.LEMMA_PH, word).replace(DictionaryUtil.MEANINGS_PH, definition);
    }

    private void writePage(int index, String line) {

        try {
            if (pageWriter == null) {
                beginPage(index);
            }

            pageWriter.write(line);
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void beginPage(int index) {

        try {
            pageWriter = new BufferedWriter(new FileWriter("dictionaries/title" + index + ".html"));
            for (String line : DictionaryUtil.OPF_PAGE_BEGINNING) {
                pageWriter.write(line);
            }
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void endPage(int index) {

        try {
            for (String line : DictionaryUtil.OPF_PAGE_END) {
                pageWriter.write(line);
                manifestItems.add(String.format(DictionaryUtil.MANIFEST_ITEM, Integer.toString(index), name));
                spineItems.add(String.format(DictionaryUtil.SPINE_ITEM, Integer.toString(index)));
            }

            // FIXME
            pageWriter.close();
            pageWriter = null;
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
