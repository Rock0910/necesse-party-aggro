package partyaggro.server;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import necesse.engine.network.server.SavedServerClientData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.world.WorldFile;
import partyaggro.data.AggroPlayerRef;
import partyaggro.util.Debug;

/** Builds the server's known-player list (login records) with first/last seen times. */
public final class AggroPlayerList {
    private AggroPlayerList() {
    }

    public static List<AggroPlayerRef> build(Server server) {
        LinkedHashMap<Long, AggroPlayerRef> map = new LinkedHashMap<Long, AggroPlayerRef>();
        if (server == null || server.world == null) {
            return new ArrayList<AggroPlayerRef>();
        }
        try {
            for (SavedServerClientData data : server.getSavedClients()) {
                long[] times = times(server, data.authentication);
                long first = AggroLoginTimes.firstSeen(data.authentication);
                long last = AggroLoginTimes.lastSeen(data.authentication);
                if (first == 0L) {
                    first = times[0];
                }
                if (last == 0L) {
                    last = times[1];
                }
                map.put(data.authentication,
                        new AggroPlayerRef(data.authentication, cleanName(data.name), first, last));
            }
        } catch (Throwable t) {
            Debug.log("error listing saved players: " + t);
        }

        // Prefer live names for connected clients, and include new clients not yet saved.
        try {
            long now = System.currentTimeMillis();
            for (ServerClient client : server.getClients()) {
                if (client == null) {
                    continue;
                }
                long auth = client.authentication;
                String live = cleanName(client.getName());
                AggroPlayerRef existing = map.get(auth);
                long first = AggroLoginTimes.firstSeen(auth);
                if (first == 0L) {
                    first = existing == null ? 0L : existing.firstSeen;
                }
                if (first == 0L) {
                    first = now;
                }
                // Online right now: name and last-online are current.
                map.put(auth, new AggroPlayerRef(auth, live, first, now));
            }
        } catch (Throwable t) {
            Debug.log("error listing live players: " + t);
        }

        ArrayList<AggroPlayerRef> out = new ArrayList<AggroPlayerRef>(map.values());
        Collections.sort(out, new Comparator<AggroPlayerRef>() {
            @Override
            public int compare(AggroPlayerRef a, AggroPlayerRef b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        Debug.log("player list built: " + out.size());
        return out;
    }

    private static String cleanName(String name) {
        if (name == null || name.isEmpty() || "N/A".equals(name)) {
            return "N/A";
        }
        return name;
    }

    /** Returns {firstSeen, lastOnline}. */
    private static long[] times(Server server, long authentication) {
        long firstSeen = 0L;
        long lastOnline = 0L;
        try {
            WorldFile worldFile = server.world.fileSystem.getPlayerFile(authentication);
            File file = worldFile == null ? null : worldFile.toFile();
            if (file != null && file.exists()) {
                lastOnline = file.lastModified();
                try {
                    firstSeen = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
                            .creationTime().toMillis();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        if (firstSeen == 0L) {
            firstSeen = lastOnline;
        }
        return new long[] { firstSeen, lastOnline };
    }
}
