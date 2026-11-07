package top.cnolm.betterNameTag.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.cnolm.betterNameTag.BetterNameTag;
import top.cnolm.betterNameTag.ConfigManager;
import top.cnolm.betterNameTag.NameTagManager;

public class CommandHandler implements CommandExecutor {

    private final BetterNameTag plugin;
    private final NameTagManager nameTagManager;

    public CommandHandler(BetterNameTag plugin) {
        this.plugin = plugin;
        this.nameTagManager = plugin.getNameTagManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "coverprefix":
                return handleCoverPrefix(sender, args);
            case "coversuffix":
                return handleCoverSuffix(sender, args);
            case "removecover":
                return handleRemoveCover(sender, args);
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleCoverPrefix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bnt.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /bnt coverprefix <玩家名> <前缀>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[1]);
            return true;
        }

        String prefix = ChatColor.translateAlternateColorCodes('&', args[2]);
        nameTagManager.setCoverPrefix(target, prefix);
        sender.sendMessage(ChatColor.GREEN + "已为玩家 " + target.getName() + " 设置覆盖前缀: " + prefix);
        return true;
    }

    private boolean handleCoverSuffix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bnt.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /bnt coversuffix <玩家名> <后缀>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[1]);
            return true;
        }

        String suffix = ChatColor.translateAlternateColorCodes('&', args[2]);
        nameTagManager.setCoverSuffix(target, suffix);
        sender.sendMessage(ChatColor.GREEN + "已为玩家 " + target.getName() + " 设置覆盖后缀: " + suffix);
        return true;
    }

    private boolean handleRemoveCover(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bnt.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /bnt removecover <玩家名>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[1]);
            return true;
        }

        nameTagManager.removeCover(target);
        sender.sendMessage(ChatColor.GREEN + "已移除玩家 " + target.getName() + " 的覆盖层");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("bnt.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
            return true;
        }

        plugin.getConfigManager().reload();
        nameTagManager.reloadAllPlayers();
        sender.sendMessage(ChatColor.GREEN + "BetterNameTag 配置已重新加载！");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /bnt info <玩家名>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[1]);
            return true;
        }

        ConfigManager.GroupConfig base = plugin.getConfigManager().getGroupForPlayer(target);
        String coverPrefix = nameTagManager.getCoverPrefix(target);
        String coverSuffix = nameTagManager.getCoverSuffix(target);

        sender.sendMessage(ChatColor.GOLD + "=== " + target.getName() + " 的名称信息 ===");
        sender.sendMessage(ChatColor.GRAY + "基础前缀: " + base.prefix);
        sender.sendMessage(ChatColor.GRAY + "基础后缀: " + base.suffix);
        sender.sendMessage(ChatColor.GRAY + "覆盖前缀: " + (coverPrefix != null ? coverPrefix : ChatColor.RED + "无"));
        sender.sendMessage(ChatColor.GRAY + "覆盖后缀: " + (coverSuffix != null ? coverSuffix : ChatColor.RED + "无"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== BetterNameTag 命令帮助 ===");
        sender.sendMessage(ChatColor.GRAY + "/bnt coverprefix <玩家> <前缀> - 设置覆盖前缀");
        sender.sendMessage(ChatColor.GRAY + "/bnt coversuffix <玩家> <后缀> - 设置覆盖后缀");
        sender.sendMessage(ChatColor.GRAY + "/bnt removecover <玩家> - 移除覆盖层");
        sender.sendMessage(ChatColor.GRAY + "/bnt reload - 重新加载配置");
        sender.sendMessage(ChatColor.GRAY + "/bnt info <玩家> - 查看玩家名称信息");
    }
}