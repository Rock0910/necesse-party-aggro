# Spec: 冒險隊伍仇恨系統 (Party Aggro)

- 狀態：草稿 v2，等待使用者確認
- 目標遊戲版本：Necesse 1.3.x（本機為 1.3.3）
- Mod id：`user.partyaggro`（暫定），名稱：`Party Aggro`

## 1. Objective

在 Necesse 的「冒險隊伍設定（Adventure Party）」畫面新增一套「仇恨／反擊」控制，
讓隊伍中的村民（settler / guard / 隊員）在 PvP 情境下會協助隊伍擁有者作戰。

使用者故事：

1. 當我或我的隊員被玩家攻擊時，我的村民自動把攻擊者視為敵對並反擊。
2. 當我主動攻擊其他玩家時，我的村民也把對方視為敵對（隊伍支援我的攻擊）。
3. 可用三組勾選框分別決定哪種情境要「永久記仇」。
4. 有整體開關（勾選框）與快捷鍵可隨時開／關整個仇恨系統。
5. 有白名單避免誤傷朋友，並可手動管理仇恨／白名單。
6. 清單來源是「該伺服器有登入紀錄的玩家」，顯示最後上線時間，並可依名稱篩選。
7. 敵人包含仇恨對象「本人」及其「冒險隊伍成員」（衍生的敵人，不列入清單）。

## 2. 已確認的需求（使用者回答）

- 環境：朋友開主機的多人遊戲（主機＝伺服器），**同時支援 dedicated server**；
  伺服器與所有加入者都要安裝同一顆 mod（`clientside = false`）。
- 玩家端（本機 config）：
  - 主開關勾選框：整體開啟／關閉仇恨功能。
  - 三組「自動加入仇恨清單」勾選框，各自獨立且永久記仇：
    1. **玩家受攻擊時**
    2. **冒險隊伍成員受攻擊時**
    3. **玩家主動攻擊時**
  - 白名單（永不被村民攻擊）。
  - 快捷鍵（預設 `Ctrl+H`，可於設定中修改）：開／關仇恨系統。
  - 清單儲存：本機 config 檔，**依不同伺服器（world unique ID）分別保存/讀取**，
  永久保存（自動加入的也永久保存）。
- 反擊方式：怒氣（anger）機制，村民「邊跟隨邊打」，脫離戰鬥後自動恢復；不中斷跟隨指令。
- 清單管理：同一個頁籤同時顯示白名單與仇恨清單，兩者互斥；可一鍵切換該玩家狀態。
- 顯示玩家登入時間（首次見到）／最後上線時間。
- 自動加入仇恨時，於聊天視窗提示「已將 XXX 加入敵對名單」。
- 反擊對象：仇恨對象本人 + 其冒險隊伍成員（後者為衍生敵人、不列入清單，清單以玩家為主）。
- 觸發反擊時會一併通知附近同隊村民（沿用原生 alert 行為）。
- 語系：英文、繁體中文、簡體中文、日文。

## 3. Assumptions（若以下任一有誤請告知）

1. 村民要能攻擊某玩家，該玩家必須在遊戲核心判定上「非友軍」——即世界開啟 `forcedPvP`，
   或該玩家自行開啟 PvP。這是 Necesse 核心 `HumanMob.canTarget()` 的限制，本 mod 不去繞過。
2. 「隊伍成員」指 `AdventureParty` 中的村民以及隊伍擁有者本人。
3. 本機 config 以「伺服器（world unique ID）」為單位分區保存：勾選框、仇恨清單（含自動與手動）、
   白名單、來源標記、`firstSeen` 皆為 per-server；自動加入由伺服器以封包通知該玩家 client 寫入。
   快捷鍵為全域設定（不分伺服器）。
4. 每位玩家管理自己的勾選／名單；伺服器（含 dedicated）只在使用者連線期間於記憶體保存其設定，
   client 每次連線會依該世界的 world unique ID 讀取並重新上傳本機 config。
5. 主機端（Host）玩家本身也是伺服器端 client，其設定同樣送進伺服器端套用。
6. 快捷鍵切換的是「該伺服器的 `enabled` 設定」（與勾選框同一個值），會即時同步勾選框並永久保存。
7. 「最後上線」取自玩家存檔檔案的最後修改時間、「登入時間」取其建立時間
   （`WorldFile.toFile()` 的 `lastModified()` / 檔案建立時間）；可在專用伺服器取得，
   核心本身無提供這些時間欄位（見 12.1）。
8. 伺服器識別使用 client 的 `getWorldUniqueID()`（連線後才有效）；若為 0 或不可用，
   退回以伺服器位址/名稱字串作為 key，並在日誌警告（見 12.10）。
9. 快捷鍵改用遊戲內建 `Control.addModControl(...)` 註冊（會出現在 Settings > Controls 的 mod 區可重綁），
   預設鍵 `N`；非自製組合鍵（Control 為單鍵綁定）。

## 4. Tech Stack

- Java 8 語言等級；以本機 JDK 21 的 `javac --release 8` 編譯。
- 依賴 `Necesse.jar` 與 `Necesse/lib/*`（含遊戲內建的 byte-buddy 1.12.8）。
- 以 `net.bytebuddy.asm.Advice` + `@ModMethodPatch` 對核心方法掛鉤。
- 建置：自訂 `build.ps1`（javac + jar），避免 Gradle 版本（本機快取為 9.6.1，範例專案用 7.3）。
- 無第三方依賴。

## 5. Commands

```
# 編譯 + 打包
powershell -ExecutionPolicy Bypass -File build.ps1

# 安裝成本機 mod
copy build\PartyAggro-1.3.3-0.1.0.jar %APPDATA%\Necesse\mods\

# 啟動測試（可選，需要 Steam 環境）
#   主機：一般啟動遊戲並開房
#   測試端：使用 Necesse 的 -dev 啟動參數
```

## 6. Project Structure

```
PartyAggro/
  SPEC.md                  # 本文件
  build.ps1                # javac + jar 打包腳本
  tasks/                   # 計畫與任務清單（Phase 2/3）
  src/main/java/partyaggro/
    PartyAggroMod.java     # @ModEntry：註冊封包、事件、讀寫 config
    data/AggroConfig.java  # 本機 config（開關、三個自動勾選、仇恨/白名單、快捷鍵、首次見到時間）
    data/AggroPlayerRef.java   # { authentication, name, lastOnline, firstSeen }
    net/PacketAggroSettings.java       # client -> server：上傳本機設定
    net/PacketAggroRequestPlayers.java # client -> server：請求登入紀錄名單
    net/PacketAggroPlayers.java        # server -> client：回傳名單（含時間）
    net/PacketAggroHatredAdded.java    # server -> client：通知自動加入仇恨以持久化
    server/AggroServerState.java       # 伺服器端 session：auth -> settings / 仇恨集合
    server/AggroLogic.java             # 觸發仇恨、白名單判斷、衍生敵人、呼叫 anger
    patches/MobIsHitPatch.java         # 三種觸發情境 → 更新仇恨
    patches/HumanAngerPatch.java       # 白名單優先 + 衍生敵人：調整怒氣
    patches/PartyConfigFormPatch.java  # 在隊伍設定畫面加勾選框 + 管理按鈕
    patches/InputHotkeyPatch.java      # 全域快捷鍵切換
    ui/AggroSettingsForm.java          # 單一頁籤管理視窗（仇恨/白名單互斥 + 名稱篩選 + 時間）
  src/main/resources/
    locale/en.lang
    locale/zh-TW.lang
    locale/zh-CN.lang
    locale/ja.lang
    preview.png
```

## 7. Behavior Spec（詳細行為）

### 7.1 設定項（本機 config，per client）
結構：全域設定 + 依「伺服器（world unique ID）」分區的設定。

全域（global）：
- `hotkey`：快捷鍵，預設 `Ctrl+H`，可於設定視窗修改（不分伺服器）。

per-server（key = world unique ID，另存顯示名稱）：
- `enabled` (bool)：主開關，預設 `true`。
- `autoAddOnPlayerAttacked` (bool)：玩家受攻擊時自動記仇，預設 `true`。
- `autoAddOnPartyMemberAttacked` (bool)：冒險隊伍成員受攻擊時自動記仇，預設 `true`。
- `autoAddOnPlayerAttacks` (bool)：玩家主動攻擊他人時自動記仇，預設 `true`。
- `hatred` (list of authentication)：仇恨清單，永久保存。
- `hatredSource` (map auth -> AUTO / MANUAL)：記錄每筆仇恨的加入來源，永久保存。
- `whitelist` (list of authentication)：白名單，永久保存；與 `hatred` 互斥。
- `firstSeen` (map auth -> epoch)：各玩家首次見到時間，永久保存（用於顯示登入時間）。

執行期狀態 `temporarilyDisabled`（不持久化，亦不分伺服器）。

互斥規則：一個玩家不可同時在 `hatred` 與 `whitelist`。切換到其中一邊會自動從另一邊移除。

### 7.2 伺服器端 session 狀態（記憶體，key = 隊伍擁有者 authentication）
- 該玩家的 `AggroSettings`（由 client 上傳）。
- 當自動加入仇恨時：加入伺服器端集合，並以 `PacketAggroHatredAdded` 通知該玩家 client 寫入 config。
- 記錄 client 連線時間作為 `firstSeen` 的來源（若 config 尚無）。

### 7.3 有效敵對集合（effectiveHatred）
對隊伍擁有者 P，敵對玩家集合 = `hatred(P) − whitelist(P)`（白名單優先）。

衍生的敵人：若玩家 `A ∈ effectiveHatred(P)`，則 `A` 的冒險隊伍成員（`A.adventureParty.getMobs()`）
也視為 P 的敵人，但不寫入清單。

### 7.4 觸發事件（伺服器端）
在 `Mob.isHit(MobWasHitEvent, Attacker)` 之後判斷（僅 `isServer()`）：

- `victim = mob`；`attackerOwner = attacker.getAttackOwner()`。
- 若 `attackerOwner` 不是玩家 → 忽略（怪物不觸發本系統）。
- 前置：`enabled` 且未 `temporarilyDisabled`。

情境 A — **玩家受攻擊時**（`victim` 是 `PlayerMob`，`victimClient = victim.getServerClient()`）：
- 若 `autoAddOnPlayerAttacked` 且 `attackerOwner = A` 非白名單：
  - 把 `A` 加入 `hatred(victimClient)`、通知其 client 持久化、聊天提示。
  - 對 `victimClient.adventureParty` 的村民觸發 anger（見 7.6），並把 `A` 的隊員列為衍生敵人。

情境 B — **玩家主動攻擊時**（`victim` 是玩家 `B`、`attackerOwner` 是玩家 `A`）：
- 若 `autoAddOnPlayerAttacks` 且 `B` 非白名單：
  - 把 `B` 加入 `hatred(A 的隊伍)`、通知 `A` 的 client、聊天提示。
  - 對 `A` 的隊伍村民觸發 anger，並把 `B` 的隊員列為衍生敵人。

情境 C — **冒險隊伍成員受攻擊時**（`victim` 是 `HumanMob` 且在冒險隊伍中）：
- 找出其隊伍擁有者 `ownerClient`。
- 若 `autoAddOnPartyMemberAttacked` 且攻擊者對 `ownerClient` 非白名單：
  - 把攻擊者加入 `hatred(ownerClient)`、通知、聊天提示。
  - 對 `ownerClient` 的隊伍村民觸發 anger，含衍生敵人。

> 註：村民「自己」被打時，遊戲原生 `HumanAngerTargetAINode` 也會累積怒氣；此路徑仍需受白名單管制（見 7.5），但只有情境 C 會寫入清單。

情境 D — **隊伍村民攻擊玩家時**：
- 若 `victim` 是玩家，且 `attackerOwner` 是屬於某冒險隊伍的 `HumanMob`：
  - 找出該村民的隊伍擁有者 `ownerClient`。
  - 若 `autoAddOnPlayerAttacked` 且 `ownerClient` 對被擊者非白名單、非本人：
    - 把 `ownerClient` 加入被擊者的仇恨名單（玩家為主），通知、聊天提示。
    - 被擊者村民反擊 `ownerClient` 及其隊伍（衍生敵人，含攻擊的村民）。

### 7.5 白名單抑制原生怒氣
Patch `HumanAngerTargetAINode.addAnger(float, Mob, boolean)`（或 `addNearbyHumansAnger`）：
- 若 `attackOwner` 是玩家，且位於「該村民所屬隊伍的白名單」→ 直接 return，不累積怒氣。

### 7.6 觸發村民反擊（anger 機制）
對目標隊伍的每個 `HumanMob` 村民：
- 取得 `handler = mob.ai.blackboard.getObject(HumanAngerTargetAINode.class, "humanAngerHandler")`。
- 若 `handler != null`：
  - `handler.addAnger(>= 1.0f, attackerOwner, true)`（`alertNearbyToo = true`，通知附近同隊村民）。
  - 對仇恨對象 `A` 的每個冒險隊伍成員 `m`，`handler.addEnemy(m, anger)`（衍生敵人）。
- 不呼叫 `commandFollow/commandAttack`，村民維持跟隨，戰鬥結束後怒氣自然消退。

### 7.7 快捷鍵
- 以 `Control.addModControl` 註冊可重綁控制項（Settings > Controls，mod 區），預設 `N`。
- 於 client tick 檢查 `control.isPressed()`，切換該伺服器區段的 `enabled`（與勾選框同值）：
  - 停用時：清除村民作用中 anger/enemies/currentTarget 並移除 HUMAN_ANGRY，停用期間抑制原生怒氣、不加入新仇恨。
  - 啟用時：由伺服器每秒掃描自動恢復追擊仇恨名單上的人。
- 會即時更新冒險隊伍設定畫面的勾選框，並永久保存。

### 7.8 清單管理 UI
在 `PartyConfigForm` 內新增：
- 一個 `FormCheckBox`「啟用仇恨系統」綁定 `enabled`。
- 三個 `FormCheckBox`：玩家受攻擊時／隊伍成員受攻擊時／玩家主動攻擊時（綁定三個自動記仇旗標）。
- 一個 `FormTextButton`「Key bindings」開啟遊戲 Settings > Controls 頁面（可重綁快捷鍵）。
- 一個按鈕「仇恨／白名單管理」開啟 `AggroSettingsForm`。
- `AggroSettingsForm`（單一頁籤）：
  - 名稱篩選 `FormTextInput`（即時過濾）。
  - 玩家列出來自伺服器登入紀錄（`PacketAggroPlayers`），每列顯示：名稱、登入時間、最後上線時間、目前狀態、來源標記（自動／手動）。
  - 每列提供按鈕：`設為仇恨`／`設為白名單`／`清除`；三者互斥，切換時自動從另一清單移除。
  - 提供「清除全部仇恨」按鈕。
  - 關閉後將變更寫回本機 config 並上傳伺服器。

## 8. Data / Network

- 封包以 Necesse 的 `PacketRegistry` 註冊（init 階段），每個封包一個唯一 id。
- `PacketAggroSettings`：client → server，序列化 `enabled`、三個自動旗標、`hatred`、`hatredSource`、
  `whitelist`、`firstSeen` 對應的資訊、快捷鍵。
- `PacketAggroRequestPlayers`：client → server，無內容。
- `PacketAggroPlayers`：server → client，序列化 `[auth, name, lastOnlineEpoch]` 清單。
- `PacketAggroHatredAdded`：server → client，序列化一個 authentication，通知 client 寫入 `hatred`。
- 送出時機：開啟管理 UI、設定變更、進入遊戲後首次確認連線時。

## 9. Testing Strategy

自動化測試在遊戲 mod 上難以執行，採「編譯驗證 + 結構驗證 + 手動遊戲測試」：

- 編譯：`build.ps1` 必須零錯誤，且 `javac --release 8` 通過。
- 打包結構：jar 內需有 `mod.info`、`resources/`（含 4 個 locale）、class 檔。
- Dedicated server 測試：以 `Server.jar -nogui -mod <dir>` 啟動專用伺服器，搭配一般 client 連線。
- 手動測試腳本（主機或專用伺服器 + 一個 dev client）：
  1. 兩人都裝 mod、開啟世界並開啟 PvP。
  2. Host 邀請村民入隊；dev client 攻擊 host → 村民反擊 dev client，聊天顯示加入提示。
  3. Host 攻擊 dev client → dev client 被列為敵對，村民協助攻擊。
  4. 隊伍成員（村民）被 dev client 攻擊 → dev client 被記仇（情境 C）。
  5. 將 dev client 設為白名單 → 再攻擊 host，村民不反擊。
  6. 仇恨對象帶了自己的隊員 → 那些隊員也被村民視為敵人，但不出現在清單。
  7. `Ctrl+H` 停用 → 攻擊不再觸發；再按恢復。
  8. 管理 UI：名稱篩選、最後上線時間、仇恨/白名單互斥切換、重開後保留。

## 10. Boundaries

- Always：
  - 任何遊戲邏輯改動只在 `isServer()` 執行（伺服器權威）。
  - 相容 dedicated server：`initResources()`、GUI、輸入等 client-only 程式碼不得在伺服器端執行。
  - 所有 patch 需對 `null`、`removed()`、無 `adventureParty` 等情況做防禦。
  - 設定寫入失敗或封包版本不符時安全降級，不崩潰。
- Ask first：
  - 新增任何封包註冊 id（可能與其他 mod 衝突）。
  - 對核心 UI（`PartyConfigForm`）與 `HumanAngerTargetAINode` 的 patch 範圍擴大。
- Never：
  - 修改／重新打包 Necesse 核心 jar。
  - 繞過 PvP／隊伍判定硬把玩家變成可攻擊目標。
  - 把玩家 authentication 以外可識別個資上傳或外洩。

## 11. Success Criteria

- [ ] 編譯與打包成功，jar 可被遊戲載入且不報錯。
- [ ] 冒險隊伍設定畫面出現主開關、三個自動記仇勾選框、快捷鍵設定與管理按鈕。
- [ ] 三種情境各自受勾選框控制且永久記仇（寫入 config）。
- [ ] PvP 下：玩家攻擊隊員 → 村民以怒氣機制反擊且仍跟隨。
- [ ] 仇恨對象的隊伍成員被視為衍生敵人，且不出現在清單。
- [ ] 白名單玩家永不被村民攻擊（含原生怒氣抑制）。
- [ ] 快捷鍵可開／關且即時生效。
- [ ] 管理視窗顯示最後上線時間、名稱篩選、仇恨/白名單互斥切換。
- [ ] 自動加入仇恨時有聊天提示。
- [ ] 清單顯示來源標記，且有「清除全部仇恨」按鈕。
- [ ] 可在 dedicated server 上載入並運作（`clientside = false`）。
- [ ] 四語系檔齊全。

## 12. Risks / 待驗證

1. **最後上線時間來源**：核心 `SavedServerClientData` 無時間欄位；
   計畫改用 `world.fileSystem.getPlayerFile(auth).toFile().lastModified()`（需驗證存取權限與格式），
   並由 mod 於首次見到時記錄 `firstSeen`。
2. `@ModMethodPatch` 是否能掛在**建構子** `<init>` 與 `Input.tick` 上（需先做最小 patch 驗證）。
3. `PacketRegistry` 註冊 API 的實際方法名與 id 衝突規則（需查 1.3 反編譯）。
4. 取得 anger handler 的 blackboard key 與 `getObject` 是否可在 mod 端穩定呼叫。
5. 村民 anger 是否需要讓 client 端同步目標（否則只有伺服器端村民動作、畫面不同步）→ 需實測。
6. 在 `PartyConfigForm` 動態插入元件後的排版/高度調整。
7. 玩家未開 PvP 時功能不會生效（核心限制，需向使用者說明）。
8. 快捷鍵 `Ctrl+H` 可能與遊戲或其他 mod 衝突；提供可重綁。
9. 專用伺服器需同時在伺服器端與所有 client 安裝本 mod；未安裝的 client 無法連線（`clientside = false`）。
10. `getWorldUniqueID()` 在尚未連線/主選單時可能為 0；設定分區只在進入世界後讀寫。

## 13. 已確認決策

- 支援 dedicated server。
- 清單顯示來源標記（自動／手動）。
- 提供「清除全部仇恨」按鈕。
- 清單與勾選設定依「不同伺服器連線」分別保存/讀取（world unique ID）；快捷鍵為全域。
