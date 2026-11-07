package top.cnolm.betterNameTag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class NameTagManager {

    private final BetterNameTag plugin;
    private final Scoreboard mainScoreboard;
    private final Map<UUID, String> coverPrefixes = new HashMap<>();
    private final Map<UUID, String> coverSuffixes = new HashMap<>();
    private final Map<UUID, String> activeTeamNames = new HashMap<>();

    private static final int MAX_VISIBLE_LENGTH = 16;

    public NameTagManager(BetterNameTag plugin) {
        this.plugin = plugin;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.mainScoreboard = manager.getMainScoreboard();
    }

    public void setCoverPrefix(Player player, String prefix) {
        coverPrefixes.put(player.getUniqueId(), prefix);
        updatePlayer(player);
    }

    public void setCoverSuffix(Player player, String suffix) {
        coverSuffixes.put(player.getUniqueId(), suffix);
        updatePlayer(player);
    }

    public void removeCover(Player player) {
        coverPrefixes.remove(player.getUniqueId());
        coverSuffixes.remove(player.getUniqueId());
        updatePlayer(player);
    }

    public String getCoverPrefix(Player player) {
        return coverPrefixes.get(player.getUniqueId());
    }

    public String getCoverSuffix(Player player) {
        return coverSuffixes.get(player.getUniqueId());
    }

    public boolean hasCover(Player player) {
        return coverPrefixes.containsKey(player.getUniqueId())
                || coverSuffixes.containsKey(player.getUniqueId());
    }

    public void updatePlayer(Player player) {
        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GroupConfig baseConfig = configManager.getGroupForPlayer(player);

        // 获取基础前缀（pre1）
        String basePrefix = baseConfig.prefix;
        String baseSuffix = baseConfig.suffix;

        // 检查是否有覆盖层（pre2），如果有则覆盖 NameTag
        String nameTagPrefix = basePrefix;
        String nameTagSuffix = baseSuffix;
        if (coverPrefixes.containsKey(player.getUniqueId())) {
            nameTagPrefix = coverPrefixes.get(player.getUniqueId());
        }
        if (coverSuffixes.containsKey(player.getUniqueId())) {
            nameTagSuffix = coverSuffixes.get(player.getUniqueId());
        }

        // 清理长度
        nameTagPrefix = sanitizePrefix(nameTagPrefix);
        nameTagSuffix = sanitizeSuffix(nameTagSuffix);

        // 1️ 更新 NameTag（使用队伍）
        applyTeam(player, nameTagPrefix, nameTagSuffix);

        // 2️ 更新 TabList（使用 Player.setPlayerListName）
        // TabList 始终使用基础前缀（pre1），不被覆盖层影响
        String tabName = basePrefix + player.getName() + baseSuffix;
        String coloredTabName = ChatColor.translateAlternateColorCodes('&', tabName);
        // 确保不超过 16 字符
//        if (sanitizedTabName.length() > MAX_VISIBLE_LENGTH) {
//            sanitizedTabName = sanitizedTabName.substring(0, MAX_VISIBLE_LENGTH);
//        }
        player.setPlayerListName(coloredTabName);
    }

    private String sanitizePrefix(String input) {
        if (input == null || input.isEmpty()) return "";

        String colored = ChatColor.translateAlternateColorCodes('&', input);

        // 分离颜色代码和可见字符，保留所有颜色代码
        StringBuilder colorCodes = new StringBuilder();
        StringBuilder visible = new StringBuilder();

        for (int i = 0; i < colored.length(); i++) {
            char c = colored.charAt(i);
            if (c == '§' && i + 1 < colored.length()) {
                // 保留所有颜色代码
                colorCodes.append(c);
                colorCodes.append(colored.charAt(i + 1));
                i++;
            } else {
                visible.append(c);
            }
        }

        // 限制可见字符长度
        if (visible.length() > MAX_VISIBLE_LENGTH) {
            visible.setLength(MAX_VISIBLE_LENGTH);
            plugin.getLogger().warning("前缀被截断: " + visible);
        }

        // 返回：所有颜色代码 + 截断后的可见字符
        return colorCodes.toString() + visible.toString();
    }

    private String sanitizeSuffix(String input) {
        if (input == null || input.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * 将玩家的队伍同步到他的当前计分板
     * 用于外部插件在切换计分板后调用
     */
    public void syncTeamsToPlayer(Player player) {
        if (player == null) return;

        Scoreboard targetBoard = player.getScoreboard();
        if (targetBoard == null) {
            targetBoard = mainScoreboard;
            player.setScoreboard(targetBoard);
        }

        // 获取玩家当前应有的前缀（pre1 + pre2 覆盖）
        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GroupConfig baseConfig = configManager.getGroupForPlayer(player);

        String prefix = baseConfig.prefix;
        String suffix = baseConfig.suffix;

        // 如果有覆盖层（pre2），覆盖
        if (coverPrefixes.containsKey(player.getUniqueId())) {
            prefix = coverPrefixes.get(player.getUniqueId());
        }
        if (coverSuffixes.containsKey(player.getUniqueId())) {
            suffix = coverSuffixes.get(player.getUniqueId());
        }

        // 清理长度
        prefix = sanitizePrefix(prefix);
        suffix = sanitizeSuffix(suffix);

        // 在目标计分板上应用队伍
        String teamName = "bnt_" + player.getUniqueId().toString().substring(0, 8);

        // 移除目标计分板上旧的同名队伍
        Team oldTeam = targetBoard.getTeam(teamName);
        if (oldTeam != null) {
            oldTeam.unregister();
        }

        // 创建新队伍
        Team team = targetBoard.registerNewTeam(teamName);
        team.setPrefix(prefix != null ? prefix : "");
        team.setSuffix(suffix != null ? suffix : "");
        team.addEntry(player.getName());
    }

    private void applyTeam(Player player, String prefix, String suffix) {
        // 1. 获取玩家当前使用的计分板
        Scoreboard targetBoard = player.getScoreboard();

        // 2. 如果玩家没有计分板（理论上不会），使用主计分板
        if (targetBoard == null) {
            targetBoard = mainScoreboard;
            player.setScoreboard(targetBoard);
        }

        // 3. 队伍名
        String teamName = "bnt_" + player.getUniqueId().toString().substring(0, 8);

        // 4. 从目标计分板移除玩家旧队伍
        for (Team team : targetBoard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }

        // 5. 在目标计分板上获取或创建队伍
        Team team = targetBoard.getTeam(teamName);
        if (team == null) {
            team = targetBoard.registerNewTeam(teamName);
        }

        // 6. 设置前缀和后缀
        team.setPrefix(prefix != null ? prefix : "");
        team.setSuffix(suffix != null ? suffix : "");
        team.addEntry(player.getName());

        // 7. 关键：确保玩家使用这个计分板
        // 如果玩家当前计分板不是目标计分板，才需要设置
        if (player.getScoreboard() != targetBoard) {
            player.setScoreboard(targetBoard);
        }
    }

    /**
     * 将主计分板上的所有 bnt_ 队伍同步到目标计分板
     */
    private void syncTeamsFromMain(Scoreboard targetBoard) {
        if (targetBoard == mainScoreboard) return;

        // 获取主计分板上所有 bnt_ 队伍
        Set<String> mainTeamNames = new HashSet<>();
        for (Team team : mainScoreboard.getTeams()) {
            if (team.getName().startsWith("bnt_")) {
                mainTeamNames.add(team.getName());
            }
        }

        // 删除目标计分板上多余的 bnt_ 队伍
        for (Team team : targetBoard.getTeams()) {
            if (team.getName().startsWith("bnt_") && !mainTeamNames.contains(team.getName())) {
                team.unregister();
            }
        }

        // 从主计分板复制队伍到目标计分板
        for (Team team : mainScoreboard.getTeams()) {
            if (team.getName().startsWith("bnt_")) {
                Team targetTeam = targetBoard.getTeam(team.getName());
                if (targetTeam == null) {
                    targetTeam = targetBoard.registerNewTeam(team.getName());
                }
                // 复制队伍属性
                targetTeam.setPrefix(team.getPrefix() != null ? team.getPrefix() : "");
                targetTeam.setSuffix(team.getSuffix() != null ? team.getSuffix() : "");
//                targetTeam.setColor(team.getColor());
                // 复制成员
                for (String entry : team.getEntries()) {
                    if (!targetTeam.hasEntry(entry)) {
                        targetTeam.addEntry(entry);
                    }
                }
            }
        }
    }

    private void removePlayerFromTeams(Player player) {
        for (Team team : mainScoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }

    public void cleanup() {
        for (Team team : mainScoreboard.getTeams()) {
            if (team.getName().startsWith("bnt_")) {
                team.unregister();
            }
        }
        activeTeamNames.clear();
    }

    public void reloadAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }
}