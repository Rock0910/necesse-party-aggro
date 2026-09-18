package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanAngerTargetAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import net.bytebuddy.asm.Advice;
import partyaggro.data.AggroServerSection;
import partyaggro.server.AggroServerState;

/**
 * White-list wins even against the vanilla anger system: a whitelisted player
 * never makes party villagers angry, no matter who they hit.
 */
@ModMethodPatch(target = HumanAngerTargetAINode.class, name = "addAnger", arguments = {
        float.class, Mob.class, boolean.class
})
public class HumanAngerPatch {
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    static boolean onEnter(@Advice.This HumanAngerTargetAINode node, @Advice.Argument(1) Mob attackOwner) {
        try {
            if (attackOwner == null || !attackOwner.isPlayer) {
                return false;
            }
            HumanMob mob = (HumanMob) node.mob();
            if (mob == null) {
                return false;
            }
            ServerClient owner = mob.adventureParty.getServerClient();
            if (owner == null) {
                return false;
            }
            if (partyaggro.server.AggroServerState.isRuntimeDisabled(owner.authentication)) {
                partyaggro.util.Debug.log("disabled: suppressed anger from "
                        + (((PlayerMob) attackOwner).getServerClient() == null ? "?" : ((PlayerMob) attackOwner).getServerClient().getName()));
                return true;
            }
            AggroServerSection section = AggroServerState.get(owner.authentication);
            if (section == null) {
                return false;
            }
            if (!section.enabled) {
                return true; // system disabled
            }
            ServerClient attackerClient = ((PlayerMob) attackOwner).getServerClient();
            if (attackerClient != null && section.isWhitelist(attackerClient.authentication)) {
                partyaggro.util.Debug.log("whitelist suppressed anger from " + attackerClient.getName());
                return true; // skip anger
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
