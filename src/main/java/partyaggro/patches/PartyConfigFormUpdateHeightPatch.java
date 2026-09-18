package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.forms.presets.containerComponent.PartyConfigForm;
import net.bytebuddy.asm.Advice;
import partyaggro.ui.PartyAggroUi;

@ModMethodPatch(target = PartyConfigForm.class, name = "updateHeight", arguments = {})
public class PartyConfigFormUpdateHeightPatch {
    @Advice.OnMethodExit
    static void onExit(@Advice.This PartyConfigForm form) {
        try {
            PartyAggroUi.layout(form);
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to layout UI: " + t);
        }
    }
}
