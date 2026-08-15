package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Theme;
import javafx.application.ColorScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SystemThemeTest {

    @Test
    void should_pick_cupertino_dark_when_the_system_scheme_is_dark() {
        // When / Then
        assertThat(SystemTheme.themeFor(ColorScheme.DARK))
                .isInstanceOf(CupertinoDark.class)
                .matches(Theme::isDarkMode);
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
                .extracting(Theme::getUserAgentStylesheet)
                .asString()
                .endsWith("cupertino-light.css");
    }

    @Test
    void should_ship_the_stylesheet_that_bridges_the_app_styles_to_the_theme_tokens() {
        // When / Then
        assertThat(SystemTheme.class.getResource(SystemTheme.STYLESHEET)).isNotNull();
    }
}
