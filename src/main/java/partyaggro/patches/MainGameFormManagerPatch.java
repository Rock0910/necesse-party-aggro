package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.forms.MainGameFormManager;
import net.bytebuddy.asm.Advice;
import partyaggro.ui.PartyAggroUi;

@ModMethodPatch(target = MainGameFormManager.class, name = "setup", arguments = {})
public class MainGameFormManagerPatch {
    @Advice.OnMethodExit
    static void onExit(@Advice.This MainGameFormManager manager) {
        try {
            PartyAggroUi.setFormManager(manager);
        } catch (Throwable t) {
            partyaggro.util.Debug.log("MainGameFormManager patch failed: " + t);
        }
    }
}
