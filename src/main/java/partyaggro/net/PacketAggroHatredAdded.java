package partyaggro.net;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import partyaggro.PartyAggroMod;
import partyaggro.data.AggroServerSection;
import partyaggro.data.AggroSource;

/** server -> client: a player was auto-added to the hatred list, persist it locally. */
public class PacketAggroHatredAdded extends Packet {
    public final long worldId;
    public final long playerAuth;
    public final String playerName;
    public final String source;

    public PacketAggroHatredAdded(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.worldId = reader.getNextLong();
        this.playerAuth = reader.getNextLong();
        this.playerName = reader.getNextString();
        this.source = reader.getNextString();
    }

    public PacketAggroHatredAdded(long worldId, long playerAuth, String playerName, AggroSource source) {
        this.worldId = worldId;
        this.playerAuth = playerAuth;
        this.playerName = playerName == null ? "Unknown" : playerName;
        this.source = source.name();
        PacketWriter writer = new PacketWriter(this);
        writer.putNextLong(worldId);
        writer.putNextLong(playerAuth);
        writer.putNextString(this.playerName);
        writer.putNextString(this.source);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        try {
            AggroServerSection section = PartyAggroMod.CONFIG.get(this.worldId);
            section.addHatred(this.playerAuth, AggroSource.valueOf(this.source));
            PartyAggroMod.save();
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to apply hatred update: " + t);
        }
    }
}
