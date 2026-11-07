# 已知问题：多计分板混用导致的 Team 不同步

本文件是 BetterNameTag 的「已知问题 · 多计分板 team 不同步」的**完整根因分析与解决方案**。
README 的「已知问题」一节只做摘要，详细内容以本文件为准。

---

## 1. 现象

当 BetterNameTag 与其他同样操作 `Scoreboard` / `Team` 的插件（侧边栏、计分板、称号、竞技场等）同时运行时，玩家头顶 NameTag 会出现 team 不同步：

- 前缀不显示，或显示成别人的前缀；
- 后加入的玩家看不到先加入玩家的团队前缀；
- 玩家切换场景（子服 / 世界 / 游戏模式）后前缀丢失或残留；
- 偶发的 team 闪烁、客户端需要重进才恢复。

---

## 2. 根因（定位到子文件）

根因**集中在 `src/main/java/top/cnolm/betterNameTag/NameTagManager.java`**，具体是这两个方法：

- `applyTeam(Player, prefix, suffix)`（约 178–214 行）
- `syncTeamsToPlayer(Player)`（约 134–176 行）

二者都直接拿 `player.getScoreboard()` 当作「我们的板」往上注册 `bnt_` 队伍，存在三类问题：

### 2.1 污染共享主计分板

在 1.8 中，新玩家（以及被其他插件重置过的玩家）默认持有**全局共享的主计分板** `Bukkit.getScoreboardManager().getMainScoreboard()`。`applyTeam` 在其上注册 `bnt_` 队，会导致：

- 所有玩家共用同一块板，一个人的 team 变化全员可见；
- 其他插件对主板的 `resetScores` / 清队会清掉别人的数据；
- 与同样操作主板的插件互相踩踏。

### 2.2 不做「板归属」判定

`applyTeam` / `syncTeamsToPlayer` 不区分「这是不是我们的板」：

- 若当前板是**别的插件拥有的板**，BetterNameTag 把 `bnt_` 队注册进去污染它；该插件一重写 / 清空自己的板，BetterNameTag 的队就消失 → 不同步。
- 若当前板是**主板**（见 2.1），则跨玩家污染。

### 2.3 `setScoreboard` 与 team 下发的时序错误

`applyTeam` 末尾 `if (player.getScoreboard() != targetBoard) player.setScoreboard(targetBoard);` 存在时序问题：

- team 在 `setScoreboard` 之前 / 之中被增改，但客户端对已**生成实体**的 nametag 不会因后续增量 team 包而重刷；
- `syncTeamsToPlayer` 用 `oldTeam.unregister()` + `registerNewTeam(...)` 重建队，触发 team 闪烁且丢失本次关联。

> **一句话根因**：`NameTagManager` 把「玩家当前计分板」当成专属板直接写 team，且缺少「板归属判定 + 顺序下发 + 实体重新关联」三步，导致在多计分板环境下跨玩家污染与客户端不重刷。

---

## 3. 解决方案（直接给出）

核心思想：BetterNameTag 必须**拥有并独占一块「nametag 板」**（每个玩家一块，或在共享板场景下用标记区分），所有 `bnt_` 队只写在这块板上；向客户端下发时严格遵循 **「先同步 team → 再 `setScoreboard` → 最后补一次实体重新关联」** 的顺序。

### 3.1 板归属判定 `resolveBoard(Player)`

```java
private static final String MARKER = "bnt"; // 本插件在自有板上注册的标记 objective 名

private Scoreboard resolveBoard(Player player) {
    Scoreboard current = player.getScoreboard();
    boolean ours = current != null
        && current != Bukkit.getScoreboardManager().getMainScoreboard()
        && current.getObjective(MARKER) != null; // 带我们的标记 = 我们的板
    if (ours) {
        return current; // 原地复用，不重建
    }
    // 否则新建一块专属板（客户端只因 setScoreboard 重置这一次）
    Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    board.registerNewObjective(MARKER, "dummy"); // 仅作归属标记，不显示
    return board;
}
```

> 若 BetterNameTag 同时负责渲染侧边栏，则由它独占这块板并在其上渲染（「单一计分板协作」模式）；若侧边栏由别的插件负责，应改为「把 `bnt_` 队**增量同步**到对方板」，而不是抢板。

### 3.2 顺序下发：syncTeams → setScoreboard → 实体重新关联

```java
Scoreboard board = resolveBoard(player);

// 1) 先把队同步到 board（此时 board 上已有 team，sendScoreboard 会一并下发）
Team team = getOrCreateTeam(board, player); // 不存在才新建，已存在原地复用
team.setPrefix(prefix);
team.setSuffix(suffix);
team.addEntry(player.getName());

// 2) 再 setScoreboard（仅当还不是这块板）—— 客户端在生成其他玩家实体前就拿到了前缀
if (player.getScoreboard() != board) {
    player.setScoreboard(board);
}

// 3) 延迟几 tick 补一次「实体重新关联」，强制已生成实体刷出前缀
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    if (player.getScoreboard() != board) return;       // 板已被换掉则放弃
    if (team.hasEntry(player.getName())) {
        team.removeEntry(player.getName());            // 发 REMOVE_PLAYERS
        team.addEntry(player.getName());               // 发 ADD_PLAYERS
    }
}, 3L);
```

### 3.3 增量更新，禁止 unregister + register

`syncTeamsToPlayer` / `applyTeam` 改为「队不存在才新建，已存在原地 `setPrefix` / `setSuffix`」，去掉 `oldTeam.unregister()` 重建逻辑，避免闪烁与关联丢失。

### 3.4 监听板切换，自动重新同步

Bukkit 没有 `setScoreboard` 事件。解决：在 BetterNameTag 自己的刷新节奏里（玩家加入、LuckPerms 权限变更、以及必要的周期 tick）检测 `player.getScoreboard()` 是否仍为我们的板（用 `MARKER` 判定）；若被别的插件换掉，重新走 3.2 把 `bnt_` 队同步到新板。

---

## 4. 验收标准

- 单独运行 BetterNameTag：前缀正常显示；
- 与侧边栏 / 计分板插件同时运行：两边互不污染，前缀稳定、不闪、后加入玩家也能看到先加入玩家前缀；
- 玩家切场景后前缀不丢失、不残留。

---

## 5. 相关代码位置

| 现状 | 文件 | 方法 |
| --- | --- | --- |
| 根因所在 | `src/main/java/top/cnolm/betterNameTag/NameTagManager.java` | `applyTeam`、`syncTeamsToPlayer` |
| 触发刷新入口 | 同上 | `updatePlayer`、`reloadAllPlayers` |
| 对外 API（覆盖层 / 同步） | `src/main/java/top/cnolm/betterNameTag/api/BetterNameTagAPI.java` | `setCoverPrefix`、`syncTeamsToPlayer` |
