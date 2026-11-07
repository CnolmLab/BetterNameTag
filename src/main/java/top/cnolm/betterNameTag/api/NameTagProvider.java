package top.cnolm.betterNameTag.api;

import top.cnolm.betterNameTag.BetterNameTag;

/**
 * BetterNameTag API 的入口类
 * 其他插件通过此类获取 API 实例
 */
public class NameTagProvider {

    private static BetterNameTagAPI api;

    /**
     * 获取 BetterNameTag API 实例
     *
     * @return BetterNameTagAPI 实例
     * @throws IllegalStateException 如果 API 尚未初始化
     */
    public static BetterNameTagAPI getAPI() {
        if (api == null) {
            throw new IllegalStateException("BetterNameTag API 尚未初始化！请确保 BetterNameTag 已正确加载。");
        }
        return api;
    }

    /**
     * 注册 API（由 BetterNameTag 插件在 onEnable 时调用）
     *
     * @param plugin BetterNameTag 插件实例
     */
    public static void register(BetterNameTag plugin) {
        if (api == null) {
            api = new BetterNameTagAPI(plugin);
            plugin.getLogger().info("BetterNameTag API 已注册！");
        }
    }

    /**
     * 注销 API（由 BetterNameTag 插件在 onDisable 时调用）
     */
    public static void unregister() {
        api = null;
    }

    /**
     * 检查 API 是否可用
     *
     * @return true 如果 API 已注册
     */
    public static boolean isAvailable() {
        return api != null;
    }
}