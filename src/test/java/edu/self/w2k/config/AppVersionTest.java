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
}
