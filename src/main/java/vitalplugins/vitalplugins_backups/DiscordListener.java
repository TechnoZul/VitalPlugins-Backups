package vitalplugins.vitalplugins_backups;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import vitalplugins.vitalplugins_backups.backup.BackupGive;
import vitalplugins.vitalplugins_backups.utils.Chat;
import vitalplugins.vitalplugins_backups.utils.Helper;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;

public class DiscordListener extends ListenerAdapter {

    private Player backupPlayer;
    private String backupKiller;
    private String backupDeathTime;
    private String backupPing;
    private UUID backupID;

    private final JavaPlugin plugin;
    private final Database database;
    private final BackupGive backupGive;
    private final Helper helper;
    private final Map<UUID, Boolean> backupProcessedMap = new HashMap<>();

    public DiscordListener(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.backupGive = new BackupGive(plugin, database);
        this.helper = new Helper(plugin);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("backup")) {

            OptionMapping option = event.getOption("nick");
            String nick = option != null ? option.getAsString() : null;

            Player player = Bukkit.getPlayer(nick);

            backupPlayer = player;

            if (player == null) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription("> Gracz **" + nick + "** jest offline")
                        .setFooter("•     by VitalPlugins")
                        .setColor(Color.decode("#90abe9"));
                event.replyEmbeds(embedBuilder.build()).queue();
                return;
            }

            List<String> backupIDs = database.getBackupInfo("uuid", "backup_id", String.valueOf(player.getUniqueId()));

            if (backupIDs.isEmpty()) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription("> Gracz **" + nick + "** nie ma dostępnych backapów")
                        .setFooter("•     by VitalPlugins")
                        .setColor(Color.decode("#90abe9"));
                event.replyEmbeds(embedBuilder.build()).queue();
                return;
            }

            if (backupIDs.size() > 25) {
                backupIDs = backupIDs.subList(backupIDs.size() - 25, backupIDs.size());
            }

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setThumbnail("https://mineskin.eu/avatar/" + nick)
                    .setTitle("📋  •  Lista backapów gracza: " + nick)
                    .setDescription(
                            "> Wybierz backup z listy poniżej,\n" +
                                    "> oraz zdecyduj czy chcesz go oddać czy anulować\n" +
                                    "> przy pomocy przycisków poniżej"
                    )
                    .setFooter("•     by VitalPlugins")
                    .setColor(Color.decode("#90abe9"));

            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("choose-backup");

            for (String backupID : backupIDs) {
                String playerDeathTime = database.getBackupInfo("backup_id", "death_time", backupID).toString();
                String playerPing = database.getBackupInfo("backup_id", "ping", backupID).toString();

                backupKiller = database.getBackupInfo("backup_id", "killed_by", backupID).toString();
                backupDeathTime = database.getBackupInfo("backup_id", "death_time", backupID).toString();
                backupPing = playerPing;

                menuBuilder.addOption(backupID,
                        backupID,
                        helper.cleanString(playerDeathTime) + " | Ping: " + helper.cleanString(playerPing),
                        Emoji.fromFormatted("💀"));

            }

            StringSelectMenu backupMenu = menuBuilder.build();

            event.replyEmbeds(embedBuilder.build())
                    .addActionRow(backupMenu)
                    .queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("choose-backup")) {

            backupID = UUID.fromString(event.getValues().get(0));

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("🔍  •  Informacje o backupie")
                    .setDescription(
                            "> ID: " + helper.cleanString(String.valueOf(backupID)) + "\n" +
                                    "> Nick gracza: " + helper.cleanString(backupPlayer.getName()) + "\n" +
                                    "> Nick zabójcy: " + helper.cleanString(backupKiller) + "\n" +
                                    "> Ping gracza: " + helper.cleanString(backupPing) + "\n" +
                                    "> Data utworzenia: " + helper.cleanString(backupDeathTime)
                    )
                    .setFooter("•     by VitalPlugins")
                    .setColor(Color.decode("#90abe9"));

            event.replyEmbeds(embedBuilder.build())
                    .addActionRow(
                            Button.success("give-backup", "Przywróć backup"),
                            Button.danger("destruct-backup", "Anuluj backup"))
                    .queue();
        }
    }

    @SneakyThrows
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("give-backup")) {
            event.deferReply().queue();

            if (backupProcessedMap.getOrDefault(backupID, false)) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription("> Backup został już oddany lub anulowany.")
                        .setFooter("•     by VitalPlugins")
                        .setColor(Color.RED);
                event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
                return;
            }

            backupGive.give(backupPlayer, backupID);
            backupProcessedMap.put(backupID, true);
            backupPlayer.sendTitle(Chat.color(helper.getColor() + "&lBACKUP"),
                    Chat.color("&fOtrzymałeś backupa od discord bota"),
                    10, 100, 10);

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setDescription("> Pomyślnie oddano backup dla gracza: **" + backupPlayer.getName() + "**")
                    .setFooter("•     by VitalPlugins")
                    .setColor(Color.decode("#85e962"));

            event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();

        } else if (event.getComponentId().equals("destruct-backup")) {
            event.deferReply().queue();

            if (backupProcessedMap.getOrDefault(backupID, false)) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription("> Backup został już oddany lub anulowany.")
                        .setFooter("•     by VitalPlugins")
                        .setColor(Color.RED);
                event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
                return;
            }

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setDescription("> Anulowano oddawanie backupa dla gracza: **" + backupPlayer.getName() + "**")
                    .setFooter("•     by VitalPlugins")
                    .setColor(Color.RED);

            event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();

            backupProcessedMap.put(backupID, true);
        }

    }

    @SneakyThrows
    private ItemStack[] deserializeInventory(String data) {
        byte[] decodedData = Base64.getDecoder().decode(data);

        ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedData);
        BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(byteStream);

        return (ItemStack[]) bukkitIn.readObject();
    }

}
