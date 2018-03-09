package de.tudarmstadt.ukp.jwktl.parser.en.components.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ENTemplateHandler {

    public static final Set<String> UNHANDLED_TEMPLATES = new TreeSet<>();

    private final static Logger LOG = Logger.getLogger(ENTemplateHandler.class.getName());
    private static final Map<String, Class<? extends ITemplateParser>> PARSERS = new HashMap<>();

    static {
        // Template:qualifier
        PARSERS.put("i", QualifierTemplateParser.class);
        PARSERS.put("q", QualifierTemplateParser.class);
        PARSERS.put("qf", QualifierTemplateParser.class);
        PARSERS.put("qua", QualifierTemplateParser.class);
        PARSERS.put("qual", QualifierTemplateParser.class);
        PARSERS.put("qualifier", QualifierTemplateParser.class);

        // Template:gloss — can be parsed with the qualifier parser
        PARSERS.put("gl", QualifierTemplateParser.class);
        PARSERS.put("gloss", QualifierTemplateParser.class);

        // Template:ux
        PARSERS.put("eg", UXTemplateParser.class);
        PARSERS.put("usex", UXTemplateParser.class);
        PARSERS.put("ux", UXTemplateParser.class);

        // Template:label
        PARSERS.put("lb", LabelTemplateParser.class);
        PARSERS.put("lbl", LabelTemplateParser.class);
        PARSERS.put("label", LabelTemplateParser.class);
    }

    public static ITemplateParser getParser(String templateText) {

        try {
            // templateText is non-empty
            String[] templateWords = templateText.split("\\|");
            Class<? extends ITemplateParser> parser = PARSERS.get(templateWords[0]);

            if (parser != null) {
                return parser.getConstructor(String[].class).newInstance(new Object[] { templateWords });
            }
            else {
                UNHANDLED_TEMPLATES.add(templateWords[0]);
            }
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return null;
    }

    private ENTemplateHandler() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
