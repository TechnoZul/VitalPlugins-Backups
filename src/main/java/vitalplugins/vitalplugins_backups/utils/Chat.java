package vitalplugins.vitalplugins_backups.utils;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class Chat {

    public static String color(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&',
                message.replace(">>", "»").replace(">", "›"));
    }

    public static List<String> colorList(List<String> messages) {
        return messages.stream()
                .map(Chat::color)
                .collect(Collectors.toList());
    }
}
