package partyaggro.ui;

import java.awt.Rectangle;

import necesse.engine.Settings;
import necesse.engine.localization.message.LocalMessage;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import partyaggro.L;
import partyaggro.PartyAggroMod;
import partyaggro.data.AggroServerSection;
import partyaggro.util.ClientContext;

/** Party Aggro settings window, opened from the gear button in the adventure party form. */
public class AggroSettingsForm extends Form {
    private final long worldId;
    private final FormCheckBox boxEnabled;
    private final FormCheckBox boxA1;
    private final FormCheckBox boxA2;
    private final FormCheckBox boxA3;
    private final FormCheckBox boxDebug;

    public AggroSettingsForm() {
        super("partyaggro_settings", 340, 120);
        this.worldId = ClientContext.currentWorldId();
        final AggroServerSection section = PartyAggroMod.CONFIG.get(this.worldId);

        FormFlow flow = new FormFlow(6);
        int titleY = flow.next();
        FormLabel title = new FormLabel(L.t("settings_title"), new FontOptions(20), FormLabel.ALIGN_LEFT, 6, titleY);
        this.addComponent(title);

        FormContentIconButton close = new FormContentIconButton(getWidth() - 46, titleY, FormInputSize.SIZE_32,
                ButtonColor.BASE, Settings.UI.button_cross, L.m("close"));
        close.onClicked(e -> PartyAggroUi.closeSettings());
        this.addComponent(close);
        flow.nextY(title, 16);

        this.boxEnabled = new FormCheckBox(L.t("enable_system"), 6, flow.next(), getWidth() - 12);
        this.addComponent(this.boxEnabled);
        this.boxEnabled.checked = section.enabled;
        this.boxEnabled.onClicked(e -> {
            section.enabled = this.boxEnabled.checked;
            apply();
            PartyAggroMod.notifyToggle(section.enabled);
        });
        flow.nextY(this.boxEnabled, 5);

        this.boxA1 = new FormCheckBox(L.t("auto_player_attacked"), 6, flow.next(), getWidth() - 12);
        this.addComponent(this.boxA1);
        this.boxA1.checked = section.autoAddOnPlayerAttacked;
        this.boxA1.onClicked(e -> {
            section.autoAddOnPlayerAttacked = this.boxA1.checked;
            apply();
        });
        flow.nextY(this.boxA1, 5);

        this.boxA2 = new FormCheckBox(L.t("auto_party_member_attacked"), 6, flow.next(), getWidth() - 12);
        this.addComponent(this.boxA2);
        this.boxA2.checked = section.autoAddOnPartyMemberAttacked;
        this.boxA2.onClicked(e -> {
            section.autoAddOnPartyMemberAttacked = this.boxA2.checked;
            apply();
        });
        flow.nextY(this.boxA2, 5);

        this.boxA3 = new FormCheckBox(L.t("auto_player_attacks"), 6, flow.next(), getWidth() - 12);
        this.addComponent(this.boxA3);
        this.boxA3.checked = section.autoAddOnPlayerAttacks;
        this.boxA3.onClicked(e -> {
            section.autoAddOnPlayerAttacks = this.boxA3.checked;
            apply();
        });
        flow.nextY(this.boxA3, 8);

        this.boxDebug = new FormCheckBox(L.t("debug_log"), 6, flow.next(), getWidth() - 12);
        this.addComponent(this.boxDebug);
        this.boxDebug.checked = PartyAggroMod.CONFIG.debug;
        this.boxDebug.onClicked(e -> {
            PartyAggroMod.CONFIG.debug = this.boxDebug.checked;
            partyaggro.util.Debug.setEnabled(this.boxDebug.checked);
            PartyAggroMod.save();
        });
        flow.nextY(this.boxDebug, 8);

        FormTextButton manage = new FormTextButton(L.t("manage"), 6, flow.next(), getWidth() - 12, FormInputSize.SIZE_24, ButtonColor.BASE);
        manage.onClicked(e -> PartyAggroUi.openManage());
        this.addComponent(manage);
        flow.nextY(manage, 5);

        FormTextButton keys = new FormTextButton(L.t("keybindings"), 6, flow.next(), getWidth() - 12, FormInputSize.SIZE_24, ButtonColor.BASE);
        keys.onClicked(e -> PartyAggroUi.openKeyBindings());
        this.addComponent(keys);
        flow.nextY(keys, 5);

        this.setHeight(flow.next() + 6);
        try {
            setDraggingBox(new Rectangle(0, 0, getWidth(), 34));
        } catch (Throwable ignored) {
        }

        // Explicit controller navigation order.
        linkDown(this.boxEnabled, this.boxA1);
        linkDown(this.boxA1, this.boxA2);
        linkDown(this.boxA2, this.boxA3);
        linkDown(this.boxA3, this.boxDebug);
        linkDown(this.boxDebug, manage);
        linkDown(manage, keys);
        linkDown(keys, close);
        // Initial controller focus: the master enable checkbox.
        this.boxEnabled.controllerInitialFocusPriority = 10;
    }

    private static void linkDown(FormCheckBox upper, FormCheckBox lower) {
        upper.controllerDownFocus = lower;
        lower.controllerUpFocus = upper;
    }

    private static void linkDown(necesse.gfx.forms.components.FormComponent upper, necesse.gfx.forms.components.FormComponent lower) {
        upper.controllerDownFocus = lower;
        lower.controllerUpFocus = upper;
    }

    public necesse.gfx.forms.controller.ControllerFocusHandler getInitialFocus() {
        return this.boxEnabled;
    }

    public void syncEnabled(boolean enabled) {
        this.boxEnabled.checked = enabled;
    }

    private void apply() {
        PartyAggroMod.save();
        PartyAggroMod.markDirty();
    }
}
