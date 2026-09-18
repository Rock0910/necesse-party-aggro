package partyaggro.patches;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import net.bytebuddy.asm.Advice;
import partyaggro.PartyAggroMod;

/** Client tick: settings sync + global hotkey handling. */
@ModMethodPatch(target = Input.class, name = "tick", arguments = {boolean.class, TickManager.class})
public class InputHotkeyPatch {
    @Advice.OnMethodExit
    static void onExit(@Advice.This Input input) {
        try {
            PartyAggroMod.clientTick();
        } catch (Throwable t) {
            System.out.println("[PartyAggro] error in client tick: " + t);
        }
    }
}
