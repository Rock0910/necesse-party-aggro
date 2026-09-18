package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import net.bytebuddy.asm.Advice;
import partyaggro.server.AggroLogic;
import partyaggro.util.Debug;

@ModMethodPatch(target = Mob.class, name = "isHit", arguments = {MobWasHitEvent.class, Attacker.class})
public class MobIsHitPatch {
    @Advice.OnMethodExit
    static void onExit(@Advice.This Mob mob, @Advice.Argument(0) MobWasHitEvent event, @Advice.Argument(1) Attacker attacker) {
        if (mob == null || attacker == null || event == null) {
            return;
        }
        if (!mob.isServer()) {
            return;
        }
        try {
            Mob owner = attacker.getAttackOwner();
            if (Debug.isEnabled() && (mob instanceof PlayerMob || owner instanceof PlayerMob)) {
                Debug.log("isHit victim=" + describe(mob) + " attacker=" + describe(owner)
                        + " prevented=" + event.wasPrevented + " dmg=" + event.damage);
            }
            if (event.wasPrevented) {
                return;
            }
            AggroLogic.onHit(mob, owner);
        } catch (Throwable t) {
            Debug.log("error in isHit patch: " + t);
        }
    }

    public static String describe(Mob mob) {
        if (mob == null) {
            return "null";
        }
        String name = "";
        try {
            if (mob instanceof PlayerMob) {
                ServerClient client = ((PlayerMob) mob).getServerClient();
                if (client != null) {
                    name = "(" + client.getName() + ")";
                }
            }
        } catch (Throwable ignored) {
        }
        return mob.getClass().getSimpleName() + name + "#" + mob.getUniqueID();
    }
}
