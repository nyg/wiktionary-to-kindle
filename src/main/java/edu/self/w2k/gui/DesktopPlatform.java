package edu.self.w2k.gui;

import java.util.Locale;

public enum DesktopPlatform {

    MACOS("Show in Finder"),
    WINDOWS("Show in File Explorer"),
    OTHER("Open Folder");

    private final String revealLabel;

    DesktopPlatform(String revealLabel) {
        this.revealLabel = revealLabel;
    }

    public static DesktopPlatform detect(String osName) {
        String name = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (name.startsWith("mac")) {
            return MACOS;
        }
        return name.startsWith("windows") ? WINDOWS : OTHER;
    }

    public static DesktopPlatform current() {
        return detect(System.getProperty("os.name"));
    }

    public String revealLabel() {
        return revealLabel;
    }

    public boolean hasSystemMenuBar() {
        return this == MACOS;
    }

    /**
     * Only macOS keeps its native window buttons in an extended stage; elsewhere JavaFX paints its own,
     * which match neither Windows nor any Linux desktop.
     */
    public boolean extendsIntoTitleBar() {
        return this == MACOS;
    }
}
