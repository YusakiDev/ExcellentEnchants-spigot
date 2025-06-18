package su.nightexpress.excellentenchants.enchantment.impl.tool;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Barrel;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockDropEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.SimpeListener;
import su.nightexpress.nightcore.util.ItemReplacer;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.Plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class SilkChestEnchant extends GameEnchantment implements BlockDropEnchant, SimpeListener {

    public static final String ID = "silk_chest";

    private       String        containerName;
    private       List<String>  containerLore;
    private final NamespacedKey keyContainer;

    public SilkChestEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.PLAINS_SPECIAL));

        this.keyContainer = new NamespacedKey(plugin, ID + ".item");
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Drop chests and barrels with all their contents.",
            EnchantRarity.MYTHIC,
            1,
            ItemCategories.TOOL,
            ItemCategories.AXE
        );
    }

    @Override
    public boolean checkServerRequirements() {
        if (Plugins.isSpigot()) {
            this.warn("Enchantment can only be used in PaperMC or Paper based forks.");
            return false;
        }
        return true;
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.containerName = ConfigValue.create("Settings.Chest_Item.Name", "Container (" + Placeholders.GENERIC_AMOUNT + " items)",
            "Container item display name.",
            "Use '" + Placeholders.GENERIC_AMOUNT + "' for items amount."
        ).read(config);

        this.containerLore = ConfigValue.create("Settings.Chest_Item.Lore", new ArrayList<>(),
            "Container item lore.",
            "Use '" + Placeholders.GENERIC_AMOUNT + "' for items amount."
        ).read(config);
    }

    public boolean isSilkContainer(@NotNull ItemStack item) {
        return PDCUtil.getBoolean(item, this.keyContainer).isPresent();
    }

    @NotNull
    public ItemStack getSilkContainer(@NotNull Container container) {
        ItemStack containerStack = new ItemStack(container.getType());

        BlockStateMeta stateMeta = (BlockStateMeta) containerStack.getItemMeta();
        if (stateMeta == null) return containerStack;

        BlockState containerState = stateMeta.getBlockState();
        if (!(containerState instanceof Container containerItem)) return containerStack;
        
        containerItem.getInventory().setContents(container.getInventory().getContents());
        containerItem.update(true);

        int amount = (int) Stream.of(containerItem.getInventory().getContents()).filter(i -> i != null && !i.getType().isAir()).count();

        stateMeta.setBlockState(containerItem);
        stateMeta.setDisplayName(this.containerName);
        stateMeta.setLore(this.containerLore);
        containerStack.setItemMeta(stateMeta);

        ItemReplacer.replace(containerStack, str -> str.replace(Placeholders.GENERIC_AMOUNT, String.valueOf(amount)));
        PDCUtil.set(containerStack, this.keyContainer, true);
        return containerStack;
    }

    @Override
    public boolean onDrop(@NotNull BlockDropItemEvent event,
                          @NotNull LivingEntity player, @NotNull ItemStack item, int level) {
        BlockState state = event.getBlockState();

        // Only support Chest and Barrel types specifically
        if (!(state instanceof Chest || state instanceof Barrel)) return false;
        
        if (!(state instanceof Container container)) return false;

        // Remove original container from drops to prevent duplication
        AtomicBoolean originRemoved = new AtomicBoolean(false);
        event.getItems().removeIf(drop -> drop.getItemStack().getType() == state.getType() && drop.getItemStack().getAmount() == 1 && !originRemoved.getAndSet(true));
        
        // Add container content back to the container
        container.getInventory().addItem(event.getItems().stream().map(Item::getItemStack).toList().toArray(new ItemStack[0]));
        
        // Drop nothing of container content
        event.getItems().clear();

        if (container.getInventory().isEmpty()) {
            this.plugin.populateResource(event, new ItemStack(container.getType()));
            return false;
        }

        this.plugin.populateResource(event, this.getSilkContainer(container));

        container.getInventory().clear();

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSilkContainerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType().isAir()) return;

        Block block = event.getBlockPlaced();
        BlockState state = block.getState();
        
        // Only handle Chest and Barrel types
        if (!(state instanceof Chest || state instanceof Barrel)) return;
        
        if (!(state instanceof Container container)) return;

        container.setCustomName(null);
        container.update(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSilkContainerStore(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.CRAFTING) {
            int hotkey = event.getHotbarButton();
            if (hotkey >= 0) {
                Player player = (Player) event.getWhoClicked();
                ItemStack hotItem = player.getInventory().getItem(hotkey);
                if (hotItem != null && this.isSilkContainer(hotItem)) {
                    event.setCancelled(true);
                    return;
                }
            }

            ItemStack item = event.getCurrentItem();
            if (item == null) return;

            if (this.isSilkContainer(item)) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        boolean isRightClick = (event.isRightClick() && !event.isShiftClick()) || event.getClick() == ClickType.CREATIVE;
        if (item.getType() == Material.BUNDLE && isRightClick) {
            ItemStack cursor = event.getView().getCursor(); // Creative is shit, undetectable.
            if (cursor != null && this.isSilkContainer(cursor)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSilkContainerHopper(InventoryPickupItemEvent event) {
        event.setCancelled(this.isSilkContainer(event.getItem().getItemStack()));
    }
}
