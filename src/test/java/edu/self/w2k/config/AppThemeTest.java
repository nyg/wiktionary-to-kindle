package edu.self.w2k.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AppThemeTest {

    @ParameterizedTest
    @CsvSource({
            "Mac OS X, CUPERTINO",
            "macOS, CUPERTINO",
            "Windows 11, JAVAFX",
            "Linux, JAVAFX",
    })
    void should_default_to_cupertino_on_macos_only(String osName, AppTheme expected) {
        // When / Then
        assertThat(AppTheme.defaultFor(osName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "")
    void should_default_to_javafx_when_the_os_is_unknown(String osName) {
        // When / Then
        assertThat(AppTheme.defaultFor(osName)).isEqualTo(AppTheme.JAVAFX);
    }

    @ParameterizedTest
    @EnumSource(AppTheme.class)
    void should_read_back_every_key_it_writes(AppTheme theme) {
        // When / Then
        assertThat(AppTheme.fromKey(theme.key())).contains(theme);
    }

    @Test
    void should_reject_a_key_no_theme_uses() {
        // When / Then
        assertThat(AppTheme.fromKey("nord")).isEmpty();
        assertThat(AppTheme.fromKey(null)).isEmpty();
    }

    @Test
    void should_show_the_theme_label_in_a_picker() {
        // When / Then
        assertThat(AppTheme.CUPERTINO).hasToString("AtlantaFX — Cupertino");
        assertThat(AppTheme.JAVAFX).hasToString("JavaFX");
    }
}
