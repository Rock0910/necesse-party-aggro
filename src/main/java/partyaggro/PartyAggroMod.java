package partyaggro;

import java.awt.Color;

import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.events.ServerClientDisconnectEvent;
import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.network.client.Client;
import necesse.engine.registries.PacketRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import partyaggro.data.AggroConfig;
import partyaggro.data.AggroServerSection;
import partyaggro.net.PacketAggroHatredAdded;
import partyaggro.net.PacketAggroPlayers;
import partyaggro.net.PacketAggroRequestPlayers;
import partyaggro.net.PacketAggroSettings;
import partyaggro.server.AggroLoginTimes;
import partyaggro.server.AggroServerState;
import partyaggro.ui.PartyAggroUi;
import partyaggro.util.ClientContext;

@ModEntry
public class PartyAggroMod {
    public static final String MOD_ID = "rockdices.partyaggro";
    public static final AggroConfig CONFIG = new AggroConfig();

    /** Rebindable in Settings > Controls (mod section). Toggles the saved "enabled" flag. */
    public static Control toggleControl;
    /** Rebindable in Settings > Controls (mod section). Opens the hatred management window. */
    public static Control openManageControl;

    private static volatile boolean dirty = true;
    private static long lastSentWorldId = Long.MIN_VALUE;
    private static volatile boolean configLoaded = false;
    private static boolean bindingsReconciled = false;
    private static int lastToggleKey = Integer.MIN_VALUE;
    private static int lastOpenKey = Integer.MIN_VALUE;

    public void init() {
        PacketRegistry.registerPacket(PacketAggroSettings.class);
        PacketRegistry.registerPacket(PacketAggroHatredAdded.class);
        PacketRegistry.registerPacket(PacketAggroRequestPlayers.class);
        PacketRegistry.registerPacket(PacketAggroPlayers.class);
        GameEvents.addListener(ServerClientConnectedEvent.class, new GameEventListener<ServerClientConnectedEvent>() {
            @Override
            public void onEvent(ServerClientConnectedEvent event) {
                if (event != null && event.client != null) {
                    AggroLoginTimes.onConnect(event.client.authentication);
                }
            }
        });
        GameEvents.addListener(ServerClientDisconnectEvent.class, new GameEventListener<ServerClientDisconnectEvent>() {
            @Override
            public void onEvent(ServerClientDisconnectEvent event) {
                if (event != null && event.client != null) {
                    AggroLoginTimes.onDisconnect(event.client.authentication);
                    AggroServerState.remove(event.client.authentication);
                }
            }
        });
        System.out.println("[PartyAggro] init");
        partyaggro.util.Debug.log("init dev=" + GlobalData.isDevMode());
    }

    public void postInit() {
        try {
            if (!GlobalData.isServer()) {
                toggleControl = Control.addModControl(new Control(78, "partyaggro_toggle",
                        new LocalMessage("partyaggro", "control_toggle")));
                toggleControl.tooltip = new LocalMessage("partyaggro", "control_toggle_tip");
                openManageControl = Control.addModControl(new Control(75, "partyaggro_openmanage",
                        new LocalMessage("partyaggro", "control_openmanage")));
                openManageControl.tooltip = new LocalMessage("partyaggro", "control_openmanage_tip");
            }
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to register control: " + t);
        }
        System.out.println("[PartyAggro] postInit");
    }

    /**
     * Persisted to %APPDATA%/Necesse/cfg/mods/user.partyaggro.cfg by the mod loader.
     * Holds one section per server.
     */
    public ModSettings initSettings() {
        return new ModSettings() {
            @Override
            public void addSaveData(SaveData save) {
                save.addStringList("config", PartyAggroMod.CONFIG.serialize());
            }

            @Override
            public void applyLoadData(LoadData data) {
                PartyAggroMod.CONFIG.deserialize(data.getStringList("config"));
                partyaggro.util.Debug.setEnabled(PartyAggroMod.CONFIG.debug);
                PartyAggroMod.configLoaded = true;
            }
        };
    }

    /** Marks settings as changed so the next client tick uploads them to the server. */
    public static void markDirty() {
        dirty = true;
    }

    /**
     * Persists the mod's control bindings ourselves (Necesse's mod-control save proved
     * unreliable here). On the first tick after the config is loaded we apply the saved keys,
     * then we watch for rebinds and store them.
     */
    private static void reconcileBindings() {
        if (!configLoaded || toggleControl == null || openManageControl == null) {
            return;
        }
        if (!bindingsReconciled) {
            bindingsReconciled = true;
            try {
                if (CONFIG.toggleKey > 0) {
                    toggleControl.changeKey(CONFIG.toggleKey);
                }
                if (CONFIG.openManageKey > 0) {
                    openManageControl.changeKey(CONFIG.openManageKey);
                }
            } catch (Throwable ignored) {
            }
            lastToggleKey = toggleControl.getKey();
            lastOpenKey = openManageControl.getKey();
            CONFIG.toggleKey = lastToggleKey;
            CONFIG.openManageKey = lastOpenKey;
            return;
        }
        int t = toggleControl.getKey();
        if (t != lastToggleKey) {
            lastToggleKey = t;
            CONFIG.toggleKey = t;
            save();
            partyaggro.util.Debug.log("binding toggle -> " + t);
        }
        int o = openManageControl.getKey();
        if (o != lastOpenKey) {
            lastOpenKey = o;
            CONFIG.openManageKey = o;
            save();
            partyaggro.util.Debug.log("binding openmanage -> " + o);
        }
    }

    /** Called every client frame from the Input.tick patch. */
    public static void clientTick() {
        PartyAggroUi.tickManage();
        reconcileBindings();

        if (toggleControl != null && toggleControl.isPressed()) {
            Client hotkeyClient = ClientContext.client();
            if (hotkeyClient != null && hotkeyClient.getWorldUniqueID() != 0L) {
                AggroServerSection section = CONFIG.get(hotkeyClient.getWorldUniqueID());
                section.enabled = !section.enabled;
                save();
                markDirty();
                PartyAggroUi.syncEnabled(section.enabled);
                notifyToggle(section.enabled);
                partyaggro.util.Debug.log("hotkey toggled enabled=" + section.enabled);
            }
        }

        if (openManageControl != null && openManageControl.isPressed() && ClientContext.client() != null) {
            PartyAggroUi.toggleManage();
        }

        Client client = ClientContext.client();
        if (client == null || client.network == null) {
            return;
        }
        long worldId = client.getWorldUniqueID();
        if (worldId == 0L) {
            lastSentWorldId = Long.MIN_VALUE;
            return;
        }
        if (dirty || worldId != lastSentWorldId) {
            AggroServerSection section = CONFIG.get(worldId);
            client.network.sendPacket(new PacketAggroSettings(worldId, section, false));
            lastSentWorldId = worldId;
            dirty = false;
            partyaggro.util.Debug.log("send settings world=" + worldId + " enabled=" + section.enabled);
        }
    }

    /** Shows a local-only chat message (visible only to this client). */
    public static void notifyToggle(boolean enabled) {
        try {
            Client client = ClientContext.client();
            if (client != null) {
                client.chat.addMessage(L.t(enabled ? "status_enabled" : "status_disabled"));
            }
        } catch (Throwable t) {
            partyaggro.util.Debug.log("notifyToggle failed: " + t);
        }
    }

    public static void requestPlayers() {
        try {
            Client client = ClientContext.client();
            if (client != null && client.network != null) {
                client.network.sendPacket(new PacketAggroRequestPlayers());
            }
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to request players: " + t);
        }
    }

    /** Persists the config immediately. Client only. */
    public static void save() {
        try {
            Settings.saveClientSettings();
        } catch (Throwable t) {
            System.out.println("[PartyAggro] failed to save settings: " + t);
        }
    }
}
