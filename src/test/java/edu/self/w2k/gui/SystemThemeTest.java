package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import javafx.application.ColorScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SystemThemeTest {

    @ParameterizedTest
    @CsvSource({
            "Mac OS X, true",
            "macOS, true",
            "Windows 11, false",
            "Linux, false",
    })
    void should_theme_macos_only(String osName, boolean expected) {
        // When / Then
        assertThat(SystemTheme.appliesTo(osName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "")
    void should_leave_the_platform_default_when_the_os_is_unknown(String osName) {
        // When / Then
        assertThat(SystemTheme.appliesTo(osName)).isFalse();
    }

    @Test
    void should_pick_cupertino_dark_when_the_system_scheme_is_dark() {
        // When / Then
        assertThat(SystemTheme.themeFor(ColorScheme.DARK))
                .isInstanceOf(CupertinoDark.class)
                .matches(atlantafx.base.theme.Theme::isDarkMode);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "LIGHT")
    void should_pick_cupertino_light_when_the_system_scheme_is_light_or_unreported(String scheme) {
        // Given
        ColorScheme colorScheme = scheme == null ? null : ColorScheme.valueOf(scheme);

        // When / Then
        assertThat(SystemTheme.themeFor(colorScheme))
                .isInstanceOf(CupertinoLight.class)
                .extracting(atlantafx.base.theme.Theme::getUserAgentStylesheet)
                .asString()
                .endsWith("cupertino-light.css");
    }

    @Test
    void should_ship_the_stylesheet_that_bridges_the_app_styles_to_the_theme_tokens() {
        // When / Then
        assertThat(SystemTheme.class.getResource(SystemTheme.STYLESHEET)).isNotNull();
    }
}
