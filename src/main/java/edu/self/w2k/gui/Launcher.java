package edu.self.w2k.gui;

import javafx.application.Application;

/**
 * Entry point for the desktop app.
 * <p>
 * Exists solely so the main class is <em>not</em> the {@link Application} subclass. When the app runs
 * from the classpath rather than as a JPMS module — which it must, since Jackson, logback and picocli
 * are not modules — the JavaFX launcher rejects a main class that extends {@code Application} with
 * "JavaFX runtime components are missing". Delegating from a plain class sidesteps that check.
 * <p>
 * This is the class jpackage is pointed at; the shaded CLI JAR keeps {@code edu.self.w2k.CLI} as its
 * manifest main class.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
