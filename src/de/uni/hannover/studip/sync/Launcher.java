package de.uni.hannover.studip.sync;

import de.uni.hannover.studip.sync.utils.Cli;
import javafx.application.Application;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * @author Lennart Glauer
 */
public final class Launcher {

    @SuppressWarnings("unused")
    private static ServerSocket globalAppMutex;

    /**
     * Launcher main method
     *
     * @see <a href="http://mail.openjdk.java.net/pipermail/openjfx-dev/2018-June/021977.html">launching JavaFX in 11</a>
     * @param args
     */
    public static void main(final String... args) {
        try {
            // Acquire system wide app mutex to allow only one running instance.
            globalAppMutex = new ServerSocket(9001, 1, InetAddress.getLoopbackAddress());

            Cli.handleArgs(args);
            Application.launch(Main.class, args);

        } catch (final IOException e) {
            System.out.println("FileSync läuft bereits.");
            System.exit(1);
        }
    }

}
