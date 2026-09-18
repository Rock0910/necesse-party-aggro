package partyaggro.patches;

import necesse.engine.modLoader.annotations.ModConstructorPatch;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.presets.containerComponent.PartyConfigForm;
import necesse.inventory.container.Container;
import net.bytebuddy.asm.Advice;
import partyaggro.ui.PartyAggroUi;

@ModConstructorPatch(target = PartyConfigForm.class, arguments = {
        Client.class, Container.class, int.class, int.class, int.class, Runnable.class, Runnable.class, Runnable.class
})
public class PartyConfigFormConstructorPatch {
    @Advice.OnMethodExit
    static void onExit(@Advice.This PartyConfigForm form) {
        try {
            PartyAggroUi.attach(form);
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to attach UI: " + t);
            t.printStackTrace();
        }
    }
}
