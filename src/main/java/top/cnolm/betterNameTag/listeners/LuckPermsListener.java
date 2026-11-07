package top.cnolm.betterNameTag.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import top.cnolm.betterNameTag.BetterNameTag;

public class LuckPermsListener implements Listener {

    private final BetterNameTag plugin;

    public LuckPermsListener(BetterNameTag plugin) {
        this.plugin = plugin;
        registerLuckPermsEvent();
    }

    private void registerLuckPermsEvent() {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            EventBus eventBus = luckPerms.getEventBus();

            // 注册 UserDataRecalculateEvent 事件
            eventBus.subscribe(plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);

            plugin.getLogger().info("已注册 LuckPerms 事件监听！");
        } catch (Exception e) {
            plugin.getLogger().warning("无法注册 LuckPerms 事件，权限变更后名称可能不会自动更新。");
            plugin.getLogger().warning("错误信息: " + e.getMessage());
        }
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        // 当玩家的权限数据重新计算时触发
        String playerName = event.getUser().getFriendlyName();
        Player player = Bukkit.getPlayer(playerName);

        if (player != null && player.isOnline()) {
            // 延迟1 tick确保权限已经完全更新
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getNameTagManager().updatePlayer(player);
            }, 1L);
        }
    }
}