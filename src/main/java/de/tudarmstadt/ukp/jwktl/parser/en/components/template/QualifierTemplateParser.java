package de.tudarmstadt.ukp.jwktl.parser.en.components.template;

import java.util.Arrays;

public class QualifierTemplateParser implements ITemplateParser {

    private String[] parameters;

    public QualifierTemplateParser(String[] words) {
        parameters = Arrays.copyOfRange(words, 1, words.length);
    }

    @Override
    public String getPlainText() {
        return "(" + String.join(", ", parameters) + ")";
    }
}
