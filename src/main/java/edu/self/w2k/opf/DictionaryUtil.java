package edu.self.w2k.opf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class DictionaryUtil {

    public static final List<String> OPF_MAIN;
    public static final List<String> OPF_PAGE_BEGINNING;
    public static final List<String> OPF_PAGE_END;
    public static final String OPF_ENTRY;

    public static final String MANIFEST_ITEM = "<item id=\"dictionary%1$s\" href=\"%2$s%1$s.html\" media-type=\"text/x-oeb1-document\"/>";
    public static final String SPINE_ITEM = "<itemref idref=\"dictionary%s\"/>";

    public static final String DICT_NAME_PH = "{DICT_NAME}";
    public static final String SOURCE_LANG_PH = "{SOURCE_LANG}";
    public static final String TARGET_LANG_PH = "{TARGET_LANG}";
    public static final String LEMMA_PH = "{LEMMA}";
    public static final String MEANINGS_PH = "{MEANINGS}";

    public static final String MANIFEST_TAG = "<manifest>";
    public static final String SPINE_TAG = "<spine>";

    static {

        try {
            OPF_MAIN = Files.readAllLines(Paths.get("src/main/resources/opf_main.xml"));
            OPF_PAGE_BEGINNING = Files.readAllLines(Paths.get("src/main/resources/opf_page_beginning.html"));
            OPF_PAGE_END = Files.readAllLines(Paths.get("src/main/resources/opf_page_end.html"));

            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/opf_entry.xml"));
            OPF_ENTRY = String.join("", lines);
        }
        catch (IOException e) {
            throw new RuntimeException("One of the template files could not be read!", e);
        }
    }

    private DictionaryUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
