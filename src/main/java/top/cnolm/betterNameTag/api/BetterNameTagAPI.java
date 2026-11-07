package top.cnolm.betterNameTag.api;

import org.bukkit.entity.Player;
import top.cnolm.betterNameTag.BetterNameTag;
import top.cnolm.betterNameTag.ConfigManager;
import top.cnolm.betterNameTag.NameTagManager;

/**
 * BetterNameTag 的公开 API 接口
 * 用于其他插件调用，管理玩家的名称标签
 */
public class BetterNameTagAPI {

    private final BetterNameTag plugin;
    private final NameTagManager nameTagManager;
    private final ConfigManager configManager;

    public BetterNameTagAPI(BetterNameTag plugin) {
        this.plugin = plugin;
        this.nameTagManager = plugin.getNameTagManager();
        this.configManager = plugin.getConfigManager();
    }

    // ==================== 覆盖层管理 ====================

    /**
     * 为玩家设置覆盖前缀（pre2）
     * 只影响头顶 NameTag，不影响 TabList
     *
     * @param player 目标玩家
     * @param prefix 前缀（支持 & 颜色代码）
     */
    public void setCoverPrefix(Player player, String prefix) {
        if (player == null || prefix == null) return;
        nameTagManager.setCoverPrefix(player, prefix);
    }

    /**
     * 为玩家设置覆盖后缀（pre2）
     * 只影响头顶 NameTag，不影响 TabList
     *
     * @param player 目标玩家
     * @param suffix 后缀（支持 & 颜色代码）
     */
    public void setCoverSuffix(Player player, String suffix) {
        if (player == null || suffix == null) return;
        nameTagManager.setCoverSuffix(player, suffix);
    }

    /**
     * 移除玩家的覆盖层（pre2）
     * 恢复为基础前缀（pre1）
     *
     * @param player 目标玩家
     */
    public void removeCover(Player player) {
        if (player == null) return;
        nameTagManager.removeCover(player);
    }

    /**
     * 获取玩家的覆盖前缀
     *
     * @param player 目标玩家
     * @return 覆盖前缀，如果没有则返回 null
     */
    public String getCoverPrefix(Player player) {
        if (player == null) return null;
        return nameTagManager.getCoverPrefix(player);
    }

    /**
     * 获取玩家的覆盖后缀
     *
     * @param player 目标玩家
     * @return 覆盖后缀，如果没有则返回 null
     */
    public String getCoverSuffix(Player player) {
        if (player == null) return null;
        return nameTagManager.getCoverSuffix(player);
    }

    /**
     * 检查玩家是否有覆盖层
     *
     * @param player 目标玩家
     * @return true 如果有覆盖层
     */
    public boolean hasCover(Player player) {
        if (player == null) return false;
        return nameTagManager.hasCover(player);
    }

    // ==================== 名称刷新 ====================

    /**
     * 强制刷新玩家的名称标签
     *
     * @param player 目标玩家
     */
    public void updatePlayer(Player player) {
        if (player == null) return;
        nameTagManager.updatePlayer(player);
    }

    /**
     * 刷新所有在线玩家的名称标签
     */
    public void updateAllPlayers() {
        nameTagManager.reloadAllPlayers();
    }

    public void syncTeamsToPlayer(Player player) {
        if (player == null) return;
        nameTagManager.syncTeamsToPlayer(player);
    }

    // ==================== 信息查询 ====================

    /**
     * 获取玩家当前应用的基础分组（pre1）
     *
     * @param player 目标玩家
     * @return 分组配置，包含前缀和后缀
     */
    public GroupInfo getBaseGroup(Player player) {
        if (player == null) return null;
        ConfigManager.GroupConfig config = configManager.getGroupForPlayer(player);
        return new GroupInfo(config.name, config.prefix, config.suffix, config.priority);
    }

    /**
     * 获取所有已配置的分组
     *
     * @return 分组列表
     */
    public java.util.Collection<GroupInfo> getAllGroups() {
        java.util.Collection<GroupInfo> result = new java.util.ArrayList<>();
        for (ConfigManager.GroupConfig config : configManager.getGroups().values()) {
            result.add(new GroupInfo(config.name, config.prefix, config.suffix, config.priority));
        }
        return result;
    }

    // ==================== 配置管理 ====================

    /**
     * 重载配置文件
     */
    public void reload() {
        configManager.reload();
        nameTagManager.reloadAllPlayers();
    }

    // ==================== 数据类 ====================

    /**
     * 分组信息（不可变数据类）
     */
    public static class GroupInfo {
        public final String name;
        public final String prefix;
        public final String suffix;
        public final int priority;

        public GroupInfo(String name, String prefix, String suffix, int priority) {
            this.name = name;
            this.prefix = prefix;
            this.suffix = suffix;
            this.priority = priority;
        }

        @Override
        public String toString() {
            return "GroupInfo{" +
                    "name='" + name + '\'' +
                    ", prefix='" + prefix + '\'' +
                    ", suffix='" + suffix + '\'' +
                    ", priority=" + priority +
                    '}';
        }
    }
}