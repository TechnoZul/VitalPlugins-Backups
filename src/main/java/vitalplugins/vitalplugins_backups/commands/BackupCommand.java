package vitalplugins.vitalplugins_backups.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vitalplugins.vitalplugins_backups.Database;
import vitalplugins.vitalplugins_backups.gui.MainGui;
import vitalplugins.vitalplugins_backups.utils.Helper;
import vitalplugins.vitalplugins_backups.utils.Chat;

import java.util.List;

public class BackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Database database;
    private final Helper helper;
    private final MainGui mainGui;

    public BackupCommand(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.helper = new Helper(plugin);
        this.mainGui = new MainGui(plugin, database);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracze mogą używać tej komendy");
            return true;
        }

        if (!sender.hasPermission(helper.getPermission())) {
            sender.sendMessage(Chat.color(helper.getPermissionMessage().replace("{permission}", helper.getPermission())));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Chat.color("&cPoprawne użycie: /backup <gracz>"));
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);

        List<String> backupExist = database.getBackupInfo("uuid", "nickname", String.valueOf(player.getUniqueId()));

        if (backupExist.isEmpty()) {
            sender.sendMessage(Chat.color(helper.getNoBackup()));
            return true;
        }

        if (player == null) {
            sender.sendMessage(Chat.color(helper.getOfflineMessage()));
            return true;
        }

        mainGui.open((Player) sender, player, 0);

        return true;
    }
}
