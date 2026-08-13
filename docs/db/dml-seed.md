# 種子資料 (DML Seed Data)

## 1. 文件資訊

| 項目 | 內容 |
|------|------|
| **狀態 (Status)** | Proposed |
| **日期 (Date)** | 2026-08-14 |
| **角色 (Layer)** | SD — DML（種子資料） |
| **範圍** | Auth DB、Campaign DB、Inventory DB 的初始資料 |

> 本文件為 dev / POC 的種子資料。**prod 上線前務必**：替換 ADMIN 密碼雜湊為營運自訂值、確認範例活動與獎品為營運真實配置。

---

## 2. Auth DB — 角色與 ADMIN 帳號

### 2.1 roles（角色）

```sql
INSERT INTO roles (id, code, name) VALUES
    (1, 'ROLE_USER',  '一般使用者'),
    (2, 'ROLE_ADMIN', '管理人員');
```

### 2.2 users + user_roles（ADMIN 帳號，BCrypt 佔位符）

```sql
-- ADMIN 帳號（POC 以種子資料預先建立，不提供任何提權 API — SA UC-4）
-- password_hash 為 BCrypt 雜湊，絕非明文（FR-AUTH-06）
INSERT INTO users (id, username, email, password_hash) VALUES
    (1, 'admin', 'admin@example.com', '<BCRYPT_HASH>');

-- ADMIN 帳號授與 ROLE_ADMIN（如需 admin 亦可抽獎，再補 (1, 1) 授與 ROLE_USER）
INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 2);  -- admin → ROLE_ADMIN
```

> **`<BCRYPT_HASH>` 佔位符說明**：實際值須以 BCrypt 產生，例如：
>
> ```java
> // Spring Security
> String hash = new BCryptPasswordEncoder().encode("admin123");
> ```
>
> 或 CLI：`htpasswd -bnBC 10 "" "admin123"`
>
> 範例（password = `admin123`；`$2y$` 前綴與 Spring Security 的 `$2a$` 相容，可被 `BCryptPasswordEncoder.matches()` 驗證）：
>
> ```
> $2y$10$O0zmtqBM3kfb1kvHL2iwTOBBB7k/ezcyfpQTVVSS39fRGD3Z7aPHm
> ```

---

## 3. Campaign DB — 範例活動與獎品（機率總和 = 100%）

> 機率配置範例取自 campaign-service SA `AC-CAMP-015`（p1=5%, p2=15%, p3=30%, THANK_YOU=50%），總和 = **100.00%**，滿足 `FR-CAMP-04`。

### 3.1 campaigns（活動）

```sql
INSERT INTO campaigns (id, name, status, start_time, end_time, draw_limit) VALUES
    (1, '2026 中秋轉盤抽獎', 'ACTIVE',
     '2026-08-15 00:00:00+00',
     '2026-09-15 23:59:59+00',
     10);   -- 每使用者於本活動整個週期最多 10 次（活動期間總額，非每日）
```

### 3.2 prizes（3 個 PRIZE + 1 個 THANK_YOU）

```sql
INSERT INTO prizes (id, campaign_id, name, type, probability, stock, sort_order) VALUES
    (1, 1, 'iPhone 16 Pro',      'PRIZE',     5.00,   1, 1),   -- 限量 1 件（熱門獎品，驗證防超抽）
    (2, 1, 'Apple Watch Series', 'PRIZE',    15.00,  10, 2),
    (3, 1, '星巴克 $100 兌換券',  'PRIZE',    30.00, 100, 3),
    (4, 1, '銘謝惠顧',           'THANK_YOU', 50.00,   0, 4);  -- THANK_YOU stock=0 且忽略（視為無限）

-- 機率總和檢查：5.00 + 15.00 + 30.00 + 50.00 = 100.00 ✓ (FR-CAMP-04)
-- 至少一個 THANK_YOU ✓ (FR-CAMP-06)
```

### 3.3 draw_records（無種子）

`draw_records` 為 runtime 產生（每次抽獎一筆），**不提供種子**。其冪等鍵 `UNIQUE(user_id, campaign_id, idempotency_key)`（ADR-005）與 replay 快照語意見 [campaign-db.md](campaign-db.md)。

---

## 4. Inventory DB — 庫存初始值

> 依 inventory-service SA §5.1 註記：庫存真相的**初始值**來自 campaign-service 的獎品配置（`prizes.stock`）。種子階段同步寫入 inventory；此後一切扣減以 inventory-service 為準。`THANK_YOU`（prize_id=4）**無 inventory 列**（銘謝惠顧不扣庫存）。

```sql
INSERT INTO inventory (id, prize_id, stock, version) VALUES
    (1, 1,   1, 0),   -- iPhone 16 Pro：剩餘 1（真相來源，可被條件更新扣減）
    (2, 2,  10, 0),   -- Apple Watch：剩餘 10
    (3, 3, 100, 0);   -- 星巴克券：剩餘 100
```

`reservations` 為 runtime 產生（消費 `inventory-commit` event 時落庫），**不提供種子**。

---

## 5. 種子資料一致性摘要

| 檢查項 | 值 | 對應需求 |
|--------|-----|---------|
| 角色集合 | `ROLE_USER`, `ROLE_ADMIN` | `FR-AUTH-05` |
| ADMIN 帳號 | `admin`（BCrypt 佔位符，無明文） | `FR-AUTH-06`, SA UC-4 |
| 獎品機率總和 | 5.00 + 15.00 + 30.00 + 50.00 = **100.00** | `FR-CAMP-04` |
| 至少一個 THANK_YOU | prize_id=4 `type=THANK_YOU` | `FR-CAMP-06` |
| 每獎品機率 ∈ [0,100] | 全部落於區間 | `FR-CAMP-06` |
| `draw_limit >= 1` | 10 | SA UC-1 |
| `end_time > start_time` | 2026-08-15 → 2026-09-15 | SA UC-1 |
| inventory 初始值 = prizes.stock | 1 / 10 / 100 對齊 | SA inventory §5.1 |
