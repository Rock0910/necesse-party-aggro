package partyaggro.server;

import java.util.HashMap;

import partyaggro.data.AggroServerSection;

/**
 * Server-side, in-memory settings per connected player (key = authentication).
 * Settings come from clients via PacketAggroSettings and are not persisted server-side.
 */
public final class AggroServerState {
    private static final class Entry {
        final long worldId;
        final AggroServerSection section;
        final boolean runtimeDisabled;

        Entry(long worldId, AggroServerSection section, boolean runtimeDisabled) {
            this.worldId = worldId;
            this.section = section;
            this.runtimeDisabled = runtimeDisabled;
        }
    }

    private static final HashMap<Long, Entry> BY_AUTH = new HashMap<Long, Entry>();

    private AggroServerState() {
    }

    public static synchronized void set(long authentication, long worldId, AggroServerSection section, boolean runtimeDisabled) {
        if (section == null) {
            BY_AUTH.remove(authentication);
        } else {
            BY_AUTH.put(authentication, new Entry(worldId, section, runtimeDisabled));
        }
    }

    public static synchronized AggroServerSection get(long authentication) {
        Entry entry = BY_AUTH.get(authentication);
        return entry == null ? null : entry.section;
    }

    public static synchronized boolean isRuntimeDisabled(long authentication) {
        Entry entry = BY_AUTH.get(authentication);
        return entry == null || entry.runtimeDisabled;
    }

    public static synchronized long worldId(long authentication) {
        Entry entry = BY_AUTH.get(authentication);
        return entry == null ? 0L : entry.worldId;
    }

    public static synchronized void remove(long authentication) {
        BY_AUTH.remove(authentication);
    }
}
