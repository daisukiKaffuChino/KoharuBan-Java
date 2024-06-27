package github.daisukiKaffuChino.koharu;

import github.daisukiKaffuChino.koharu.listener.CmdActionListener;
import github.daisukiKaffuChino.koharu.listener.PlayerActionListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class KoharuBan extends JavaPlugin {

    @Override
    public void onLoad() {
        getLogger().info("[KoharuBan-Java] 启动！");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new PlayerActionListener(KoharuBan.this, getConfig()), this);
        Objects.requireNonNull(getCommand("koharu")).setExecutor(new CmdActionListener(KoharuBan.this));
        Objects.requireNonNull(getCommand("koharu")).setTabCompleter(new CmdActionListener.KoharuCmdTabCompleter());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
