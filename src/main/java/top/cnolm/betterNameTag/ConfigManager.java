package top.cnolm.betterNameTag;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<Integer, GroupConfig> groups = new TreeMap<>();
    private LuckPerms luckPerms;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            plugin.getLogger().warning("无法连接到 LuckPerms，将回退到 Bukkit 权限检查");
        }
        reload();
    }

    public void reload() {
        groups.clear();
        plugin.reloadConfig();

        ConfigurationSection groupsSection = plugin.getConfig().getConfigurationSection("Groups");
        if (groupsSection == null) {
            plugin.getLogger().warning("未找到 Groups 配置节，使用默认配置");
            loadDefaultGroups();
            return;
        }

        for (String key : groupsSection.getKeys(false)) {
            ConfigurationSection group = groupsSection.getConfigurationSection(key);
            if (group == null) continue;

            String permission = group.getString("Permission");
            String prefix = group.getString("Prefix", "");
            String suffix = group.getString("Suffix", "");
            int priority = group.getInt("SortPriority", 100);

            if (permission == null || permission.isEmpty()) {
                plugin.getLogger().warning("分组 " + key + " 缺少 Permission 字段，已跳过");
                continue;
            }

            GroupConfig config = new GroupConfig(
                    key,
                    permission,
                    ChatColor.translateAlternateColorCodes('&', prefix),
                    ChatColor.translateAlternateColorCodes('&', suffix),
                    priority
            );
            groups.put(priority, config);
        }

        if (groups.isEmpty()) {
            loadDefaultGroups();
        }
    }

    private void loadDefaultGroups() {
        GroupConfig defaultGroup = new GroupConfig(
                "Default",
                "bnt.default",
                ChatColor.translateAlternateColorCodes('&', "&7"),
                ChatColor.translateAlternateColorCodes('&', "&f"),
                9999
        );
        groups.put(9999, defaultGroup);
    }

    /**
     * 检查玩家是否拥有某个权限（正确处理继承，排除 OP 万能权限）
     */
    private boolean hasPermissionExact(Player player, String permission) {
        // 如果 LuckPerms 可用，使用它的缓存数据查询
        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    // getCachedData().getPermissionData() 会计算所有继承的权限
                    CachedPermissionData permissionData = user.getCachedData().getPermissionData(QueryOptions.defaultContextualOptions());
                    // checkPermission 返回 Tristate.TRUE / FALSE / UNDEFINED
                    return permissionData.checkPermission(permission).asBoolean();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("LuckPerms 查询失败，回退到 Bukkit 权限检查: " + e.getMessage());
            }
        }

        // 回退方案：Bukkit 原生检查，但 OP 需要额外处理
        if (player.isOp()) {
            // OP 玩家：检查是否拥有 bnt.bypassop 权限才绕过 OP 限制
            return player.hasPermission("bnt.bypassop") && player.hasPermission(permission);
        }
        // 非 OP 玩家使用常规检查
        return player.hasPermission(permission);
    }

    /**
     * 获取玩家应该应用的分组
     */
    public GroupConfig getGroupForPlayer(Player player) {
        for (Map.Entry<Integer, GroupConfig> entry : groups.entrySet()) {
            GroupConfig group = entry.getValue();
            if (hasPermissionExact(player, group.permission)) {
                return group;
            }
        }

        // 没有匹配任何组，返回默认组
        return groups.values().stream()
                .filter(g -> g.permission.equals("bnt.default"))
                .findFirst()
                .orElse(groups.values().iterator().next());
    }

    public Map<Integer, GroupConfig> getGroups() {
        return groups;
    }

    public static class GroupConfig {
        public final String name;
        public final String permission;
        public final String prefix;
        public final String suffix;
        public final int priority;

        public GroupConfig(String name, String permission, String prefix, String suffix, int priority) {
            this.name = name;
            this.permission = permission;
            this.prefix = prefix;
            this.suffix = suffix;
            this.priority = priority;
        }
    }
}