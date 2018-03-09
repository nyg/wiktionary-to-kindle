package de.tudarmstadt.ukp.jwktl.parser.en.components.template;

import java.util.Arrays;

public class UXTemplateParser implements ITemplateParser {

    private String example;
    private String translation;

    public UXTemplateParser(String[] words) {

        String[] parameters = Arrays.copyOfRange(words, 2, words.length);

        for (String parameter : parameters) {

            if (parameter.matches("^t=.+")) {
                translation = parameter.split("=")[1];
            }
            else if (!parameter.contains("=")) {

                /* The first unnamed parameter is the example. The second one
                 * is the translation (which can also be a named one, see "if"
                 * above these are the only two unnamed parameters. */

                if (example == null) {
                    example = parameter;
                }
                else if (translation == null) {
                    translation = parameter;
                }
            }
        }
    }

    @Override
    public String getPlainText() {
        return example + " — " + translation;
    }
}
