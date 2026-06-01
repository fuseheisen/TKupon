package net.turkeynw.tkupon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final TKupon plugin;

    public InventoryListener(TKupon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith("Kupon Düzenle: ")) {
            String kod = title.replace("Kupon Düzenle: ", "");
            Inventory inventory = event.getInventory();
            ItemStack[] contents = inventory.getContents();

            plugin.getFoliaLib().getImpl().runAsync((task) -> {
                try {
                    String base64 = Base64Utils.itemStackArrayToBase64(contents);
                    plugin.getFileManager().getCoupons().set("coupons." + kod + ".items", base64);
                    plugin.getFileManager().saveCoupons();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}