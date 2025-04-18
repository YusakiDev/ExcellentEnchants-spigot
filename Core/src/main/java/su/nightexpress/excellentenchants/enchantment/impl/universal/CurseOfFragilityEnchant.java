package su.nightexpress.excellentenchants.enchantment.impl.universal;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.enchantment.type.GenericEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.SimpeListener;

import java.io.File;

public class CurseOfFragilityEnchant extends GameEnchantment implements GenericEnchant, SimpeListener {

    public static final String ID = "curse_of_fragility";

    public CurseOfFragilityEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.treasure());
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Prevents an item from being grindstoned or anviled.",
            EnchantRarity.COMMON,
            1,
            ItemCategories.BREAKABLE
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {

    }

    @Override
    public boolean isCurse() {
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);

        boolean cursedFirst = (first != null && EnchantUtils.getLevel(first, this.getBukkitEnchantment()) >= 1);
        boolean cursedSecond = (second != null && EnchantUtils.getLevel(second, this.getBukkitEnchantment()) >= 1);

        if (cursedFirst || cursedSecond) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemGrindstoneClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.GRINDSTONE) return;

        this.stopGrindstone(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemGrindstoneDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.GRINDSTONE) return;

        this.stopGrindstone(inventory);
    }

    private void stopGrindstone(@NotNull Inventory inventory) {
        this.plugin.getFoliaLib().runNextTick(task -> {
            ItemStack first = inventory.getItem(0);
            ItemStack second = inventory.getItem(1);

            boolean cursedFirst = (first != null && EnchantUtils.getLevel(first, this.getBukkitEnchantment()) >= 1);
            boolean cursedSecond = (second != null && EnchantUtils.getLevel(second, this.getBukkitEnchantment()) >= 1);

            if (cursedFirst || cursedSecond) {
                inventory.setItem(2, null);
            }
        });
    }

}
