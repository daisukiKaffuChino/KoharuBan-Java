package github.daisukiKaffuChino.koharu.listener;

import github.daisukiKaffuChino.koharu.PluginConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
            if (PluginConfig.isDebuggable) {
                this.plugin.reloadConfig();
                commandSender.sendMessage("KoharuBan 配置已重载");
            } else commandSender.sendMessage("未处于调试模式");
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
