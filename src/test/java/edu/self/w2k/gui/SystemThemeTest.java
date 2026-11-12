package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import edu.self.w2k.config.AppTheme;
import javafx.application.ColorScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SystemThemeTest {

    @Test
    void should_pick_cupertino_dark_when_the_system_scheme_is_dark() {
        // When / Then
        assertThat(SystemTheme.themeFor(AppTheme.CUPERTINO, ColorScheme.DARK))
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
        assertThat(SystemTheme.themeFor(AppTheme.CUPERTINO, colorScheme))
                .isInstanceOf(CupertinoLight.class)
                .extracting(Theme::getUserAgentStylesheet)
                .asString()
                .endsWith("cupertino-light.css");
    }

    @Test
    void should_pick_primer_dark_when_primer_is_chosen_and_the_system_scheme_is_dark() {
        // When
        Theme theme = SystemTheme.themeFor(AppTheme.PRIMER, ColorScheme.DARK);

        // Then
        assertThat(theme).isInstanceOf(PrimerDark.class).matches(Theme::isDarkMode);
    }

    @Test
    void should_pick_primer_light_when_primer_is_chosen_and_the_system_scheme_is_light() {
        // When
        Theme theme = SystemTheme.themeFor(AppTheme.PRIMER, ColorScheme.LIGHT);

        // Then
        assertThat(theme).isInstanceOf(PrimerLight.class);
    }

    @Test
    void should_ship_the_stylesheet_that_bridges_the_app_styles_to_the_theme_tokens() {
        // When / Then
        assertThat(SystemTheme.class.getResource(SystemTheme.STYLESHEET)).isNotNull();
    }
}
