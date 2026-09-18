package partyaggro.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Simple appender to %APPDATA%/Necesse/partyaggro-debug.log for diagnostics. */
public final class Debug {
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS");

    private Debug() {
    }

    public static synchronized void log(String message) {
        try {
            File file = new File(System.getenv("APPDATA"), "Necesse/partyaggro-debug.log");
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            FileWriter writer = new FileWriter(file, true);
            PrintWriter printer = new PrintWriter(writer);
            printer.println(TIME.format(new Date()) + " " + message);
            printer.close();
        } catch (Throwable ignored) {
        }
    }
}
