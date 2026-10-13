package logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Debug {
    private static final PrintWriter logger;

    static {
        try {
            logger = new PrintWriter(
                    new FileWriter("debug.log", true)
            );
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open logs", e);
        }
    }

    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        logger.println("[" + timestamp + "] " + message);

        if (logger.checkError()) {
            System.err.println("Failed to write to server.log");
        }
    }
}
