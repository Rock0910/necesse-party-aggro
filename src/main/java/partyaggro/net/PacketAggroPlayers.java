package partyaggro.net;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import partyaggro.data.AggroPlayerRef;
import partyaggro.ui.AggroManageCache;

/** server -> client: the list of players known to this server. */
public class PacketAggroPlayers extends Packet {
    public final List<AggroPlayerRef> players = new ArrayList<AggroPlayerRef>();

    public PacketAggroPlayers(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        int count = reader.getNextInt();
        for (int i = 0; i < count; i++) {
            long auth = reader.getNextLong();
            String name = reader.getNextString();
            long firstSeen = reader.getNextLong();
            long lastOnline = reader.getNextLong();
            this.players.add(new AggroPlayerRef(auth, name, firstSeen, lastOnline));
        }
    }

    public PacketAggroPlayers(List<AggroPlayerRef> players) {
        if (players != null) {
            this.players.addAll(players);
        }
        PacketWriter writer = new PacketWriter(this);
        writer.putNextInt(this.players.size());
        for (AggroPlayerRef p : this.players) {
            writer.putNextLong(p.authentication);
            writer.putNextString(p.name == null ? "Unknown" : p.name);
            writer.putNextLong(p.firstSeen);
            writer.putNextLong(p.lastOnline);
        }
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        AggroManageCache.set(this.players);
    }
}
