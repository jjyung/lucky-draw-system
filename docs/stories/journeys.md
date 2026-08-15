# 使用者旅程 (User Journeys)

> **目的**：以「角色端到端旅程」串起跨服務的 story，揭露 FR 列表與 per-service SA 無法單獨表達的**隱含需求**。SD 應先讀本檔，再設計 API。
>
> 每段旅程標注：**觸及的 story**、**跨過的服務**、**對應 API/事件**、以及**揭露的隱含需求（缺口）**。

## 旅程總覽

| Journey | 角色 | 路徑 | 揭露的關鍵缺口 |
|---------|------|------|----------------|
| [J-1](#j-1) | ADMIN | 登入 → 建活動 → 配獎品 → 啟用 → 動態調整 → 結束 | 動態改獎品後，庫存需同步給 inventory |
| [J-2](#j-2) | USER | 註冊 → 登入 → 瀏覽活動 → 抽獎 → 得結果 | **活動詳情端點**（開放點 #1） |
| [J-3](#j-3) | USER/平台 | 抽獎重送 → replay；庫存不足 → 降級 → 補償 | 補償告警的可觀察性出口 |

---

## J-1 營運人員旅程 (ADMIN Journey)

> 從零到一個「可抽獎」的活動，並在活動期間動態調整。

```
ADMIN ──登入──► auth ──簽發憑證(ROLE_ADMIN)──► gateway 驗證+傳遞身份
   │
   ├─ 建立活動 (DRAFT) ──► campaign ──► INSERT campaigns
   ├─ 配置獎品+機率(總和100%) ──► campaign ──► upsert prizes + 發布 prize-stock-configured
   ├─ 啟用活動 (DRAFT→ACTIVE) ──► campaign ──► 狀態轉移
   ├─ 動態改獎品(名稱/數量/機率) ──► campaign ──► 後續抽獎生效 + 發布 prize-stock-configured
   └─ 結束活動 (ACTIVE→ENDED) ──► campaign ──► 狀態轉移(終態)
```

- **觸及 story**: ST-AUTH-002/004, ST-CAMP-001/002/003, ST-GW-001/002/005, ST-X-001/002
- **對應 API**: `auth-tokens-001` → `campaign-campaigns-003/004/005` → `campaign-prizes-001`
- **揭露的隱含需求**:
  1. ADMIN 建立活動前，必須先**登入取得 `ROLE_ADMIN` 憑證**，且 gateway 會驗證+傳遞身份（story 已涵蓋）。
  2. 動態改獎品「數量」時，campaign 需發布 `prize-stock-configured` 事件同步 inventory（ADR-010）——此為 **SD 已知事件**，但 FR 未明列「數量變更要同步庫存」的跨服務副作用，屬**隱含需求，已由 ADR-010 補齊**。

---

## J-2 抽獎者旅程 (USER Journey)

> 從註冊到抽中獎品（或銘謝惠顧）的完整路徑。

```
USER ──註冊──► auth ──► INSERT users (ROLE_USER, 密碼雜湊)
   │
   ├─ 登入 ──► auth ──► 簽發憑證
   │
   ├─ 瀏覽活動列表 ──► campaign (PUBLIC) ──► GET /campaigns
   ├─ 查看活動詳情 ──► campaign (PUBLIC) ──► GET /campaigns/{id}   ★開放點#1
   │
   ├─ 抽獎(單次/批次, 帶 Idempotency-Key) ──► gateway 檢查冪等識別 ──► campaign
   │      ├─ 檢查剩餘次數(活動總額)
   │      ├─ 權重隨機選獎
   │      ├─ 命中獎品 → 確認庫存；不足 → 降級銘謝惠顧
   │      ├─ 記錄結果 + 計次
   │      └─ 中獎 → 發布 inventory-commit ──► inventory 扣減真相
   └─ 收到結果(中獎/銘謝惠顧)
```

- **觸及 story**: ST-AUTH-001/002, ST-CAMP-004/005/006/007/008/009, ST-GW-001/003/004/005/006, ST-X-001/002
- **對應 API**: `auth-users-001` → `auth-tokens-001` → `campaign-campaigns-001/002` → `campaign-draws-001` → 事件 `inventory-commit`
- **揭露的隱含需求**:
  1. **活動詳情端點**：USER 抽獎前需「查看活動詳情」以決定是否參與。此情境 FR **未明列**，SA 亦未寫 UC，但 SD 已自行補了 `campaign-campaigns-002` 並標為開放點。→ **這是缺口，需回寫 story + SA UC**（見審計 §3）。
  2. 活動列表/詳情為 **PUBLIC**（FR-GW-06「活動查詢等公開功能」），無需登入——story 已涵蓋於 ST-GW-006。

---

## J-3 重送與防超抽旅程 (Replay & Anti-Overselling Journey)

> 抽獎請求重送、以及庫存不足時的降級與補償路徑。

```
USER ──抽獎請求──► gateway ──► campaign
   │
   ├─ 相同 Idempotency-Key 重送 ──► campaign 回傳首次結果(replay, 不重抽/不重扣/不重計)
   │
   └─ 命中獎品但庫存不足 ──► campaign 降級銘謝惠顧(不重抽, 仍計次)
                              └─► (若扣減時仍不足) inventory 撤銷中獎 + 校正 + 告警(OPS)
```

- **觸及 story**: ST-CAMP-008/009, ST-INV-001/002/003, ST-GW-004
- **對應 API/事件**: `campaign-draws-001`（replay 語意）→ 事件 `inventory-commit`（冪等）→ 補償（無對外 API，內部排程/告警）
- **揭露的隱含需求**:
  1. replay 語意要求「回傳與首次逐位元一致」，此約束在 SD（API README §2.2）已明訂，story 層以「回傳相同結果」表達，**無缺口**。
  2. 補償（ST-INV-003）的告警出口屬 **NFR-06 可觀察性**，無對外 API，由 inventory 內部實現——**已由 ADR-006/007 補齊，無缺口**。
