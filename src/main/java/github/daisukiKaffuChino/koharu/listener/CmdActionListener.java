package github.daisukiKaffuChino.koharu.listener;

import github.daisukiKaffuChino.koharu.KoharuBan;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CmdActionListener implements CommandExecutor {
    private final JavaPlugin plugin;

    public CmdActionListener(JavaPlugin javaPlugin) {
        this.plugin = javaPlugin;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String str, @NotNull String[] strArr) {
        if (!str.equalsIgnoreCase("koharu")) return false;

        if (strArr[0].equalsIgnoreCase("reload") && strArr.length == 1) {
            if (commandSender instanceof ConsoleCommandSender) {
                plugin.reloadConfig();
                KoharuBan koharuBan = (KoharuBan) plugin;
                koharuBan.notifyConfigReloadListeners();
                commandSender.sendMessage("KoharuBan 配置已重载");
            } else commandSender.sendMessage("只允许控制台使用");

            return true;
        }

        if (strArr[0].equalsIgnoreCase("key")) {
            Player player = (Player) commandSender;
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            player.sendMessage("查询物品的附魔key：");
            for (Enchantment enchantment : itemInMainHand.getEnchantments().keySet())
                player.sendMessage("[Key] " + enchantment.getKey());
            return true;
        }

        commandSender.sendMessage("命令没有执行");
        return true;
    }

    public static class KoharuCmdTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (command.getName().equalsIgnoreCase("koharu")) {
                if (args.length == 1) {
                    completions.add("reload");
                    completions.add("key");
                }
            }
            return completions;
        }
    }
}
