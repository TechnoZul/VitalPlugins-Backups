package vitalplugins.vitalplugins_backups.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import vitalplugins.vitalplugins_backups.Database;
import vitalplugins.vitalplugins_backups.utils.Chat;
import vitalplugins.vitalplugins_backups.utils.Helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class BackupGive {

    private final JavaPlugin plugin;
    private final Database database;
    private final Gson gson;
    private final Chat chat;
    private final Helper helper;

    public BackupGive(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.gson = new GsonBuilder().create();
        this.chat = new Chat();
        this.helper = new Helper(plugin);
    }

    public void give(Player player, UUID backupID) {
        String encodedInventory = database.getInventoryJson(backupID.toString());

        ItemStack[] inventoryContents = deserializeInventory(encodedInventory);

        player.getInventory().setContents(inventoryContents);
    }

    private ItemStack[] deserializeInventory(String data) {
        byte[] decodedData = Base64.getDecoder().decode(data);

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedData);
             BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(byteStream)) {

            return (ItemStack[]) bukkitIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
