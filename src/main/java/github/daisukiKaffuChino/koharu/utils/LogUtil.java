package github.daisukiKaffuChino.koharu.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.bukkit.entity.Player;

public class LogUtil {

    public void outputLogFile(Player player, String str) {
        player.sendMessage("§l§9[KoharuBan] §c已清除违规物品");

        String format = String.format("[%s] [%s] %s%n", getTimeString("yyyy-MM-dd HH:mm:ss"), player.getName(), str);
        String fileName = "logs/KoharuBan/" + getTimeString("yyyy-MM-dd") + ".txt";
        try {
            if (!new File("logs/KoharuBan/").exists()) {
                new File("logs/KoharuBan/").mkdirs();
            }
            Files.write(Paths.get(fileName), format.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private String getTimeString(String str) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(str);
        return simpleDateFormat.format(timestamp);
    }
}

