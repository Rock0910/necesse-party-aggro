# Implementation Plan: 冒險隊伍仇恨系統 (Party Aggro)

## Overview

為 Necesse 1.3.3 開發一顆 server+client mod（`clientside = false`），在冒險隊伍設定畫面加入
仇恨系統控制，並以伺服器權威方式讓隊伍村民在 PvP 下協助作戰。實作採用 ByteBuddy
`@ModMethodPatch` 掛鉤核心方法，搭配自訂 Packet 同步設定；不使用 Gradle，以 `build.ps1`
（javac + jar）編譯打包。

## Architecture Decisions

- **掛鉤方式**：`@ModMethodPatch` + `@Advice`，與參考專案相同；不修改核心 jar。
- **伺服器權威**：所有戰鬥邏輯在 `isServer()` 執行；沿用原生 server-only 的 anger 行為
  （原生 `HumanAngerTargetAINode` 本身也是 server-only），因此同步行為與原版一致。
- **儲存**：設定與名單只寫本機 `ModSettings` config（client），**以伺服器 world unique ID
  分區保存**（快捷鍵為全域）；server 於連線期間以記憶體保存，client 每次進入世界重新上傳。
- **名單互斥**：`hatred` 與 `whitelist` 為兩集合，任何加入操作會先從另一集合移除。
- **時間顯示**：`firstSeen` 取玩家存檔建立時間、`lastOnline` 取最後修改時間
  （`world.fileSystem.getPlayerFile(auth).toFile()`）；取不到時顯示 `N/A`。
- **建置**：`build.ps1` 手動產生 `mod.info` 並以 `jar` 打包，避開本機 Gradle 9.6.1 與範例
  專案 7.3 的相容問題。
- **純邏輯可測**：把集合運算（互斥、effectiveHatred）抽成不依賴遊戲類別的類別，用
  `tests/LogicTest.java` 以純 JDK 執行驗證。

## Task List

### Phase 0：可載入骨架與技術驗證（fail fast）

- [ ] Task 1：建置腳本 + 最小 mod 骨架
  - Acceptance：`build.ps1` 產出 jar；jar 內含 `mod.info`、class；遊戲 mods 清單可看到並啟用。
  - Verify：執行 `build.ps1`；複製 jar 到 `%APPDATA%\Necesse\mods\`；啟動遊戲確認 mod 載入無錯。
  - Files: `build.ps1`, `src/main/java/partyaggro/PartyAggroMod.java`, `src/main/resources/preview.png`
  - Scope: S

- [ ] Task 2：最小 patch 驗證（`Mob.isHit` + `Input.tick` + 建構子 patch）
  - Acceptance：日誌可在玩家受擊與按鍵時輸出；確認 patch 不崩潰、伺服器/客戶端都能載入。
  - Verify：`build.ps1`；進遊戲打怪/被打、按鍵，檢查 `latest-log.txt`。
  - Files: `patches/MobIsHitPatch.java`, `patches/InputHotkeyPatch.java`
  - Scope: S

### Checkpoint 0
- [ ] jar 可載入；patch 生效且無例外；確認 anger blackboard key 可取得。

### Phase 1：設定資料與同步

- [ ] Task 3：純邏輯資料模型與單元測試
  - Acceptance：`AggroConfig` 具全域 `hotkey`，以及 per-server 區段（`enabled`、三個自動旗標、
    `hatred`、`whitelist`、`hatredSource`、`firstSeen`）；互斥與 `effectiveHatred` 正確；測試全過。
  - Verify：`javac` 編譯測試並 `java LogicTest`，輸出 `ALL TESTS PASSED`。
  - Files: `data/AggroConfig.java`, `data/AggroServerSection.java`, `data/AggroPlayerRef.java`, `tests/LogicTest.java`
  - Scope: S

- [ ] Task 4：ModSettings 持久化（per-server）
  - Acceptance：各伺服器區段序列化寫入 `%APPDATA%/cfg/...`；切換不同世界讀到各自的設定；
    重啟遊戲後保留。
  - Verify：進世界 A 改設定 → 進世界 B 確認為預設 → 回 A 確認保留 → 重啟再確認。
  - Files: `PartyAggroMod.java`, `data/AggroConfig.java`
  - Scope: M

- [ ] Task 5：設定同步封包（client→server）
  - Acceptance：進入世界（`getWorldUniqueID() != 0`）後自動上傳該伺服器區段；
    `PacketAggroSettings` 註冊成功；server 記憶體持有正確設定。
  - Verify：兩 client 連線同一世界，於 server 端日誌輸出收到的設定；換世界不串資料。
  - Files: `net/PacketAggroSettings.java`, `server/AggroServerState.java`, `PartyAggroMod.java`
  - Scope: M

### Checkpoint 1
- [ ] 設定可持久化並同步到 server；純邏輯測試通過。

### Phase 2：核心戰鬥行為（伺服器）

- [ ] Task 6：觸發邏輯（三情境）
  - Acceptance：情境 A/B/C 依對應勾選框把玩家加入仇恨、通知 client、發聊天訊息。
  - Verify：兩 client 開 PvP 對打，觀察仇恨清單與聊天訊息。
  - Files: `patches/MobIsHitPatch.java`, `server/AggroLogic.java`
  - Scope: M

- [ ] Task 7：村民反擊（anger）+ 白名單抑制 + 衍生敵人
  - Acceptance：被仇恨者的村民會反擊且仍跟隨；白名單玩家不被反擊；仇恨對象的隊員成為衍生敵人。
  - Verify：三 client（含一個帶村民的仇恨對象）測試。
  - Files: `patches/HumanAngerPatch.java`, `server/AggroLogic.java`
  - Scope: M

- [ ] Task 8：仇恨持久化通知（server→client）
  - Acceptance：自動加入的仇恨即時寫入 client config。
  - Verify：觸發後重啟遊戲，仇恨仍在。
  - Files: `net/PacketAggroHatredAdded.java`, `server/AggroLogic.java`, `PartyAggroMod.java`
  - Scope: S

### Checkpoint 2
- [ ] 三情境與反擊在 PvP 下完整運作；白名單有效；仇恨可持久化。

### Phase 3：UI

- [ ] Task 9：隊伍設定畫面控制項
  - Acceptance：出現主開關、三個自動記仇勾選框、快捷鍵重綁列、管理按鈕；排版不重疊。
  - Verify：遊戲內截圖/目視；切換後設定即時生效並保存。
  - Files: `patches/PartyConfigFormPatch.java`, `PartyAggroMod.java`
  - Scope: M

- [ ] Task 10：管理視窗（單頁籤）
  - Acceptance：顯示登入紀錄玩家、登入/最後上線時間、來源標記；名稱即時篩選；
    仇恨/白名單互斥切換；清除全部仇恨。
  - Verify：兩 client 登入後開啟視窗逐一操作。
  - Files: `ui/AggroSettingsForm.java`, `net/PacketAggroRequestPlayers.java`,
    `net/PacketAggroPlayers.java`
  - Scope: L

### Checkpoint 3
- [ ] UI 全部功能可用且與 config/伺服器狀態一致。

### Phase 4：快捷鍵、語系與收尾

- [ ] Task 11：快捷鍵開關
  - Acceptance：`Ctrl+H` 切換；停用時村民停手且不新增仇恨；可於 UI 重綁。
  - Verify：遊戲內測試切換與重綁。
  - Files: `patches/InputHotkeyPatch.java`
  - Scope: S

- [ ] Task 12：語系與圖示
  - Acceptance：`en`、`zh-TW`、`zh-CN`、`ja` 四語系齊全；`preview.png` 存在。
  - Verify：切換語言檢視文字。
  - Files: `src/main/resources/locale/*.lang`, `preview.png`
  - Scope: S

- [ ] Task 13：邊界與防呆
  - Acceptance：無 adventureParty、mob removed、封包版本不符、config 損毀皆不崩潰。
  - Verify：上述情境手動測試與日誌檢視。
  - Files: 全部 patch 與資料類別
  - Scope: M

### Checkpoint: Complete
- [ ] 所有 Success Criteria 達成；通過 code review。

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `@ModMethodPatch` 無法掛建構子/`Input.tick` | High | Task 2 先驗證；失敗則改用 `MainGameFormManager` 或事件監聽 |
| 封包 id 與其他 mod 衝突 | Med | 使用 `PacketRegistry.registerPacket(Class)` 自動配 id；確認 mod 載入順序 |
| 村民 anger 客戶端不同步 | Med | 沿用原生 server-only 機制；Task 7 實測動畫/移動 |
| `PartyConfigForm` 插入元件排版破損 | Med | Task 9 調整 `updateHeight`；必要時改為浮動獨立按鈕 |
| 最後上線時間取不到 | Low | 顯示 `N/A`；`firstSeen` 由 mod 記錄 |
| PvP 未開啟時功能無效 | Med | 於 UI/文件說明；不繞過核心判定 |
| dedicated server 缺 client 資源 | Med | 資源僅在 `initResources()`；Task 12 於 runServer 驗證 |

## Open Questions

- Form 元件（`FormCheckBox`、`FormTextInput`、`FormContentBox`）的實際建構子簽名需在 Task 9 前查證。
- 快捷鍵修飾鍵（Ctrl）偵測用 GLFW key code 或 `Control` 類別，Task 11 定案。
