package vitalplugins.vitalplugins_backups.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Helper {

    private JavaPlugin plugin;
    private FileConfiguration pluginConfig;

    public Helper(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getConfig();
    }

    public String getColor() {
        return pluginConfig.getString("plugin-color");
    }

    public String getHost() {
        return pluginConfig.getString("mysql.host");
    }

    public String getPort() {
        return pluginConfig.getString("mysql.port");
    }

    public String getName() {
        return pluginConfig.getString("mysql.name");
    }

    public String getUser() {
        return pluginConfig.getString("mysql.user");
    }

    public String getPassword() {
        return pluginConfig.getString("mysql.password");
    }

    public String getUrl() {
        return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getName();
    }

    public String getPermission() {
        return pluginConfig.getString("permission");
    }

    public String getPermissionMessage() {
        return pluginConfig.getString("messages.permission");
    }

    public String getOfflineMessage() {
        return pluginConfig.getString("messages.offline-player");
    }

    public String getNoBackup() {
        return pluginConfig.getString("messages.no-backup");
    }

    public String cleanString(String input) {
        return input == null ? "" : input.replace("[", "").replace("]", "");
    }
}
