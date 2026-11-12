package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DesktopPlatformTest {

    @ParameterizedTest
    @CsvSource({
            "Mac OS X, MACOS",
            "macOS, MACOS",
            "Windows 11, WINDOWS",
            "Linux, OTHER",
            "FreeBSD, OTHER",
    })
    void should_detect_the_desktop_when_given_an_os_name(String osName, DesktopPlatform expected) {
        // When
        DesktopPlatform unit = DesktopPlatform.detect(osName);

        // Then
        assertThat(unit).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "")
    void should_fall_back_to_other_when_the_os_is_unknown(String osName) {
        // When
        DesktopPlatform unit = DesktopPlatform.detect(osName);

        // Then
        assertThat(unit).isEqualTo(DesktopPlatform.OTHER);
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, Show in Finder",
            "WINDOWS, Show in File Explorer",
            "OTHER, Open Folder",
    })
    void should_name_the_file_manager_when_labelling_the_reveal_button(DesktopPlatform unit, String expected) {
        // When
        String label = unit.revealLabel();

        // Then
        assertThat(label).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, true",
            "WINDOWS, false",
            "OTHER, false",
    })
    void should_extend_into_the_title_bar_and_use_the_system_menu_bar_when_on_macos_only(DesktopPlatform unit,
                                                                                        boolean expected) {
        // When
        boolean extended = unit.extendsIntoTitleBar();
        boolean systemMenuBar = unit.hasSystemMenuBar();

        // Then
        assertThat(extended).isEqualTo(expected);
        assertThat(systemMenuBar).isEqualTo(expected);
    }
}
