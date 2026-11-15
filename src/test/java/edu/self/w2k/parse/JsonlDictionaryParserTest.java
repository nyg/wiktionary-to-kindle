package edu.self.w2k.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionaryForm;
import edu.self.w2k.model.WiktionaryFormOf;
import edu.self.w2k.model.WiktionaryLinkage;
import edu.self.w2k.model.WiktionarySense;
import edu.self.w2k.model.WiktionarySound;

@ExtendWith(MockitoExtension.class)
class JsonlDictionaryParserTest {

    private final JsonlDictionaryParser unit = new JsonlDictionaryParser();

    @TempDir
    Path tmp;

    @Test
    void should_return_only_entries_matching_lang_when_parsing() throws Exception {
        // Given
        String jsonl = """
                {"word":"γεια","lang_code":"el","senses":[]}
                {"word":"hello","lang_code":"en","senses":[]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result)
                .extracting(WiktionaryEntry::word)
                .containsExactly("γεια");
    }

    @Test
    void should_skip_entries_with_blank_word_when_parsing() throws Exception {
        // Given
        String jsonl = """
                {"word":"","lang_code":"el","senses":[]}
                {"word":"καλή","lang_code":"el","senses":[]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result)
                .extracting(WiktionaryEntry::word)
                .containsExactly("καλή");
    }

    @Test
    void should_deserialise_pos_and_forms_when_parsing() throws Exception {
        // Given
        String jsonl = """
                {"word":"σύντροφος","lang_code":"el","pos":"noun","senses":[],"forms":[\
                {"form":"σύντροφοι","tags":["plural","nominative"],"article":"οι"},\
                {"form":"συντρόφου","tags":["singular","genitive"],"article":"του"},\
                {"form":"συντρόφισσα","tags":["feminine"],"source":"form line template 'équiv-pour'"}]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        WiktionaryEntry entry = result.getFirst();
        assertThat(entry.pos()).isEqualTo("noun");
        assertThat(entry.forms())
                .extracting(WiktionaryForm::form)
                .containsExactly("σύντροφοι", "συντρόφου", "συντρόφισσα");
        assertThat(entry.forms().get(0).tags()).containsExactly("plural", "nominative");
        assertThat(entry.forms().get(0).article()).isEqualTo("οι");
        assertThat(entry.forms().get(2).source()).contains("équiv-pour");
    }

    @Test
    void should_deserialise_form_of_when_sense_is_an_inflection() throws Exception {
        // Given
        String jsonl = """
                {"word":"suis","lang_code":"la","pos":"adj","senses":[\
                {"glosses":["Datif pluriel de suus."],"form_of":[{"word":"suus"}],"tags":["form-of"]},\
                {"glosses":["Ablatif pluriel de suus."],"form_of":[{"word":"suus"}],"tags":["form-of"]}]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "la")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        WiktionaryEntry entry = result.getFirst();
        assertThat(entry.senses())
                .hasSize(2)
                .allSatisfy(sense -> assertThat(sense.formOf())
                        .extracting(WiktionaryFormOf::word)
                        .containsExactly("suus"));
    }

    @Test
    void should_deserialise_headline_etymology_labels_and_examples_when_an_edition_carries_them() throws Exception {
        // Given
        String jsonl = """
                {"word":"chat","lang_code":"en","pos":"noun","pos_title":"Nom commun","tags":["masculine"],\
                "sounds":[{"ipa":"\\\\ˈtʃæt\\\\","audio":"En-us-chat.ogg"}],"etymology_texts":["Apocope de chatter."],\
                "synonyms":[{"word":"talk","sense":"discussion"}],"antonyms":[{"word":"silence"}],\
                "senses":[{"glosses":["Clavardage, tchat."],"tags":["familiar"],"raw_tags":["Internet"],\
                "topics":["computing"],"examples":[{"text":"Save your chat.","bold_text_offsets":[[10,14]],\
                "translation":"Sauvegardez votre tchat.","ref":"Manual"}]}]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "en")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        WiktionaryEntry entry = result.getFirst();
        assertThat(entry.posTitle()).isEqualTo("Nom commun");
        assertThat(entry.tags()).containsExactly("masculine");
        assertThat(entry.sounds()).extracting(WiktionarySound::ipa).containsExactly("\\ˈtʃæt\\");
        assertThat(entry.etymologyTexts()).containsExactly("Apocope de chatter.");
        assertThat(entry.synonyms()).extracting(WiktionaryLinkage::word).containsExactly("talk");
        assertThat(entry.antonyms()).extracting(WiktionaryLinkage::word).containsExactly("silence");
        WiktionarySense sense = entry.senses().getFirst();
        assertThat(sense.tags()).containsExactly("familiar");
        assertThat(sense.rawTags()).containsExactly("Internet");
        assertThat(sense.topics()).containsExactly("computing");
        WiktionaryExample example = sense.examples().getFirst();
        assertThat(example.translation()).isEqualTo("Sauvegardez votre tchat.");
        assertThat(example.boldTextOffsets()).containsExactly(List.of(10, 14));
    }

    @Test
    void should_deserialise_single_etymology_text_and_raw_glosses_when_the_english_edition_carries_them() throws Exception {
        // Given
        String jsonl = """
                {"word":"free","lang_code":"en","pos":"noun","etymology_text":"From Middle English free.",\
                "senses":[{"glosses":["A free transfer."],"raw_glosses":["(soccer) A free transfer."],\
                "synonyms":[{"word":"bosman"}],"examples":[{"text":"Left on a free.","english":"Sans indemnité."}]}]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "en")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        WiktionaryEntry entry = result.getFirst();
        assertThat(entry.etymologyText()).isEqualTo("From Middle English free.");
        assertThat(entry.etymologyTexts()).isEmpty();
        assertThat(entry.sounds()).isEmpty();
        WiktionarySense sense = entry.senses().getFirst();
        assertThat(sense.rawGlosses()).containsExactly("(soccer) A free transfer.");
        assertThat(sense.synonyms()).extracting(WiktionaryLinkage::word).containsExactly("bosman");
        assertThat(sense.examples().getFirst().english()).isEqualTo("Sans indemnité.");
    }

    @Test
    void should_default_form_of_to_empty_when_field_missing() throws Exception {
        // Given
        String jsonl = """
                {"word":"suus","lang_code":"la","pos":"adj","senses":[{"glosses":["Son, sa."]}]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "la")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        WiktionarySense sense = result.getFirst().senses().getFirst();
        assertThat(sense.formOf()).isEmpty();
    }

    @Test
    void should_default_forms_to_empty_when_field_missing() throws Exception {
        // Given
        String jsonl = """
                {"word":"foo","lang_code":"el","senses":[]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().forms()).isEmpty();
        // Jackson's Nulls.AS_EMPTY converts a missing String field to "" rather than null.
        // The renderer treats empty pos the same as null (neither equals "verb").
        assertThat(result.getFirst().pos()).isEmpty();
    }

    private Path writeGzippedJsonl(String jsonl) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);
             Writer writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            writer.write(jsonl);
        }
        Path dump = tmp.resolve("dump.jsonl.gz");
        Files.write(dump, bos.toByteArray());
        return dump;
    }
}
