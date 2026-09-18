# Todo: Party Aggro

## 已完成
- [x] T1 建置腳本 + 最小 mod 骨架（build.ps1、mod.info、可載入 jar）
- [x] T2 最小 patch 驗證（Mob.isHit / Input.tick / 建構子）
- [x] T3 純邏輯資料模型 + 單元測試（互斥、effectiveHatred、序列化）
- [x] T4 ModSettings per-server 持久化
- [x] T5 PacketAggroSettings（client→server）+ server session
- [x] T6 三情境觸發邏輯（玩家受攻擊 / 玩家主動攻擊 / 隊伍成員受攻擊）
- [x] T7 村民 anger 反擊 + 衍生敵人 + 白名單抑制原生怒氣
- [x] T8 PacketAggroHatredAdded（server→client 持久化）
- [x] T9 隊伍設定畫面控制項（主開關、三勾選、管理鈕、Key bindings 鈕）
- [x] T10 管理視窗（單頁籤、互斥、篩選、首次/最後上線、來源、清除全部、可拖曳置頂、圖示按鈕）
- [x] T11 快捷鍵（Control 註冊、預設 N、可於 Settings > Controls 重綁）
- [x] T12 四語系（en / zh-TW / zh-CN / ja）+ preview.png
- [x] T13 邊界防呆（try/catch、null 檢查、斷線清除 server 狀態）

## 待驗證（需要第二個玩家 / PvP）
- [ ] 情境 A：玩家攻擊隊伍成員 → 村民反擊
- [ ] 情境 B：玩家主動攻擊他人 → 對方被記仇
- [ ] 情境 C：隊伍成員（村民）被攻擊 → 攻擊者被記仇
- [ ] 白名單玩家不被村民攻擊（含原生怒氣抑制）
- [ ] 衍生敵人：仇恨對象的隊伍成員也被視為敵人
- [ ] 聊天提示「已將 XXX 加入敵對名單」
- [ ] 自動加入的仇恨寫回 client config（重啟後保留）

## 後續可選
- [ ] 管理視窗：加入排序（名稱/時間）、分頁
- [ ] 仇恨自動過期（目前永久）
- [ ] 更多圖示/排版微調
