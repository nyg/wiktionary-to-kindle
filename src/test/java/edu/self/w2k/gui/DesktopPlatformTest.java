package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import edu.self.w2k.gui.DesktopPlatform.MenuStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DesktopPlatformTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Mac OS X, null, MACOS",
            "macOS, KDE, MACOS",
            "Windows 11, null, WINDOWS",
            "Linux, GNOME, GNOME",
            "Linux, ubuntu:GNOME, GNOME",
            "Linux, KDE, KDE",
            "Linux, kde, KDE",
            "Linux, X-Cinnamon, GNOME",
            "Linux, null, GNOME",
            "FreeBSD, KDE, KDE",
            "null, null, GNOME",
    })
    void should_detect_the_desktop_when_given_an_os_name_and_current_desktop(String osName, String currentDesktop,
                                                                             DesktopPlatform expected) {
        // When
        DesktopPlatform unit = DesktopPlatform.detect(osName, currentDesktop);

        // Then
        assertThat(unit).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, true, SYSTEM_MENU_BAR",
            "WINDOWS, false, MENU_BAR",
            "GNOME, true, PRIMARY_MENU",
            "KDE, false, MENU_BAR",
    })
    void should_draw_its_own_title_bar_only_when_the_desktop_expects_one(DesktopPlatform unit, boolean extended,
                                                                         MenuStyle menuStyle) {
        // When
        boolean extendsIntoTitleBar = unit.extendsIntoTitleBar();
        boolean centred = unit.centresPageSwitcher();
        MenuStyle style = unit.menuStyle();

        // Then
        assertThat(extendsIntoTitleBar).isEqualTo(extended);
        assertThat(centred).isEqualTo(extended);
        assertThat(style).isEqualTo(menuStyle);
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, Build Dictionary, Show in Finder, ':', false",
            "WINDOWS, Build dictionary, Open file location, '', true",
            "GNOME, Build Dictionary, Show in Files, '', false",
            "KDE, Build Dictionary, Open Containing Folder, ':', true",
    })
    void should_word_and_order_the_controls_when_following_the_desktop_guidelines(DesktopPlatform unit,
                                                                                  String startLabel,
                                                                                  String revealLabel,
                                                                                  String labelSuffix,
                                                                                  boolean primaryFirst) {
        // When
        String start = unit.startLabel();
        String reveal = unit.revealLabel();
        String suffix = unit.labelSuffix();
        boolean first = unit.putsPrimaryButtonFirst();

        // Then
        assertThat(start).isEqualTo(startLabel);
        assertThat(reveal).isEqualTo(revealLabel);
        assertThat(suffix).isEqualTo(labelSuffix);
        assertThat(first).isEqualTo(primaryFirst);
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, Settings, false, macos",
            "WINDOWS, Settings, false, windows",
            "GNOME, Preferences, true, gnome",
            "KDE, Configure — Wiktionary to Kindle, false, kde",
    })
    void should_name_settings_and_bind_quit_when_following_the_desktop_guidelines(DesktopPlatform unit,
                                                                                  String settingsTitle,
                                                                                  boolean quitShortcut,
                                                                                  String styleClass) {
        // When
        String title = unit.settingsTitle();
        boolean quit = unit.hasQuitShortcut();
        String style = unit.styleClass();

        // Then
        assertThat(title).isEqualTo(settingsTitle);
        assertThat(quit).isEqualTo(quitShortcut);
        assertThat(style).isEqualTo(styleClass);
    }
}
