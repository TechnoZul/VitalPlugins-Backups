package vitalplugins.vitalplugins_backups.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vitalplugins.vitalplugins_backups.Database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BackupSave implements Listener {

    private final JavaPlugin plugin;
    private final Database database;
    private final Gson gson;

    public BackupSave(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.gson = new GsonBuilder().create();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        String killerName = (killer != null) ? killer.getName() : "Nieznany";
        java.util.Date deathTimeUtil = new java.util.Date();
        Date deathTime = new Date(deathTimeUtil.getTime());
        String playerPing = String.valueOf(player.getPing());
        String backupID = UUID.randomUUID().toString();

        ItemStack[] playerInventory = player.getInventory().getContents();

        String encodedInventory = serializeInventory(playerInventory);

        CompletableFuture.runAsync(() -> {
            try {
                database.insert(playerName, playerUUID, killerName, deathTime, playerPing, backupID, encodedInventory);
            } catch (SQLException e) {
                e.printStackTrace();
                plugin.getServer().getLogger().severe("Błąd podczas zapisywania danych do bazy danych: " + e.getMessage());
            }
        });
    }

    private String serializeInventory(ItemStack[] inventory) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(byteStream)) {

            bukkitOut.writeObject(inventory);
            bukkitOut.flush();

            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
