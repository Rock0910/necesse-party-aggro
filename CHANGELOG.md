# Changelog

## 1.0.4
- Fixed hotkey bindings resetting after restarting the game: the mod now persists its control bindings in its own config (`rockdices.partyaggro.cfg`) and re-applies them on launch, instead of relying on the game's mod-control save.

## 1.0.3
- Added a rebindable hotkey to open the hatred management window (default `B`), in Settings > Controls. Pressing it again closes the window (toggle).
- The settings window's "Manage" button label follows the currently bound key.
- Window positions are remembered (saved to `%APPDATA%/Necesse/cfg/partyaggropos.cfg`) and clamped on screen so they never go off-screen.

## 1.0.2
- Removed the stale hardcoded version in the init log line.
- No gameplay changes.

## 1.0.1
- Controller (gamepad) support: explicit focus order and initial focus for the settings and management windows.
- Debug log is now a checkbox and **off by default**; when off there is no log I/O and no hot-path string building.
- Closing the adventure party window now closes the mod settings window but keeps the hatred list open.
- Hatred management window: added a title, pagination (8 per page, wraps around), state filter (All / Hatred / Whitelist / Unset), sorting by last online (most recent first), shorter name column and wider last-online column.
- Defaults changed: the three auto-hate triggers now default **off**; only "Enable aggro system" defaults on.
- Whitelisting / unsetting a player now prunes villagers that were already engaged (they stop immediately).
- Proactive engagement: while enabled, villagers treat everyone on the hatred list (and their party members, as derived enemies) as enemies.
- New scenario D: if your villager attacks a player, that player adds your party to their own hatred list.
- Login / last-online times are tracked server-side per session so they no longer show as N/A for online players.

## 1.0.0
- Initial release.
- Gear button in the adventure party window opens the mod settings.
- Enable switch + three independent auto-hate triggers.
- Hatred / whitelist management with search, state filter, sorting by last online, pagination.
- Hotkey (default `N`, rebindable) to toggle the system, with local chat feedback and checkbox sync.
- Whitelist suppresses both the mod and the vanilla anger system.
- Per-server persistence in the local config file.
- Localization: en, zh-TW, zh-CN, ja.

