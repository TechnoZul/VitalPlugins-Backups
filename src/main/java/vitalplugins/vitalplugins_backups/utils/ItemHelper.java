package vitalplugins.vitalplugins_backups.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemHelper {

    public ItemStack createItem(Material material, String name, String lore) {
        List<String> loreList = (lore == null || lore.isEmpty())
                ? Collections.emptyList()
                : Arrays.asList(lore.split("\n"));
        return createItem(material, name, loreList);
    }

    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return itemStack;

        if (name != null && !name.isEmpty()) {
            itemMeta.setDisplayName(Chat.color(name));
        } else {
            itemMeta.setDisplayName(" ");
        }
        if (lore != null && !lore.isEmpty()) {
            itemMeta.setLore(Chat.colorList(lore));
        }
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
