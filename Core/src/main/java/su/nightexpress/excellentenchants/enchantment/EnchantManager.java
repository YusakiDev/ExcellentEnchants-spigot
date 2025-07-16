package su.nightexpress.excellentenchants.enchantment;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.config.ConfigBridge;
import su.nightexpress.excellentenchants.api.EnchantId;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.enchantment.universal.SelfRepairingEnchant;
import su.nightexpress.excellentenchants.manager.listener.AnvilListener;
import su.nightexpress.excellentenchants.manager.listener.GenericListener;
import su.nightexpress.excellentenchants.manager.menu.EnchantsMenu;
import su.nightexpress.excellentenchants.api.EnchantRegistry;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.PDCUtil;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantManager extends AbstractManager<EnchantsPlugin> {

    private final Set<PassiveEnchant> passiveEnchants;
    private EnchantsMenu enchantsMenu;

    public EnchantManager(@NotNull EnchantsPlugin plugin) {
        super(plugin);
        this.passiveEnchants = new HashSet<>(); // Will be populated when enchantments are loaded
    }

    protected void onLoad() {
        this.enchantsMenu = new EnchantsMenu(this.plugin);

        this.addListener(new GenericListener(this.plugin, this));
        this.addListener(new AnvilListener(this.plugin));

        // Projectile trail timer removed in new architecture

        if (!this.passiveEnchants.isEmpty()) {
            plugin.getFoliaLib().runTimer(task -> updatePassiveEnchantEffects(), ConfigBridge.getEnchantsTickInterval(), ConfigBridge.getEnchantsTickInterval());
        }

        // FlameWalker handling removed in new architecture
    }

    @Override
    protected void onShutdown() {
        // FlameWalker cleanup removed in new architecture

        if (this.enchantsMenu != null) this.enchantsMenu.clear();
    }

    public void openEnchantsMenu(@NotNull Player player) {
        this.enchantsMenu.open(player);
    }

    private void displayProjectileTrails() {
        // Projectile trail functionality removed in new architecture
    }

    private void updatePassiveEnchantEffects() {
        try {
            // Process all passive enchants directly without period meta filtering

            // Get entities but process them individually in their own regions
            Set<LivingEntity> entities = this.getPassiveEnchantEntities();
            for (LivingEntity entity : entities) {
                try {
                    plugin.getFoliaLib().runAtEntity(entity, task -> {
                        try {
                            for (PassiveEnchant enchant : this.passiveEnchants) {
                                // Special handling for SelfRepairing to include inventory for players
                                if (enchant instanceof SelfRepairingEnchant && entity instanceof Player player) {
                                    PlayerInventory inventory = player.getInventory();
                                    
                                    // 1. Check Equipped Items (using the standard method)
                                    EnchantUtils.getEquipped(entity, enchant).forEach((item, level) -> {
                                        this.tryTriggerEnchant(enchant, entity, item, level);
                                    });

                                    // 2. Check Main Inventory Items (slots 0-35)
                                    for (int i = 0; i < 36; i++) {
                                        ItemStack item = inventory.getItem(i);
                                        if (item == null || item.getType().isAir()) continue;

                                        // Get level and trigger if applicable
                                        int level = EnchantUtils.getLevel(item, enchant.getBukkitEnchantment());
                                        if (level > 0) {
                                            this.tryTriggerEnchant(enchant, entity, item, level);
                                        }
                                    }
                                }
                                // Default behavior for other enchants or non-players
                                else {
                                    EnchantUtils.getEquipped(entity, enchant).forEach((item, level) -> {
                                        this.tryTriggerEnchant(enchant, entity, item, level);
                                    });
                                }
                            }
                        }
                        catch (Exception ex) {
                            // Log error but continue processing other entities
                            this.plugin.error("Error processing entity enchants: " + ex.getMessage());
                            ex.printStackTrace(); 
                        }
                    });
                }
                catch (Exception ex) {
                    // Log entity scheduling error but continue
                    this.plugin.error("Error scheduling entity task: " + ex.getMessage());
                    ex.printStackTrace(); 
                }
            }

            // Timing is now handled by components in the new architecture
        }
        catch (Exception ex) {
            // Catch any other errors to prevent timer from stopping
            this.plugin.error("Error in passive enchant processing: " + ex.getMessage());
            ex.printStackTrace(); 
            if (plugin.getFoliaLib().isFolia()) {
                this.plugin.warn("This error may be related to Folia cross-region entity access.");
                this.plugin.warn("Consider disabling 'Core.Apply_Passive_Enchants_To_Mobs' in config.yml");
            }
        }
    }

    /**
     * Helper method to safely attempt triggering a passive enchant.
     * Includes a check to prevent double-triggering within the same processing cycle.
     */
    private void tryTriggerEnchant(@NotNull PassiveEnchant enchant, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        // Prevent double-triggering in the same processing cycle using currentTimeMillis
        NamespacedKey pdcKey = new NamespacedKey(plugin, "ee_last_repair_ts_" + enchant.getId());
        long currentTimeMs = System.currentTimeMillis();
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return; // Cannot work with PDC without meta
        
        // Get last processed time as String, default to "0"
        String lastTimeStr = PDCUtil.getString(meta, pdcKey).orElse("0");
        long lastTimeMs = 0;
        try {
            lastTimeMs = Long.parseLong(lastTimeStr);
        } catch (NumberFormatException ignored) { /* Ignore if parsing fails */ }

        // Allow processing if more than ~50ms (1 tick) has passed to prevent immediate re-trigger
        if (currentTimeMs - lastTimeMs < 50) { 
            return; // Already processed this item for this enchant very recently
        }

        try {
            if (!enchant.isAvailableToUse(entity)) return;
            if (enchant.isOutOfCharges(item)) return;
            
            // Call onTrigger and update PDC if successful
            if (enchant.onTrigger(entity, item, level)) {
                enchant.consumeCharges(item, level);
                // Get meta again in case onTrigger/consumeCharges modified it
                ItemMeta updatedMeta = item.getItemMeta();
                if (updatedMeta != null) {
                    // Use the basic PDCUtil.set, storing time as String
                    PDCUtil.set(updatedMeta, pdcKey, String.valueOf(currentTimeMs)); 
                    item.setItemMeta(updatedMeta); // Apply the updated meta with the PDC tag
                }
            }
        }
        catch (Exception ex) {
            // Log error but continue processing other items/enchants
            this.plugin.error("Error processing enchanted item (" + item.getType() + ") for enchant " + enchant.getId() + ": " + ex.getMessage());
            ex.printStackTrace(); 
        }
    }

    @NotNull
    private Set<LivingEntity> getPassiveEnchantEntities() {
        Set<LivingEntity> list = new HashSet<>();
        
        // Get players directly since they're thread-safe to access
        list.addAll(plugin.getServer().getOnlinePlayers());

        // Add mobs if enabled
        if (Config.CORE_PASSIVE_ENCHANTS_FOR_MOBS.get()) {
            // Instead of trying to access all entities at once, we'll check
            // configured flags and disable mob enchant checking if on Folia
            if (plugin.getFoliaLib().isFolia()) {
                // On Folia, we'll only process players for safety
                this.plugin.warn("Passive enchantments for mobs are not fully supported on Folia servers.");
                this.plugin.warn("Only player enchantments will be processed to avoid cross-region threading issues.");
                // We can't directly modify the ConfigValue, so we'll disable processing here
                // and recommend users update their config manually
                this.plugin.warn("Please set 'Core.Apply_Passive_Enchants_To_Mobs: false' in your config.yml");
            }
            else {
                // Original code for non-Folia servers
                plugin.getServer().getWorlds().stream()
                    .filter(world -> !world.getPlayers().isEmpty())
                    .forEach(world -> {
                        // Schedule mob collection per-region to be safe
                        plugin.getFoliaLib().runAtLocation(world.getSpawnLocation(), task -> {
                            try {
                                list.addAll(world.getEntitiesByClass(LivingEntity.class));
                            }
                            catch (Exception e) {
                                plugin.error("Error collecting entities in world " + world.getName() + ": " + e.getMessage());
                            }
                        });
                    });
            }
        }

        list.removeIf(entity -> {
            try {
                return entity.isDead() || !entity.isValid();
            }
            catch (Exception e) {
                // If we can't check, better remove it
                return true;
            }
        });
        return list;
    }
}

