package partyaggro;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

/** Localization shortcut for category "partyaggro" (resources/locale/*.lang). */
public final class L {
    private L() {
    }

    public static GameMessage m(String key) {
        return new LocalMessage("partyaggro", key);
    }

    public static GameMessage msg(String key, String... replacements) {
        return new LocalMessage("partyaggro", key, (Object[]) replacements);
    }

    public static String t(String key) {
        try {
            return m(key).translate();
        } catch (Throwable t) {
            return key;
        }
    }

    public static String tf(String key, String... replacements) {
        try {
            return msg(key, replacements).translate();
        } catch (Throwable t) {
            return key;
        }
    }
}
