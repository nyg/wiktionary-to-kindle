package edu.self.w2k.gui;

import java.util.Arrays;
import java.util.Locale;

/**
 * The window conventions of each desktop, after its own guidelines: Apple's HIG, Windows' Fluent
 * design and Win32 UX guide, the GNOME HIG and the KDE HIG. Linux is split by desktop rather than
 * treated as one, since GNOME and KDE disagree on nearly every point here.
 */
public enum DesktopPlatform {

    MACOS,
    WINDOWS,
    GNOME,
    KDE;

    public enum MenuStyle {
        SYSTEM_MENU_BAR,
        MENU_BAR,
        PRIMARY_MENU
    }

    public static DesktopPlatform detect(String osName, String currentDesktop) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.startsWith("mac")) {
            return MACOS;
        }
        if (os.startsWith("windows")) {
            return WINDOWS;
        }
        return isKde(currentDesktop) ? KDE : GNOME;
    }

    public static DesktopPlatform current() {
        return detect(System.getProperty("os.name"), System.getenv("XDG_CURRENT_DESKTOP"));
    }

    /** {@code XDG_CURRENT_DESKTOP} is a colon-separated list, such as {@code ubuntu:GNOME}. */
    private static boolean isKde(String currentDesktop) {
        return currentDesktop != null
                && Arrays.stream(currentDesktop.split(":")).anyMatch(desktop -> desktop.equalsIgnoreCase("KDE"));
    }

    public String styleClass() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * macOS and GNOME draw the app's header as the title bar. Windows and KDE keep the system title
     * bar: on Windows it is what provides Snap Layouts, and KDE's window manager decorates its own.
     */
    public boolean extendsIntoTitleBar() {
        return this == MACOS || this == GNOME;
    }

    public MenuStyle menuStyle() {
        return switch (this) {
            case MACOS -> MenuStyle.SYSTEM_MENU_BAR;
            case WINDOWS, KDE -> MenuStyle.MENU_BAR;
            case GNOME -> MenuStyle.PRIMARY_MENU;
        };
    }

    /** The page switcher is centred in a title bar, and leads a toolbar like a row of tabs. */
    public boolean centresPageSwitcher() {
        return extendsIntoTitleBar();
    }

    /** Windows and KDE put the affirmative button first; macOS and GNOME put it last. */
    public boolean putsPrimaryButtonFirst() {
        return this == WINDOWS || this == KDE;
    }

    /** Only Apple's and KDE's form layouts end a label with a colon. */
    public String labelSuffix() {
        return this == MACOS || this == KDE ? ":" : "";
    }

    /** Windows uses sentence case for buttons; the other three use title case. */
    public String startLabel() {
        return this == WINDOWS ? "Build dictionary" : "Build Dictionary";
    }

    public String revealLabel() {
        return switch (this) {
            case MACOS -> "Show in Finder";
            case WINDOWS -> "Open file location";
            case GNOME -> "Show in Files";
            case KDE -> "Open Containing Folder";
        };
    }

    public String settingsTitle() {
        return switch (this) {
            case MACOS, WINDOWS -> "Settings";
            case GNOME -> "Preferences";
            case KDE -> "Configure — Wiktionary to Kindle";
        };
    }

    /** GNOME's primary menu carries no Quit, so Ctrl+Q has to work on its own. */
    public boolean hasQuitShortcut() {
        return this == GNOME;
    }
}
