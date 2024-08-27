package github.daisukiKaffuChino.koharu.listener;

import github.daisukiKaffuChino.koharu.IConfig;
import github.daisukiKaffuChino.koharu.KoharuBan;
import github.daisukiKaffuChino.koharu.PluginConfig;
import github.daisukiKaffuChino.koharu.utils.LogUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerActionListener implements Listener, IConfig {
    private final KoharuBan plugin;
    private final Map<String, Material> bannedItems = new HashMap<>();
    private final LogUtil logUtil = new LogUtil();
    private String opUuid;

    public PlayerActionListener(JavaPlugin javaPlugin, FileConfiguration fileConfiguration) {
        this.plugin = (KoharuBan) javaPlugin;
        plugin.registerConfigReloadListener(this);
        loadConfig(fileConfiguration);
    }

    @Override
    public void onConfigReload() {
        //FileConfiguration fileConfiguration = plugin.getConfig();
        bannedItems.clear();
        loadConfig(plugin.getConfig());
    }

    private void loadConfig(FileConfiguration fileConfiguration) {
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
        //添加检测延迟：感谢某服热心玩家反馈bug（
        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.getPlayer() instanceof Player player) {
                    info(player.getName() + " 开了: " + event.getInventory().getType());
                    info("uuid: " + player.getUniqueId());
                    checkInventoryAndRemove(player);
                }
            }
        }.runTaskLater(plugin, 10);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) checkInventoryAndRemove(player);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (opUuid.equals(player.getUniqueId().toString())) return;
            ItemStack itemStack = event.getItem().getItemStack();
            if (bannedItems.containsValue(itemStack.getType())) {
                event.setCancelled(true);
                event.getItem().remove();
                logUtil.outputLogFile(player, itemStack.getType().name());
            }
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

            if (item != null)
                detectIllegalEnchantment(player, item);

        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String message = playerCommandPreprocessEvent.getMessage();
        Player player = playerCommandPreprocessEvent.getPlayer();
        if (opUuid.equals(player.getUniqueId().toString())) return;
        String[] split = message.split(" ");

        if (split.length >= 3 && split[0].equals("/give")) {

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

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (result != null && bannedItems.containsValue(result.getType())) {
            inventory.setResult(new ItemStack(Material.AIR));
            Player player = (Player) event.getView().getPlayer();
            Inventory playerInventory = player.getInventory();
            for (ItemStack item : inventory.getMatrix())
                if (item != null && item.getType() != Material.AIR) playerInventory.addItem(item);//加回去
            for (int i = 0; i < inventory.getMatrix().length + 1; i++)
                inventory.setItem(i, new ItemStack(Material.AIR));
            player.sendMessage(String.format("§l§9[KoharuBan] §c你不能合成%s，材料已返还！", result.getType()));
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

            if (itemStack != null)
                detectIllegalEnchantment(player, itemStack);

        }

    }

    //TODO 检查冲突附魔 例如无限和经验修补
    //TODO 更好的检查过量附魔方法
    private void detectIllegalEnchantment(Player player, ItemStack item) {
        //boolean hasDetected = false;
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            Map<Enchantment, Integer> enchantments = item.getEnchantments();

            //找到更好的方法之前， Koharu 将不再检查附魔数量
            //虽然根据知乎高赞回答“铁砧的话，剑最多可以附魔六个，当然每个都是满级魔咒。弓是五个，工具是四个...”，可以把超过 6 个的附魔全部判定为非法，但是假如当 *byd* 玩家们给剑附上了保护时，这一规则就会发生误判
            /*if (enchantments.size() > 6) {
                Set<Enchantment> keySet = enchantments.keySet();
                keySet.forEach(item::removeEnchantment);
                plugin.getLogger().warning("移除过量附魔，数量：" + enchantments.size());
                return true;//不需要继续遍历了
            }*/

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();
                if (level > enchantment.getMaxLevel()) {
                    item.removeEnchantment(enchantment); //只移除超最大等级的那个附魔，而非全部
                    player.sendMessage("§l§9[KoharuBan] §c非法附魔已移除，你的行为将被记录！");
                    String msg = String.format("清除附魔：%s 等级：%s", enchantment.getKey().getKey(), level);
                    logUtil.outputLogFile(player, item.getType().name() + msg);
                    plugin.getLogger().warning(msg);
                    //hasDetected = true;
                }
            }

        }
        //return hasDetected;
    }

    private void info(String str) {
        if (PluginConfig.isDebuggable) plugin.getLogger().info(str);
    }
}
