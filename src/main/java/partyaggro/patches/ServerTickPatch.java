package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.Server;
import net.bytebuddy.asm.Advice;
import partyaggro.server.AggroLogic;
import partyaggro.util.Debug;

/** Keeps the hatred list as active enemies while the system is enabled. */
@ModMethodPatch(target = Server.class, name = "tick", arguments = {})
public class ServerTickPatch {
    @Advice.OnMethodExit
    static void onExit(@Advice.This Server server) {
        try {
            AggroLogic.serverTick(server);
        } catch (Throwable t) {
            Debug.log("ServerTickPatch error: " + t);
        }
    }
}
