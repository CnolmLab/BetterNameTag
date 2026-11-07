# BetterNameTag

> 基于 Bukkit / Spigot（Minecraft 1.8.8）的自定义名称标签插件，支持 LuckPerms 分组前缀/后缀、临时覆盖层与开放 API。

[![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-red.svg)](./README.md)

---

## 特性

- **头顶 NameTag + TabList 同步**：为玩家显示分组前缀/后缀，TabList 始终使用基础前缀。
- **LuckPerms 深度集成**：通过 LuckPerms API 计算**继承后的权限**来判定分组；未安装 LuckPerms 时回退到 Bukkit 原生权限检查（OP 需 `bnt.bypassop` 才绕过限制）。
- **双层前缀模型**：
  - `pre1` 基础前缀/后缀 —— 来自 `config.yml` 的 `Groups` 配置，按 `SortPriority` 升序取首个匹配分组。
  - `pre2` 覆盖层前缀/后缀 —— 临时覆盖，**只影响头顶 NameTag，不影响 TabList**；可通过命令或 API 设置/移除。
- **权限变更自动刷新**：监听 LuckPerms 用户权限变更事件，自动更新受影响玩家的名称标签。
- **开放 API**：供其他插件查询/设置覆盖层、刷新标签、同步计分板队伍、读取分组等。
- **命令管理**：覆盖前缀/后缀、移除覆盖、重载配置、查询玩家信息。

---

## 环境要求

| 依赖 | 说明 |
| --- | --- |
| Java 8 | 编译与运行目标版本（`sourceCompatibility = 1.8`） |
| Spigot / Paper 1.8.8 | `plugin.yml` 中 `api-version: 1.8` |
| LuckPerms | **软依赖**：缺失时回退原生权限检查，分组前缀仍按 Bukkit 权限判定 |
| ProtocolLib 4.4.0 | 运行时依赖，已打包于 `src/libs/ProtocolLib-4.4.0.jar` |

---

## 安装

1. 从 [Releases](../../releases) 下载最新构建的 `BetterNameTag.jar`；
2. 放入服务器的 `plugins/` 目录；
3. 启动服务器，自动生成 `plugins/BetterNameTag/config.yml`；
4. （可选）在 LuckPerms 中配置分组与对应权限节点。

---

## 配置（`config.yml`）

分组按 `SortPriority` **升序**遍历，返回**首个权限匹配**的分组；均无匹配时回退默认组。

```yaml
Groups:
  Owner:
    Permission: nte.owner        # 判定分组所用的权限节点
    Prefix: '&4&o[Owner]'        # 前缀，支持 & 颜色代码
    Suffix: '&f'                 # 后缀，支持 & 颜色代码
    SortPriority: 1              # 数值越小优先级越高
  Admin:
    Permission: nte.admin
    Prefix: '&c&o[Admin]'
    Suffix: '&f'
    SortPriority: 2
  Default:
    Permission: nte.default
    Prefix: '&7'
    Suffix: '&f'
    SortPriority: 3
```

- 前缀可见字符超过 16 字符会自动截断（Bukkit Team 前缀长度限制）。
- 修改后执行 `/bnt reload` 热加载，无需重启。

---

## 命令与权限

主命令 `/bnt`（别名 `/betternametag`），需要权限 `bnt.admin`（默认 OP）。

| 命令 | 说明 |
| --- | --- |
| `/bnt coverprefix <玩家> <前缀>` | 为玩家设置覆盖前缀（`pre2`），支持 `&` 颜色码 |
| `/bnt coversuffix <玩家> <后缀>` | 为玩家设置覆盖后缀（`pre2`） |
| `/bnt removecover <玩家>` | 移除玩家覆盖层，恢复基础前缀 |
| `/bnt reload` | 重新加载配置并刷新所有在线玩家 |
| `/bnt info <玩家>` | 查看玩家的基础/覆盖前缀后缀 |

---

## API 用法（供其他插件调用）

通过 `NameTagProvider` 获取 API 实例，使用前请确认 `NameTagProvider.isAvailable()`。

```java
import top.cnolm.betterNameTag.api.NameTagProvider;
import top.cnolm.betterNameTag.api.BetterNameTagAPI;

public class MyPlugin {
    public void example(org.bukkit.entity.Player player) {
        if (!NameTagProvider.isAvailable()) return;

        BetterNameTagAPI api = NameTagProvider.getAPI();

        // 设置临时覆盖前缀（仅头顶 NameTag，不影响 TabList）
        api.setCoverPrefix(player, "&6[VIP]");

        // 移除覆盖
        api.removeCover(player);

        // 刷新 / 同步到玩家当前计分板
        api.updatePlayer(player);
        api.syncTeamsToPlayer(player);

        // 读取分组信息
        BetterNameTagAPI.GroupInfo base = api.getBaseGroup(player);
        String prefix = base.prefix; // 基础前缀
    }
}
```

主要方法一览：

- 覆盖层：`setCoverPrefix` / `setCoverSuffix` / `removeCover` / `getCoverPrefix` / `getCoverSuffix` / `hasCover`
- 刷新：`updatePlayer` / `updateAllPlayers` / `syncTeamsToPlayer`
- 查询：`getBaseGroup` / `getAllGroups`
- 配置：`reload`
- 数据类：`BetterNameTagAPI.GroupInfo`（name / prefix / suffix / priority）

---

## 从源码构建

前置：JDK 8，可访问 Maven 仓库（Spigot、LuckPerms）的网络环境。

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

成品位于 `build/libs/BetterNameTag-1.0.0.jar`。

> 说明：`src/libs/` 下的 `ProtocolLib-4.4.0.jar` 已纳入版本控制并参与打包；`NickAPI-7.11.jar` 已废弃且代码未引用，已被 `.gitignore` 排除。

---

## 已知问题

### 多计分板混用导致 team 不同步

**现象**：当本插件与其他同样操作 Scoreboard / Team 的插件（侧边栏、计分板类插件等）同时运行时，玩家头顶 NameTag 可能出现 team 不同步——例如前缀不显示、后加入的玩家看不到先加入玩家的团队前缀等。

**根因**：
1. 在 1.8 中，新玩家默认持有**全局共享的主计分板**；若直接在其上注册 objective / team，会导致所有玩家共用同一块板，且 `resetScores` 会清掉他人数据。
2. 若 `setScoreboard` 时 board 为空、之后再增量下发 team 包，客户端不会对**已生成的实体**重刷 NameTag 前缀。
3. team 的 `REMOVE_PLAYERS` / `ADD_PLAYERS` 包时机不正确时，前缀同样无法刷新。

**解决方案（参考实现）**：完整修复思路见同级项目 `ultrapvp` 的计分板管理器：

```
ultrapvp/src/main/java/top/cnolm/ultrapvp/_bases/scoreboard/BaseScoreboardManager.java
```

核心要点：

1. **板归属判定（`resolveBoard`）**：不要直接 `player.getScoreboard()` 上注册。先判定当前板是否“属于自己的板”（非主计分板 **且** 已带有本插件 objective 名）；是则原地复用（标题/槽位变化仅发 UPDATE 包），否则新建板并 `setScoreboard`（客户端只重置这一次）。
2. **顺序保证（`syncTeams` 先于 `setScoreboard`）**：新建板时，先同步 team 再 `setScoreboard`，使 `sendScoreboard` 把 team 一并下发，客户端在生成其他玩家实体前即拿到前缀。
3. **实体重新关联补丁（`scheduleEntryResync`）**：建板后延迟若干 tick，对每个 `bnt_` team 的 entry 执行 `removeEntry` + `addEntry`，补发 `REMOVE_PLAYERS` + `ADD_PLAYERS` 包，强制客户端把已下发实体重新关联到 team、刷出前缀（仅首次执行一次，不影响增量更新、不会每秒闪烁）。
4. **增量 team 同步（`createTeamOnBoard`）**：team 不存在才新建，已存在则原地 `setPrefix` / `setSuffix`，不再 `unregister` + `register`，避免 team 闪烁。

> 注：本仓库 `NameTagManager` 当前采用的是“直接应用到玩家当前计分板”的简化策略，尚未合并上述多计分板兼容补丁。若需在多计分板混用场景下彻底修复，建议按 `BaseScoreboardManager` 的思路改造 `syncTeamsToPlayer` / `applyTeam`。

---

## 许可证

本项目目前**未声明开源许可证**（保留所有权利）。如需以开源方式发布，请补充 `LICENSE` 文件。
