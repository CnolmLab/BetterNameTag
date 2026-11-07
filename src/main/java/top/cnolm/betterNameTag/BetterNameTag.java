package top.cnolm.betterNameTag;

import org.bukkit.plugin.java.JavaPlugin;
import top.cnolm.betterNameTag.api.NameTagProvider;
import top.cnolm.betterNameTag.commands.CommandHandler;
import top.cnolm.betterNameTag.listeners.LuckPermsListener;
import top.cnolm.betterNameTag.listeners.PlayerListener;

public class BetterNameTag extends JavaPlugin {

    private static BetterNameTag instance;
    private ConfigManager configManager;
    private NameTagManager nameTagManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // 初始化管理器
        this.nameTagManager = new NameTagManager(this);

        // 注册事件监听
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 注册命令
        getCommand("bnt").setExecutor(new CommandHandler(this));

        // ✅ 注册 API
        NameTagProvider.register(this);

        // 注册 LuckPerms 事件监听（权限变更自动更新）
        new LuckPermsListener(this);

        // 为所有已在线玩家初始化
        getServer().getOnlinePlayers().forEach(nameTagManager::updatePlayer);

        getLogger().info("BetterNameTag 已启用！");
    }

    @Override
    public void onDisable() {
        // ✅ 注销 API
        NameTagProvider.unregister();
        // 清理所有队伍
        if (nameTagManager != null) {
            nameTagManager.cleanup();
        }
        getLogger().info("BetterNameTag 已禁用！");
    }

    public static BetterNameTag getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NameTagManager getNameTagManager() {
        return nameTagManager;
    }
}