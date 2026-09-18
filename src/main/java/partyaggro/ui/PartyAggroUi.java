package partyaggro.ui;

import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.state.MainGame;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.presets.PauseMenuForm;
import necesse.gfx.forms.presets.containerComponent.PartyConfigForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import partyaggro.L;
import partyaggro.PartyAggroMod;

/**
 * Adds a gear button to the vanilla adventure party form which opens the Party Aggro
 * settings window, and owns the standalone management window.
 */
public final class PartyAggroUi {
    private static FormContentIconButton gearButton;

    private static MainGameFormManager formManager;
    private static AggroSettingsForm settingsForm;
    private static AggroManageForm manageForm;
    private static necesse.gfx.forms.Form partyForm;

    private PartyAggroUi() {
    }

    public static void setFormManager(MainGameFormManager manager) {
        formManager = manager;
        partyaggro.util.Debug.log("formManager set (" + (manager != null) + ")");
    }

    public static void attach(PartyConfigForm form) {
        if (form == null) {
            return;
        }
        partyForm = form;
        // Only a gear button in the top-right corner; nothing added below the vanilla content,
        // so the form never grows over the hotbar/inventory.
        gearButton = new FormContentIconButton(form.getWidth() - 40, 4, FormInputSize.SIZE_32, ButtonColor.BASE,
                Settings.UI.config_button_32, L.m("settings_tip"));
        gearButton.onClicked(e -> {
            partyaggro.util.Debug.log("gear clicked (formManager=" + (formManager != null) + ")");
            openSettings();
        });
        form.addComponent(gearButton);
    }

    /** Keeps the gear pinned to the top-right corner. */
    public static void layout(PartyConfigForm form) {
        if (form == null || gearButton == null) {
            return;
        }
        gearButton.setPosition(form.getWidth() - 40, 4);
    }

    public static void syncEnabled(boolean enabled) {
        if (settingsForm != null) {
            settingsForm.syncEnabled(enabled);
        }
    }

    public static void openSettings() {
        if (formManager == null) {
            partyaggro.util.Debug.log("openSettings: formManager not available yet");
            return;
        }
        try {
            if (settingsForm == null) {
                settingsForm = new AggroSettingsForm();
                formManager.addComponent(settingsForm);
            }
            settingsForm.setHidden(false);
            positionBesideParty(settingsForm);
            try {
                settingsForm.tryPutOnTop();
            } catch (Throwable ignored) {
            }
            partyaggro.util.Debug.log("openSettings: opened");
        } catch (Throwable t) {
            partyaggro.util.Debug.log("openSettings failed: " + t);
            t.printStackTrace();
        }
    }

    public static void closeSettings() {
        if (settingsForm != null) {
            settingsForm.setHidden(true);
        }
    }

    public static void openManage() {
        if (formManager == null) {
            partyaggro.util.Debug.log("openManage: formManager not available yet");
            return;
        }
        try {
            if (manageForm == null) {
                manageForm = new AggroManageForm();
                formManager.addComponent(manageForm);
            }
            manageForm.setHidden(false);
            positionNearTop(manageForm, 60);
            try {
                manageForm.tryPutOnTop();
            } catch (Throwable ignored) {
            }
            PartyAggroMod.requestPlayers();
            partyaggro.util.Debug.log("openManage: opened");
        } catch (Throwable t) {
            partyaggro.util.Debug.log("openManage failed: " + t);
            t.printStackTrace();
        }
    }

    public static void closeManage() {
        if (manageForm != null) {
            manageForm.setHidden(true);
        }
    }

    public static void tickManage() {
        if (manageForm != null && !manageForm.isHidden()) {
            manageForm.tick();
        }
    }

    private static void positionBesideParty(necesse.gfx.forms.Form form) {
        try {
            int hudWidth = WindowManager.getWindow().getHudWidth();
            int hudHeight = WindowManager.getWindow().getHudHeight();
            int x = hudWidth / 2;
            int y = 40;
            if (partyForm != null) {
                x = partyForm.getX() + partyForm.getWidth() + 10;
                y = partyForm.getY();
                if (x + form.getWidth() > hudWidth - 4) {
                    x = partyForm.getX() - form.getWidth() - 10;
                }
            }
            x = Math.max(4, Math.min(x, Math.max(4, hudWidth - form.getWidth() - 4)));
            y = Math.max(4, Math.min(y, Math.max(4, hudHeight - form.getHeight() - 4)));
            form.setPosition(x, y);
        } catch (Throwable ignored) {
        }
    }

    private static void positionNearTop(necesse.gfx.forms.Form form, int top) {
        try {
            int hudWidth = WindowManager.getWindow().getHudWidth();
            int hudHeight = WindowManager.getWindow().getHudHeight();
            int x = hudWidth / 2 - form.getWidth() / 2;
            int y = top;
            x = Math.max(4, Math.min(x, Math.max(4, hudWidth - form.getWidth() - 4)));
            y = Math.max(4, Math.min(y, Math.max(4, hudHeight - form.getHeight() - 4)));
            form.setPosition(x, y);
        } catch (Throwable ignored) {
        }
    }

    /** Opens the game's Settings > Controls page so the player can rebind the hotkey. */
    public static void openKeyBindings() {
        try {
            Object state = GlobalData.getCurrentState();
            if (!(state instanceof MainGame)) {
                return;
            }
            MainGameFormManager manager = ((MainGame) state).formManager;
            if (manager == null || manager.pauseMenu == null) {
                return;
            }
            PauseMenuForm pauseMenu = manager.pauseMenu;
            pauseMenu.setHidden(false);
            try {
                pauseMenu.settings.setHidden(false);
            } catch (Throwable ignored) {
            }
            try {
                pauseMenu.makeCurrent(pauseMenu.settings);
            } catch (Throwable ignored) {
            }
            boolean hasList = false;
            try {
                hasList = readField(pauseMenu.settings, "currentControlList") != null;
            } catch (Throwable ignored) {
            }
            try {
                if (hasList) {
                    pauseMenu.settings.makeControlsCurrent();
                } else {
                    pauseMenu.settings.makeControlTypeCurrent();
                }
            } catch (Throwable t) {
                System.out.println("[PartyAggro] failed to open controls page: " + t);
            }
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to open key bindings: " + t);
        }
    }

    private static Object readField(Object object, String name) {
        if (object == null) {
            return null;
        }
        Class<?> c = object.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
