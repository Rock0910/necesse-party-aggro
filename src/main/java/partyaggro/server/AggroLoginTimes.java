package partyaggro.server;

import java.util.HashMap;

/** Server-side in-memory login times per authentication: {firstSeen, lastSeen}. */
public final class AggroLoginTimes {
    private static final HashMap<Long, long[]> TIMES = new HashMap<Long, long[]>();

    private AggroLoginTimes() {
    }

    public static synchronized void onConnect(long authentication) {
        long now = System.currentTimeMillis();
        long[] times = TIMES.get(authentication);
        if (times == null) {
            TIMES.put(authentication, new long[] { now, now });
        } else {
            times[1] = now;
        }
    }

    public static synchronized void onDisconnect(long authentication) {
        long now = System.currentTimeMillis();
        long[] times = TIMES.get(authentication);
        if (times == null) {
            TIMES.put(authentication, new long[] { 0L, now });
        } else {
            times[1] = now;
        }
    }

    public static synchronized long firstSeen(long authentication) {
        long[] times = TIMES.get(authentication);
        return times == null ? 0L : times[0];
    }

    public static synchronized long lastSeen(long authentication) {
        long[] times = TIMES.get(authentication);
        return times == null ? 0L : times[1];
    }
}
