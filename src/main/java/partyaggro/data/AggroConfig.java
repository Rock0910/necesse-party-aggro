package partyaggro.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Local (client) config. Global hotkey plus one {@link AggroServerSection} per server,
 * keyed by the client's world unique ID.
 */
public class AggroConfig {
    public final HashMap<Long, String> serverNames = new HashMap<Long, String>();
    public final HashMap<Long, AggroServerSection> servers = new HashMap<Long, AggroServerSection>();

    /** Returns the section for the given server, creating a default one if needed. */
    public AggroServerSection get(long worldId) {
        AggroServerSection section = this.servers.get(worldId);
        if (section == null) {
            section = new AggroServerSection();
            this.servers.put(worldId, section);
        }
        return section;
    }

    /** Returns the section only if it already exists. */
    public AggroServerSection getIfPresent(long worldId) {
        return this.servers.get(worldId);
    }

    public void setServerName(long worldId, String name) {
        if (name != null && !name.isEmpty()) {
            this.serverNames.put(worldId, name.replace('|', ' '));
        }
    }

    public String serverName(long worldId) {
        String name = this.serverNames.get(worldId);
        return name == null ? "" : name;
    }

    public List<String> serialize() {
        ArrayList<String> out = new ArrayList<String>();
        for (Map.Entry<Long, AggroServerSection> e : this.servers.entrySet()) {
            out.add("S|" + e.getKey() + "|" + serverName(e.getKey()) + "|" + e.getValue().serialize());
        }
        return out;
    }

    public void deserialize(List<String> lines) {
        this.servers.clear();
        this.serverNames.clear();
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            if (line == null || !line.startsWith("S|")) {
                continue;
            }
            try {
                String[] p = line.split("\\|", 4);
                long worldId = Long.parseLong(p[1]);
                if (p.length >= 4) {
                    this.serverNames.put(worldId, p[2]);
                    this.servers.put(worldId, AggroServerSection.deserialize(p[3]));
                } else if (p.length == 3) {
                    this.servers.put(worldId, AggroServerSection.deserialize(""));
                }
            } catch (Exception ignored) {
            }
        }
    }
}
