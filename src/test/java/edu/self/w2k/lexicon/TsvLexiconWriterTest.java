package edu.self.w2k.lexicon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsvLexiconWriterTest {

    private final TsvLexiconWriter writer = new TsvLexiconWriter();

    @Test
    void write_singleEntry(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("lexicon.txt");
        writer.write(file, Stream.of(new LexiconEntry("hello", "<b>greeting</b>")));

        String content = Files.readString(file);
        assertEquals("hello\t<b>greeting</b>\n", content);
    }

    @Test
    void write_multipleEntries(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("lexicon.txt");
        writer.write(file, Stream.of(
                new LexiconEntry("alpha", "first"),
                new LexiconEntry("beta", "second"),
                new LexiconEntry("gamma", "third")
        ));

        List<String> lines = Files.readAllLines(file);
        assertEquals(3, lines.size());
        assertEquals("alpha\tfirst", lines.get(0));
        assertEquals("beta\tsecond", lines.get(1));
        assertEquals("gamma\tthird", lines.get(2));
    }

    @Test
    void write_emptyStream(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("lexicon.txt");
        writer.write(file, Stream.empty());

        assertTrue(Files.readString(file).isEmpty());
    }

    @Test
    void write_tabInDefinition(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("lexicon.txt");
        writer.write(file, Stream.of(new LexiconEntry("word", "def\twith\ttabs")));

        String content = Files.readString(file);
        assertEquals("word\tdef\twith\ttabs\n", content);
    }
}
