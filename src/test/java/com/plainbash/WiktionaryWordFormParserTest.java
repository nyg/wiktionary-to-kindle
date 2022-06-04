package com.plainbash;

import org.junit.Assert;
import org.junit.Test;

public class WiktionaryWordFormParserTest {
    @Test
    public void validString_thenParse() {
        final String shortDescription = "{{fi-form of|case=adessive|saada|pr=third-person|pl=singular|mood=indicative|tense=present|suffix=-pas}}";
        final String longDescription = "{{fi-form of|saada|pr=second-person|pl=singular|mood=imperative|tense=present connegative|suffix=-pas}}";

        WiktionaryWordFormParser parser = new WiktionaryWordFormParser();

        Assert.assertEquals("Invalid string", "<i>case=adessive|pr=third-person|pl=singular|mood=indicative|tense=present|suffix=-pas form of <a href=\"#saada\">saada</a></i>", parser.format(shortDescription));
        Assert.assertEquals("Invalid string", "<i>pr=second-person|pl=singular|mood=imperative|tense=present connegative|suffix=-pas form of <a href=\"#saada\">saada</a></i>", parser.format(longDescription));
    }
}
