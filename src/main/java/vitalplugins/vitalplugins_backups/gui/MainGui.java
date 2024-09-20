package vitalplugins.vitalplugins_backups.gui;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import vitalplugins.vitalplugins_backups.Database;
import vitalplugins.vitalplugins_backups.backup.BackupGive;
import vitalplugins.vitalplugins_backups.utils.Chat;
import vitalplugins.vitalplugins_backups.utils.ItemHelper;
import vitalplugins.vitalplugins_backups.utils.Helper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MainGui implements Listener {
    private final JavaPlugin plugin;
    private final Database database;
    private final Helper helper;
    private final ItemHelper itemHelper;
    private final BackupGive backupGive;
    private final BackupInformationGui backupInformationGui;

    private static final int INVENTORY_SIZE = 54;
    private static final int BACKUPS_PER_PAGE = 45;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;

    public MainGui(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.helper = new Helper(plugin);
        this.itemHelper = new ItemHelper();
        this.backupGive = new BackupGive(plugin, database);
        this.backupInformationGui = new BackupInformationGui(plugin, database);
    }

    public void open(Player sender, Player player, int page) {
        CompletableFuture.runAsync(() -> {
            List<String> backupIDs = database.getBackupInfo("uuid", "backup_id", String.valueOf(player.getUniqueId()));
            Map<String, BackupInfo> backupMap = new HashMap<>();

            for (String backupID : backupIDs) {
                String playerName = database.getBackupInfo("backup_id", "nickname", backupID).toString();
                String playerUUID = player.getUniqueId().toString();
                String playerKiller = database.getBackupInfo("backup_id", "killed_by", backupID).toString();
                String playerDeathTime = database.getBackupInfo("backup_id", "death_time", backupID).toString();
                String playerPing = database.getBackupInfo("backup_id", "ping", backupID).toString();

                backupMap.put(backupID, new BackupInfo(playerName, playerUUID, playerKiller, playerDeathTime, playerPing));
            }

            List<Map.Entry<String, BackupInfo>> sortedBackups = backupMap.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getValue().deathTime))
                    .collect(Collectors.toList());

            int startIndex = page * BACKUPS_PER_PAGE;
            int endIndex = Math.min(startIndex + BACKUPS_PER_PAGE, sortedBackups.size());

            if (startIndex >= sortedBackups.size()) {
                int lastPage = Math.max(0, (sortedBackups.size() - 1) / BACKUPS_PER_PAGE);
                startIndex = lastPage * BACKUPS_PER_PAGE;
                endIndex = Math.min(startIndex + BACKUPS_PER_PAGE, sortedBackups.size());
            }

            final int start = startIndex;
            final int end = endIndex;

            Inventory gui = Bukkit.createInventory(sender, INVENTORY_SIZE, Chat.color("&8Zapisane backupy (" + (page + 1) + ")"));

            for (int index = start; index < end; index++) {
                Map.Entry<String, BackupInfo> entry = sortedBackups.get(index);
                String backupID = entry.getKey();
                BackupInfo info = entry.getValue();

                String backupName = String.format("&7ID zapisu: %s%s", helper.getColor(), backupID);
                String playerName = String.format("%s%s", helper.getColor(), helper.cleanString(info.playerName));
                String playerUUID = String.format("%s%s", helper.getColor(), helper.cleanString(info.playerUUID));
                String playerKiller = String.format("%s%s", helper.getColor(), helper.cleanString(info.playerKiller));
                String playerDeathTime = String.format("%s%s", helper.getColor(), helper.cleanString(info.deathTime));
                String playerPing = String.format("%s%s", helper.getColor(), helper.cleanString(info.playerPing));

                String LPM = String.format("%sLPM", helper.getColor());
                String PPM = String.format("%sPPM", helper.getColor());

                List<String> lore = Arrays.asList(
                        "",
                        "&7Nick gracza: " + playerName,
                        "&7UUID gracza: " + playerUUID,
                        "&7Nick zabójcy: " + playerKiller,
                        "&7Ping gracza: " + playerPing,
                        "&7Data: " + playerDeathTime,
                        "",
                        "&8>> &7Kliknij {LPM} &7aby oddać backup".replace("{LPM}", LPM),
                        "&8>> &7Kliknij {PPM} &7aby zobaczyć podgląd".replace("{PPM}", PPM)
                );

                ItemStack backup = itemHelper.createItem(Material.CHEST, backupName, lore);
                gui.setItem(index % BACKUPS_PER_PAGE, backup);
            }

            gui.setItem(PREVIOUS_PAGE_SLOT, page > 0 ? itemHelper.createItem(Material.PAPER, Chat.color("&7Poprzednia strona"), Collections.singletonList(Chat.color("&8>> &7Kliknij aby zobaczyć poprzednią stronę"))) : createDisabledItem("&cBrak poprzedniej strony"));
            gui.setItem(NEXT_PAGE_SLOT, end < sortedBackups.size() ? itemHelper.createItem(Material.PAPER, Chat.color("&7Następna strona"), Collections.singletonList(Chat.color("&8>> &7Kliknij aby zobaczyć następną stronę"))) : createDisabledItem("&cBrak następnej strony"));

            for (int i = 46; i < 53; i++) {
                gui.setItem(i, itemHelper.createItem(Material.BLACK_STAINED_GLASS_PANE, Chat.color("&7by VitalPlugins"), ""));
            }

            Bukkit.getScheduler().runTask(plugin, () -> sender.openInventory(gui));

        });
    }

    private ItemStack createDisabledItem(String name) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Chat.color(name));
        item.setItemMeta(meta);
        return item;
    }

    @SneakyThrows
    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !event.getView().getTopInventory().equals(clickedInventory)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.startsWith(Chat.color("&8Zapisane backupy"))) {
            return;
        }

        if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick() || clickedInventory.equals(event.getInventory())) {
            event.setCancelled(true);
            return;
        }

        Player whoClicked = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasLore()) {
            return;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (clickedItem.getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }

        if (clickedItem.getType() == Material.PAPER) {
            int currentPage = getCurrentPage(title);
            if (displayName.contains("Następna strona")) {
                open(whoClicked, whoClicked, currentPage + 1);
            } else if (displayName.contains("Poprzednia strona")) {
                open(whoClicked, whoClicked, currentPage - 1);
            }
            event.setCancelled(true);
            return;
        }

        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        List<String> clickedItemLore = clickedItemMeta.getLore();

        if (clickedItemLore.size() < 3) {
            event.setCancelled(true);
            return;
        }

        UUID backupID = UUID.fromString(clickedItemMeta.getDisplayName().substring(15));
        UUID playerUUID = UUID.fromString(clickedItemLore.get(2).substring(17));
        Player player = Bukkit.getPlayer(playerUUID);

        if (event.getClick().isLeftClick()) {
            backupGive.give(player, backupID);
            whoClicked.sendTitle(Chat.color(helper.getColor() + "&lBACKUP"),
                    Chat.color("&fOddałeś backup dla gracza " + Chat.color(helper.getColor() + player.getName())),
                    10, 100, 10);
            player.sendTitle(Chat.color(helper.getColor() + "&lBACKUP"),
                    Chat.color("&fOtrzymałeś backupa od administratora"),
                    10, 100, 10);
            whoClicked.closeInventory();
        } else if (event.getClick().isRightClick()) {
            backupInformationGui.open(whoClicked, player, backupID);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void inventoryDrag(InventoryDragEvent event) {
        Inventory clickedInventory = event.getInventory();
        if (clickedInventory == null || !event.getView().getTopInventory().equals(clickedInventory)) {
            return;
        }

        if (!event.getView().getTitle().startsWith(Chat.color("&8Zapisane backupy"))) {
            return;
        }

        event.setCancelled(true);
    }

    private int getCurrentPage(String title) {
        int result = Integer.parseInt(title.substring(20).replace(")", "")) - 1;
        return result;
    }

    private static class BackupInfo {
        String playerName;
        String playerUUID;
        String playerKiller;
        String deathTime;
        String playerPing;

        BackupInfo(String playerName, String playerUUID, String playerKiller, String deathTime, String playerPing) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.playerKiller = playerKiller;
            this.deathTime = deathTime;
            this.playerPing = playerPing;
        }
    }
}
