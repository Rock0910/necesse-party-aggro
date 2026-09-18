# Party Aggro

讓冒險隊伍中的村民可以根據新增的仇恨系統設定攻擊玩家 有快捷鍵可開關 可設定 預設為N
(在冒險隊伍介面上新增一個齒輪按鈕 裡面就是跟這模組有關的功能 我覺得應該夠直覺)
使用Opencode開發 也一樣除非自己有遇到BUG不然不更新 不期待 沒傷害

不准營利，不准以任何方式危害別人（尤其是發佈有毒的檔案等）
源碼:https://github.com/Rock0910/necesse-party-aggro
使用機翻 - 支援：English、日文、簡中、繁中

Adventure party aggro / retaliation controls for **Necesse 1.3.x**, built for multiplayer (hosted and dedicated servers).

Party villagers (settlers/guards in your adventure party) can be made to treat chosen players as enemies, retaliate when you or your party are attacked, and proactively attack a managed hatred list.

> This is a **server + client** mod (`clientside = false`). The server and every connecting client must install it.

## Features

在冒險隊伍介面上新增一個齒輪按鈕 裡面就是跟這模組有關的功能了
- **Gear button** in the adventure party window opens the mod's settings.
- Master **enable** switch + three independent auto-hate triggers (all off by default):
  - Auto-hate when you are attacked
  - Auto-hate when a party member is attacked
  - Auto-hate when you attack a player
- **Hatred / whitelist management** window:
  - Players listed from the server's login records
  - Name search + state filter (All / Hatred / Whitelist / Unset)
  - Sorted by last online (most recent first)
  - Pagination (8 per page, wraps around)
  - Per-row set hatred / whitelist / clear, source tag (auto/manual)
  - Clear-all button
- **Hotkey** to toggle the system, rebindable in `Settings > Controls` (default `N`). Toggling shows a local-only chat message and syncs the checkbox.
- Whitelist always wins (also suppresses the vanilla anger system).
- Hatred list persists per server (local config file).
- Localization: English, Traditional Chinese, Simplified Chinese, Japanese.

## Behaviour summary

- If a player attacks you, a party member, or your villager, they are added to your hatred list (per the enabled triggers).
- If your villager attacks a player, the victim adds your party to their own hatred list (if their trigger is on).
- While enabled, a party owner's villagers proactively treat everyone on the hatred list (and their party members, as derived enemies) as enemies.
- Whitelisted players and the party owner are never attacked.
- Requires PvP to be active (world `Forced PvP` or per-player PvP); otherwise the game blocks friendly fire and nothing happens.

## Reliability note

Automatic hate recording can occasionally miss events. This is expected in some situations, for example:

- the attack was blocked by the game (PvP off, same team, creative/invincible, or protection),
- the attacker/victim was not in a valid PvP context at that moment,
- the hit happened in a way the mod does not observe (or the server does not run the mod),
- settings had not been uploaded to the server yet.

If you need reliable targeting, **add the player to the hatred list manually** in the management window. Manual entries are saved and always applied while the system is enabled, so they do not depend on event detection.

## Build

Requires a JDK (JDK 21 is fine; compiled with `--release 8`) and the game install path set in `build.ps1`.

```
powershell -ExecutionPolicy Bypass -File build.ps1
```

Output: `build/jar/PartyAggro-<gameVersion>-<modVersion>.jar`

## Test

```
powershell -ExecutionPolicy Bypass -File test.ps1
```

## Install

- Client: copy the jar into `%APPDATA%/Necesse/mods/`
- Dedicated server: copy the jar into the server's `mods/` folder and start with `-mod ./mods`

## Workshop upload

1. Build the jar.
2. Launch the game in dev mode pointing at the build folder so the mod is loaded via `-mod`:
   ```
   .\jre\bin\java.exe -jar Necesse.jar -dev -mod "<path-to>\build\jar"
   ```
3. Main menu → **Mods** → select **Party Aggro** → click **Upload** and follow the prompts.
   (Requires `preview.png` in `resources/` and the Steam Workshop terms to be accepted.)
4. The first upload is hidden; make it public from its Workshop page when ready.

## Configuration / IDs

- Mod id: `rockdices.partyaggro`
- Hotkey control id: `partyaggro_toggle` (Settings > Controls, mod section)
- Locale category: `partyaggro`
- Local config: `%APPDATA%/Necesse/cfg/mods/rockdices.partyaggro.cfg`
- Debug log (diagnostics): `%APPDATA%/Necesse/partyaggro-debug.log`
