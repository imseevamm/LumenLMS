package com.lms;

/**
 * Entry point for the packaged (shaded) jar. A main class that extends javafx.application.Application
 * fails on the plain classpath with "JavaFX runtime components are missing"; launching through this
 * plain class avoids that. In the IDE / "mvn javafx:run" the app still starts via {@link Main}.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Main.main(args);
    }
}
