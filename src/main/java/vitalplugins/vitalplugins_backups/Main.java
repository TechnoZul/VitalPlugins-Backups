package vitalplugins.vitalplugins_backups;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.plugin.java.JavaPlugin;
import vitalplugins.vitalplugins_backups.backup.BackupSave;
import vitalplugins.vitalplugins_backups.commands.BackupCommand;
import vitalplugins.vitalplugins_backups.gui.BackupInformationGui;
import vitalplugins.vitalplugins_backups.gui.MainGui;

public final class Main extends JavaPlugin {

    private Database database;
    private JDA jda;

    @SneakyThrows
    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        database = new Database(this);
        database.createDatabase();

        jda = JDABuilder.createDefault(getConfig().getString("bot-token"))
                .setActivity(Activity.listening("Plugin by VitalyPlugins"))
                .addEventListeners(new DiscordListener(this, database))
                .build().awaitReady();

        Guild server = jda.getGuildById(getConfig().getString("guild-id"));
        if (server != null) {
            server.upsertCommand("backup", "Plugin na backupy")
                    .addOptions(new OptionData(OptionType.STRING, "nick", "Nick gracza", true))
                    .queue();
        } else {
            getLogger().severe("Nie znaleziono serwera o  danym id: " + getConfig().getString("guild-id"));
        }

        getServer().getPluginManager().registerEvents(new BackupSave(this, database), this);
        getServer().getPluginManager().registerEvents(new MainGui(this, database), this);
        getServer().getPluginManager().registerEvents(new BackupInformationGui(this, database), this);
        getCommand("backup").setExecutor(new BackupCommand(this, database));
    }

    @SneakyThrows
    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdownNow();
            jda.awaitShutdown();
        }
    }
}
