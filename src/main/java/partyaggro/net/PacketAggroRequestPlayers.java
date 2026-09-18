package partyaggro.net;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import partyaggro.server.AggroPlayerList;

/** client -> server: request the list of players known to this server. */
public class PacketAggroRequestPlayers extends Packet {
    public PacketAggroRequestPlayers(byte[] data) {
        super(data);
    }

    public PacketAggroRequestPlayers() {
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
        try {
            client.sendPacket(new PacketAggroPlayers(AggroPlayerList.build(server)));
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to build player list: " + t);
        }
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
    }
}
