package vitalplugins.vitalplugins_backups.gui;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import vitalplugins.vitalplugins_backups.Database;
import vitalplugins.vitalplugins_backups.backup.BackupGive;
import vitalplugins.vitalplugins_backups.utils.Chat;
import vitalplugins.vitalplugins_backups.utils.ItemHelper;
import vitalplugins.vitalplugins_backups.utils.Helper;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.bukkit.Material.*;

public class BackupInformationGui implements Listener {

    private static String backupGuiName;
    private static UUID backupID;
    private static Player backupPlayer;

    private final JavaPlugin plugin;
    private final Database database;
    private final Helper helper;
    private final ItemHelper itemHelper;
    private final BackupGive backupGive;

    public BackupInformationGui(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.helper = new Helper(plugin);
        this.itemHelper = new ItemHelper();
        this.backupGive = new BackupGive(plugin, database);
    }

    public void open(Player player, Player bPlayer, UUID bID) {
        String guiTitle = Chat.color("&8Backup gracza: " + helper.getColor() + bPlayer.getName());
        Inventory gui = Bukkit.createInventory(player, 54, guiTitle);

        backupGuiName = guiTitle;
        backupID = bID;
        backupPlayer = bPlayer;

        String encodedInventory = database.getInventoryJson(bID.toString());

        ItemStack[] inventoryContents = deserializeInventory(encodedInventory);

        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = inventoryContents[i];

            for (int j = 36; j <= 40; j++) {
                if (i == j) {
                    gui.setItem(54 - j - 14, item);
                }
            }

            for (int j = 9; j <= 35; j++) {
                if (i == j) {
                    gui.setItem(j + 9, item);
                }
            }

            for (int j = 0; j <= 8; j++) {
                if (i == j) {
                    gui.setItem(j + 45, item);
                }
            }

            for (int j = 9; j <= 17; j++) {
                if (i == j) {
                    gui.setItem(j, itemHelper.createItem(BLACK_STAINED_GLASS_PANE, Chat.color("&7by VitalPlugins"), ""));
                }
            }
        }

        gui.setItem(5, itemHelper.createItem(BLACK_STAINED_GLASS_PANE, Chat.color("&7by VitalPlugins"), ""));
        gui.setItem(6, itemHelper.createItem(LIME_DYE, Chat.color("&a&lODDAJ"), ""));
        gui.setItem(7, itemHelper.createItem(RED_DYE, Chat.color("&c&lUSUŃ"), ""));
        gui.setItem(8, itemHelper.createItem(OAK_FENCE_GATE, Chat.color("&f&lPOWRÓT"), ""));

        player.openInventory(gui);
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !event.getView().getTopInventory().equals(clickedInventory)) {
            return;
        }

        if (!event.getView().getTitle().equals(Chat.color(backupGuiName))) {
            return;
        }

        if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
            event.setCancelled(true);
            return;
        }

        Player whoClicked = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta clickedItemMeta = clickedItem.getItemMeta();

        if (clickedItemMeta.getDisplayName().equals(Chat.color("&a&lODDAJ"))) {
            try {
                backupGive.give(backupPlayer, backupID);
            } finally {
                whoClicked.sendTitle(Chat.color(helper.getColor() + "&lBACKUP"), Chat.color("&fOddałeś backup dla gracza " + Chat.color(helper.getColor() + backupPlayer.getName())), 10, 100, 10);
                backupPlayer.sendTitle(Chat.color(helper.getColor() + "&lBACKUP"),
                        Chat.color("&fOtrzymałeś backupa od administratora"),
                        10, 100, 10);
                whoClicked.closeInventory();
            }
        } else if (clickedItemMeta.getDisplayName().equals(Chat.color("&c&lUSUŃ"))) {
            try {
                database.delete(String.valueOf(backupID));
            } finally {
                whoClicked.sendTitle(Chat.color(helper.getColor() + "&lBACKUP"), Chat.color("&fUsunąłeś backup gracza " + Chat.color(helper.getColor() + backupPlayer.getName())), 10, 100, 10);
                whoClicked.closeInventory();
            }
        } else if (clickedItemMeta.getDisplayName().equals(Chat.color("&f&lPOWRÓT"))) {
            new MainGui(plugin, database).open(whoClicked, backupPlayer, 0);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void inventoryDrag(InventoryDragEvent event) {
        Inventory clickedInventory = event.getInventory();
        if (clickedInventory == null || !event.getView().getTopInventory().equals(clickedInventory)) {
            return;
        }

        if (!event.getView().getTitle().equals(Chat.color(backupGuiName))) {
            return;
        }

        event.setCancelled(true);
    }

    @SneakyThrows
    private ItemStack[] deserializeInventory(String data) {
        byte[] decodedData = Base64.getDecoder().decode(data);

        ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedData);
        BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(byteStream);

        return (ItemStack[]) bukkitIn.readObject();

    }

}
