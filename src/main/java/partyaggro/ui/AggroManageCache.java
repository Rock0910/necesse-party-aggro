package partyaggro.ui;

import java.util.ArrayList;
import java.util.List;

import partyaggro.data.AggroPlayerRef;

/** Client-side cache of the server's known players. */
public final class AggroManageCache {
    public static volatile int version = 0;
    public static volatile List<AggroPlayerRef> players = new ArrayList<AggroPlayerRef>();

    private AggroManageCache() {
    }

    public static synchronized void set(List<AggroPlayerRef> list) {
        players = list == null ? new ArrayList<AggroPlayerRef>() : list;
        version++;
    }
}
