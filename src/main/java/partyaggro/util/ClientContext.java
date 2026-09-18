package partyaggro.util;

import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;

/** Helper to get the client and current server identity from anywhere. */
public final class ClientContext {
    private ClientContext() {
    }

    public static Client client() {
        Object state = GlobalData.getCurrentState();
        if (state instanceof MainGame) {
            return ((MainGame) state).getClient();
        }
        return null;
    }

    public static long currentWorldId() {
        Client client = client();
        if (client == null) {
            return 0L;
        }
        try {
            return client.getWorldUniqueID();
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** The local player's authentication, or -1 if not available. */
    public static long selfAuth() {
        try {
            Client client = client();
            if (client == null || client.getPlayer() == null || client.getPlayer().getNetworkClient() == null) {
                return -1L;
            }
            return client.getPlayer().getNetworkClient().authentication;
        } catch (Throwable t) {
            return -1L;
        }
    }
}
