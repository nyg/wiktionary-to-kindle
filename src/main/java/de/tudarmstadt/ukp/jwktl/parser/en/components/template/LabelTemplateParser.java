package de.tudarmstadt.ukp.jwktl.parser.en.components.template;

import java.util.Arrays;

public class LabelTemplateParser implements ITemplateParser {

    private String[] parameters;

    public LabelTemplateParser(String[] words) {
        parameters = Arrays.copyOfRange(words, 2, words.length);
    }

    @Override
    public String getPlainText() {
        return "(" + String.join(", ", parameters) + ")";
    }
}
