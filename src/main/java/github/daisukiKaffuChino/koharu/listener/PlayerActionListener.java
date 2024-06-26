package github.daisukiKaffuChino.koharu.listener;

import github.daisukiKaffuChino.koharu.PluginConfig;
import github.daisukiKaffuChino.koharu.utils.LogUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerActionListener implements Listener {
    private final JavaPlugin plugin;
    private final Map<String, Material> bannedItems = new HashMap<>();
    private final LogUtil logUtil = new LogUtil();
    private String opUuid;

    public PlayerActionListener(JavaPlugin javaPlugin, FileConfiguration fileConfiguration) {
        this.plugin = javaPlugin;
        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection("Options");

        if (configurationSection != null) {
            List<?> list = configurationSection.getList("bannedItem");
            this.opUuid = configurationSection.getString("SuperOP");

            assert list != null;
            for (Object _key : list) {
                String key = (String) _key;
                this.bannedItems.put(key, Material.valueOf(key));
            }
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Player player) {
            info(player.getName() + " 按了 " + event.getCurrentItem());
            checkInventoryAndRemove(player);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            info(player.getName() + " 开了: " + event.getInventory().getType());
            info("uuid: " + player.getUniqueId());
            checkInventoryAndRemove(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) checkInventoryAndRemove(player);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        Player entity = (Player) event.getEntity();
        if (opUuid.equals(entity.getUniqueId().toString())) return;
        ItemStack itemStack = event.getItem().getItemStack();
        if (this.bannedItems.containsValue(itemStack.getType())) {
            event.setCancelled(true);
            event.getItem().remove();
            logUtil.outputLogFile(entity, itemStack.getType().name());
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (!opUuid.equals(player.getUniqueId().toString())) {
            if (item != null && this.bannedItems.containsValue(item.getType())) {
                player.getInventory().setItem(event.getNewSlot(), new ItemStack(Material.AIR));
                logUtil.outputLogFile(player, item.getType().name());
            }
            if (item != null) {
                Map<Enchantment, Integer> enchantments = item.getEnchantments();
                boolean hasDetected = false;
                for (Enchantment enchantment : enchantments.keySet()) {
                    if (enchantments.size() > 5 || hasIllegalEnchantment(enchantments, enchantment.getMaxLevel())) {
                        Set<Enchantment> keySet = item.getEnchantments().keySet();
                        keySet.forEach(item::removeEnchantment);
                        hasDetected = true;
                    }
                }

                if (hasDetected) {
                    player.sendMessage("§l§9[KoharuBan] §c非法附魔已移除，你的行为将被记录！");
                    this.logUtil.outputLogFile(player, item.getType().name() + " - 非法附魔");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String message = playerCommandPreprocessEvent.getMessage();
        Player player = playerCommandPreprocessEvent.getPlayer();
        if (opUuid.equals(player.getUniqueId().toString())) return;
        String[] split = message.split(" ");
        if (split.length >= 3) {

            try {
                String substring = split[2].toUpperCase().substring(10);//能跑就行
                //plugin.getLogger().warning(substring);
                try {
                    if (bannedItems.containsValue(Material.valueOf(substring))) {
                        playerCommandPreprocessEvent.setCancelled(true);
                        plugin.getLogger().warning(player.getName() + "存在滥权行为");
                        logUtil.outputLogFile(playerCommandPreprocessEvent.getPlayer(), substring);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(e.getMessage());
                }
            } catch (StringIndexOutOfBoundsException e) {
                plugin.getLogger().warning("随便敲个命令都能报错，建议严查（");
            }

        }
    }

    private void checkInventoryAndRemove(Player player) {
        if (opUuid.equals(player.getUniqueId().toString())) return;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack itemStack = contents[i];
            if (itemStack != null && this.bannedItems.containsValue(itemStack.getType())) {
                player.getInventory().clear(i);
                this.logUtil.outputLogFile(player, itemStack.getType().name());
            }
        }

    }

    private boolean hasIllegalEnchantment(Map<Enchantment, Integer> map, int i) {
        for (Integer integer : map.values())
            if (integer > i) return true;
        return false;
    }

    private void info(String str) {
        if (PluginConfig.isDebuggable) plugin.getLogger().info(str);
    }
}
