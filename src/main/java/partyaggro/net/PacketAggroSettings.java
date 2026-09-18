package partyaggro.net;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import partyaggro.data.AggroServerSection;
import partyaggro.server.AggroLogic;
import partyaggro.server.AggroServerState;

/** client -> server: upload this client's per-server aggro settings. */
public class PacketAggroSettings extends Packet {
    public final long worldId;
    public final String sectionData;
    public final boolean runtimeDisabled;

    public PacketAggroSettings(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.worldId = reader.getNextLong();
        this.sectionData = reader.getNextString();
        this.runtimeDisabled = reader.getNextBoolean();
    }

    public PacketAggroSettings(long worldId, AggroServerSection section, boolean runtimeDisabled) {
        this.worldId = worldId;
        this.sectionData = section == null ? "" : section.serialize();
        this.runtimeDisabled = runtimeDisabled;
        PacketWriter writer = new PacketWriter(this);
        writer.putNextLong(worldId);
        writer.putNextString(this.sectionData);
        writer.putNextBoolean(runtimeDisabled);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
        AggroServerSection section = AggroServerSection.deserialize(this.sectionData);
        AggroServerState.set(client.authentication, this.worldId, section, this.runtimeDisabled);
        if (this.runtimeDisabled || !section.enabled) {
            AggroLogic.clearAnger(client);
        } else {
            AggroLogic.pruneEnemies(client);
        }
        partyaggro.util.Debug.log("recv settings from " + client.getName() + " auth=" + client.authentication
                + " world=" + this.worldId + " enabled=" + section.enabled
                + " a1=" + section.autoAddOnPlayerAttacked + " a2=" + section.autoAddOnPartyMemberAttacked
                + " a3=" + section.autoAddOnPlayerAttacks + " runtimeDisabled=" + this.runtimeDisabled
                + " hatred=" + section.hatred.size() + " whitelist=" + section.whitelist.size());
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
    }
}
