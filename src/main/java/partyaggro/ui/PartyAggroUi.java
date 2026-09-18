package partyaggro.ui;

import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.state.MainGame;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.presets.PauseMenuForm;
import necesse.gfx.forms.presets.containerComponent.PartyConfigForm;
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
    private static Form partyForm;
    private static boolean manageOpen = false;
    private static long lastManageLog = 0L;

    // Remembered window positions (persisted to partyaggropos.cfg).
    private static Integer savedManageX;
    private static Integer savedManageY;
    private static Integer savedSettingsX;
    private static Integer savedSettingsY;
    private static boolean posLoaded = false;

    private PartyAggroUi() {
    }

    public static void setFormManager(MainGameFormManager manager) {
        formManager = manager;
        // New game/manager: drop references to the previous session's forms so they can be GC'd.
        settingsForm = null;
        manageForm = null;
        partyForm = null;
        gearButton = null;
        manageOpen = false;
        partyaggro.util.Debug.log("formManager set (" + (manager != null) + ")");
    }

    public static void attach(PartyConfigForm form) {
        if (form == null) {
            return;
        }
        partyForm = form;
        // Only a gear button in the top-right corner; nothing added below the vanilla content.
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
            restoreSettingsPosition(settingsForm);
            try {
                settingsForm.tryPutOnTop();
            } catch (Throwable ignored) {
            }
            try {
                formManager.setNextControllerFocus(settingsForm.getInitialFocus());
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
        savePos();
    }

    /** Called when the vanilla adventure party window closes. Closes settings only; hatred list stays open. */
    public static void onPartyConfigClosed() {
        partyaggro.util.Debug.log("onPartyConfigClosed open=" + manageOpen + " form=" + (manageForm != null));
        closeSettings();
        if (manageOpen && manageForm != null) {
            manageForm.setHidden(false);
            try {
                manageForm.tryPutOnTop();
            } catch (Throwable ignored) {
            }
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
            manageOpen = true;
            restoreManagePosition(manageForm);
            try {
                manageForm.tryPutOnTop();
            } catch (Throwable ignored) {
            }
            try {
                formManager.setNextControllerFocus(manageForm.getInitialFocus());
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
        partyaggro.util.Debug.log("closeManage called");
        manageOpen = false;
        if (manageForm != null) {
            manageForm.setHidden(true);
        }
        savePos();
    }

    /** Hotkey behaviour: toggle the hatred list open/closed. */
    public static void toggleManage() {
        if (manageOpen) {
            closeManage();
        } else {
            openManage();
        }
    }

    public static void tickManage() {
        // Refresh the settings window's key hints, and keep both windows on screen.
        if (settingsForm != null && !settingsForm.isHidden()) {
            settingsForm.tick();
            clampToScreen(settingsForm, false);
        }
        if (!manageOpen || formManager == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastManageLog > 1000L) {
            lastManageLog = now;
            partyaggro.util.Debug.log("manage state form=" + (manageForm != null)
                    + " inMgr=" + (manageForm != null && isInManager(manageForm))
                    + " hidden=" + (manageForm != null && manageForm.isHidden())
                    + " mgrComponents=" + countComponents());
        }
        if (manageForm == null || !isInManager(manageForm)) {
            manageForm = new AggroManageForm();
            formManager.addComponent(manageForm);
            restoreManagePosition(manageForm);
            partyaggro.util.Debug.log("manage (re)created");
        }
        if (manageForm.isHidden()) {
            manageForm.setHidden(false);
        }
        clampToScreen(manageForm, true);
        try {
            manageForm.tick();
        } catch (Throwable t) {
            partyaggro.util.Debug.log("manage tick failed, will recreate: " + t);
            manageForm = null;
        }
    }

    // ---- Window positions: remember, clamp on screen ----

    private static int hudWidth() {
        try {
            return WindowManager.getWindow().getHudWidth();
        } catch (Throwable t) {
            return 1280;
        }
    }

    private static int hudHeight() {
        try {
            return WindowManager.getWindow().getHudHeight();
        } catch (Throwable t) {
            return 720;
        }
    }

    private static int clamp(int value, int lo, int hi) {
        return value < lo ? lo : (value > hi ? hi : value);
    }

    private static String posFile() {
        try {
            return GlobalData.cfgPath() + "partyaggropos.cfg";
        } catch (Throwable t) {
            return null;
        }
    }

    private static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(Integer.parseInt(value));
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void loadPos() {
        if (posLoaded) {
            return;
        }
        posLoaded = true;
        String file = posFile();
        if (file == null) {
            return;
        }
        try {
            java.util.Properties p = new java.util.Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                p.load(in);
            }
            savedManageX = parseInt(p.getProperty("mx"));
            savedManageY = parseInt(p.getProperty("my"));
            savedSettingsX = parseInt(p.getProperty("sx"));
            savedSettingsY = parseInt(p.getProperty("sy"));
        } catch (Throwable ignored) {
        }
    }

    private static synchronized void savePos() {
        String file = posFile();
        if (file == null) {
            return;
        }
        try {
            java.util.Properties p = new java.util.Properties();
            if (savedManageX != null && savedManageY != null) {
                p.setProperty("mx", String.valueOf(savedManageX));
                p.setProperty("my", String.valueOf(savedManageY));
            }
            if (savedSettingsX != null && savedSettingsY != null) {
                p.setProperty("sx", String.valueOf(savedSettingsX));
                p.setProperty("sy", String.valueOf(savedSettingsY));
            }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                p.store(out, "Party Aggro window pos");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void restoreManagePosition(Form form) {
        loadPos();
        int w = hudWidth();
        int h = hudHeight();
        int x;
        int y;
        if (savedManageX != null && savedManageY != null) {
            x = savedManageX;
            y = savedManageY;
        } else {
            x = w / 2 - form.getWidth() / 2;
            y = 60;
        }
        x = clamp(x, 4, Math.max(4, w - form.getWidth() - 4));
        y = clamp(y, 4, Math.max(4, h - form.getHeight() - 4));
        form.setPosition(x, y);
        savedManageX = x;
        savedManageY = y;
    }

    private static void restoreSettingsPosition(Form form) {
        loadPos();
        int w = hudWidth();
        int h = hudHeight();
        int x;
        int y;
        if (savedSettingsX != null && savedSettingsY != null) {
            x = savedSettingsX;
            y = savedSettingsY;
        } else {
            x = w / 2;
            y = 40;
            if (partyForm != null) {
                x = partyForm.getX() + partyForm.getWidth() + 10;
                y = partyForm.getY();
                if (x + form.getWidth() > w - 4) {
                    x = partyForm.getX() - form.getWidth() - 10;
                }
            }
        }
        x = clamp(x, 4, Math.max(4, w - form.getWidth() - 4));
        y = clamp(y, 4, Math.max(4, h - form.getHeight() - 4));
        form.setPosition(x, y);
        savedSettingsX = x;
        savedSettingsY = y;
    }

    private static void clampToScreen(Form form, boolean manage) {
        if (form == null) {
            return;
        }
        int w = hudWidth();
        int h = hudHeight();
        int nx = clamp(form.getX(), 4, Math.max(4, w - form.getWidth() - 4));
        int ny = clamp(form.getY(), 4, Math.max(4, h - form.getHeight() - 4));
        if (nx != form.getX() || ny != form.getY()) {
            form.setPosition(nx, ny);
        }
        if (manage) {
            savedManageX = nx;
            savedManageY = ny;
        } else {
            savedSettingsX = nx;
            savedSettingsY = ny;
        }
    }

    private static int countComponents() {
        try {
            int n = 0;
            for (Object ignored : formManager.getComponents()) {
                n++;
            }
            return n;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static boolean isInManager(Form form) {
        try {
            for (Object component : formManager.getComponents()) {
                if (component == form) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
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
