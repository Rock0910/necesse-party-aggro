package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.forms.presets.containerComponent.PartyConfigContainerForm;
import net.bytebuddy.asm.Advice;
import partyaggro.ui.PartyAggroUi;

/** When the adventure party window closes, close the mod settings window (but keep the hatred list). */
@ModMethodPatch(target = PartyConfigContainerForm.class, name = "dispose", arguments = {})
public class PartyConfigContainerFormDisposePatch {
    @Advice.OnMethodExit
    static void onExit() {
        try {
            PartyAggroUi.onPartyConfigClosed();
        } catch (Throwable t) {
            partyaggro.util.Debug.log("onPartyConfigClosed failed: " + t);
        }
    }
}
