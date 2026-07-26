package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PreferencesDialogMemoryTest {

    @Test
    void should_state_that_heap_is_fixed_at_startup() {
        // When
        String text = PreferencesDialog.memoryText(8192);

        // Then — the wording has to explain why this is not editable, or it reads as a broken field
        assertThat(text).contains("8192 MB").contains("set at startup");
    }

    @Test
    void should_warn_about_large_editions_when_heap_is_low() {
        // When
        String text = PreferencesDialog.memoryText(PreferencesDialog.LOW_HEAP_THRESHOLD_MB - 1);

        // Then
        assertThat(text).contains("run out of memory").contains("English");
    }

    @Test
    void should_not_warn_when_heap_is_at_the_threshold() {
        // When
        String text = PreferencesDialog.memoryText(PreferencesDialog.LOW_HEAP_THRESHOLD_MB);

        // Then
        assertThat(text).doesNotContain("run out of memory");
    }
}
