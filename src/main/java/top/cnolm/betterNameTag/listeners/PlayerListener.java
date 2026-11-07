package top.cnolm.betterNameTag.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.cnolm.betterNameTag.BetterNameTag;
import top.cnolm.betterNameTag.NameTagManager;

public class PlayerListener implements Listener {

    private final BetterNameTag plugin;
    private final NameTagManager nameTagManager;

    public PlayerListener(BetterNameTag plugin) {
        this.plugin = plugin;
        this.nameTagManager = plugin.getNameTagManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 延迟1tick确保玩家完全加入
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            nameTagManager.updatePlayer(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // 切换世界后重新应用队伍
        nameTagManager.updatePlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 不需要特殊处理，队伍会保留，但玩家离线后队伍会自动失效
    }
}