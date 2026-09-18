package partyaggro.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Per-server aggro state. Pure Java, no game dependencies, so it is unit-testable. */
public class AggroServerSection {
    public boolean enabled = true;
    public boolean autoAddOnPlayerAttacked = false;
    public boolean autoAddOnPartyMemberAttacked = false;
    public boolean autoAddOnPlayerAttacks = false;

    public final LinkedHashSet<Long> hatred = new LinkedHashSet<Long>();
    public final HashMap<Long, AggroSource> hatredSource = new HashMap<Long, AggroSource>();
    public final LinkedHashSet<Long> whitelist = new LinkedHashSet<Long>();
    public final HashMap<Long, Long> firstSeen = new HashMap<Long, Long>();

    public boolean isHatred(long auth) {
        return this.hatred.contains(auth);
    }

    public boolean isWhitelist(long auth) {
        return this.whitelist.contains(auth);
    }

    /** Adds a player to the hatred list. Removes from whitelist (mutually exclusive). */
    public void addHatred(long auth, AggroSource source) {
        this.whitelist.remove(auth);
        this.hatred.add(auth);
        this.hatredSource.put(auth, source);
    }

    /** Adds a player to the whitelist. Removes from hatred (mutually exclusive). */
    public void addWhitelist(long auth) {
        this.hatred.remove(auth);
        this.hatredSource.remove(auth);
        this.whitelist.add(auth);
    }

    /** Removes a player from both lists. */
    public void clearPlayer(long auth) {
        this.hatred.remove(auth);
        this.hatredSource.remove(auth);
        this.whitelist.remove(auth);
    }

    public void clearAllHatred() {
        this.hatred.clear();
        this.hatredSource.clear();
    }

    /** Hatred minus whitelist. Whitelist always wins. */
    public Set<Long> effectiveHatred() {
        LinkedHashSet<Long> out = new LinkedHashSet<Long>(this.hatred);
        out.removeAll(this.whitelist);
        return out;
    }

    public AggroSource sourceOf(long auth) {
        return this.hatredSource.get(auth);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.enabled ? 1 : 0).append('|');
        sb.append(this.autoAddOnPlayerAttacked ? 1 : 0).append('|');
        sb.append(this.autoAddOnPartyMemberAttacked ? 1 : 0).append('|');
        sb.append(this.autoAddOnPlayerAttacks ? 1 : 0).append('|');
        sb.append(joinSet(this.hatred)).append('|');
        sb.append(joinSet(this.whitelist)).append('|');
        sb.append(joinSources(this.hatredSource)).append('|');
        sb.append(joinTimes(this.firstSeen));
        return sb.toString();
    }

    public static AggroServerSection deserialize(String line) {
        AggroServerSection s = new AggroServerSection();
        if (line == null) {
            return s;
        }
        String[] p = line.split("\\|", -1);
        if (p.length >= 4) {
            s.enabled = "1".equals(p[0]);
            s.autoAddOnPlayerAttacked = "1".equals(p[1]);
            s.autoAddOnPartyMemberAttacked = "1".equals(p[2]);
            s.autoAddOnPlayerAttacks = "1".equals(p[3]);
        }
        if (p.length >= 5) {
            parseSet(p[4], s.hatred);
        }
        if (p.length >= 6) {
            parseSet(p[5], s.whitelist);
        }
        if (p.length >= 7) {
            parseSources(p[6], s.hatredSource);
        }
        if (p.length >= 8) {
            parseTimes(p[7], s.firstSeen);
        }
        return s;
    }

    private static String joinSet(Collection<Long> values) {
        StringBuilder sb = new StringBuilder();
        for (Long v : values) {
            if (v == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(v.longValue());
        }
        return sb.toString();
    }

    private static void parseSet(String raw, Collection<Long> out) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String part : raw.split(",")) {
            try {
                out.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static String joinSources(HashMap<Long, AggroSource> map) {
        StringBuilder sb = new StringBuilder();
        List<Long> keys = new ArrayList<Long>(map.keySet());
        java.util.Collections.sort(keys);
        for (Long k : keys) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(k.longValue()).append(':').append(map.get(k).name());
        }
        return sb.toString();
    }

    private static void parseSources(String raw, HashMap<Long, AggroSource> out) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String part : raw.split(",")) {
            int idx = part.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            try {
                long auth = Long.parseLong(part.substring(0, idx).trim());
                out.put(auth, AggroSource.valueOf(part.substring(idx + 1).trim()));
            } catch (Exception ignored) {
            }
        }
    }

    private static String joinTimes(HashMap<Long, Long> map) {
        StringBuilder sb = new StringBuilder();
        List<Long> keys = new ArrayList<Long>(map.keySet());
        java.util.Collections.sort(keys);
        for (Long k : keys) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(k.longValue()).append(':').append(map.get(k).longValue());
        }
        return sb.toString();
    }

    private static void parseTimes(String raw, HashMap<Long, Long> out) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String part : raw.split(",")) {
            int idx = part.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            try {
                long auth = Long.parseLong(part.substring(0, idx).trim());
                out.put(auth, Long.parseLong(part.substring(idx + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
