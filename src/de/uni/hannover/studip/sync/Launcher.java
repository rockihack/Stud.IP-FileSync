package de.uni.hannover.studip.sync;

import javafx.application.Application;

/**
 * @author Lennart Glauer
 */
public final class Launcher {

    /**
     * Launcher main method
     *
     * @see <a href="http://mail.openjdk.java.net/pipermail/openjfx-dev/2018-June/021977.html">launching JavaFX in 11</a>
     * @param args
     */
    public static void main(final String... args) {
        Application.launch(Main.class, args);
    }

}
