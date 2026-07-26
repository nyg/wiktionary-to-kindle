package edu.self.w2k.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppVersionTest {

    @Test
    void should_read_the_filtered_version_from_the_build() {
        // When
        String version = AppVersion.get();

        // Then — proves maven-resources-plugin filtering is actually wired, not just the file present
        assertThat(version).isNotEqualTo(AppVersion.UNKNOWN)
                .matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?");
    }

    @Test
    void should_fall_back_when_the_placeholder_was_not_filtered() {
        // Given running straight from src/main/resources leaves the literal token in place
        assertThat(AppVersion.sanitise("${project.version}")).isEqualTo(AppVersion.UNKNOWN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_fall_back_when_value_is_absent(String value) {
        // When / Then
        assertThat(AppVersion.sanitise(value)).isEqualTo(AppVersion.UNKNOWN);
    }

    @Test
    void should_strip_whitespace_around_the_version() {
        // When / Then
        assertThat(AppVersion.sanitise("  2.0.0  ")).isEqualTo("2.0.0");
    }

    @Test
    void should_fall_back_when_the_resource_is_absent() {
        // When / Then
        assertThat(AppVersion.load("/no-such-resource.properties")).isEqualTo(AppVersion.UNKNOWN);
    }

    @Test
    void should_fall_back_rather_than_fail_class_init_when_properties_are_malformed() {
        // Given a truncated unicode escape, which Properties.load rejects with
        // IllegalArgumentException. Letting that escape would fail AppVersion's static initialiser
        // and turn a cosmetic version lookup into an ExceptionInInitializerError at startup.
        assertThat(AppVersion.load("/malformed-version.properties")).isEqualTo(AppVersion.UNKNOWN);
    }

    @Test
    void should_fall_back_when_the_resource_holds_no_version_key() {
        // Given binary content, which parses into entries without a usable version key
        assertThat(AppVersion.load("/edu/self/w2k/gui/icon.png")).isEqualTo(AppVersion.UNKNOWN);
    }
}
