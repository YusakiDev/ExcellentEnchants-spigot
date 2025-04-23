package su.nightexpress.excellentenchants.enchantment;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.ConfigBridge;
import su.nightexpress.excellentenchants.api.EnchantmentID;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.enchantment.bridge.FlameWalker;
import su.nightexpress.excellentenchants.api.enchantment.meta.PeriodMeta;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.enchantment.listener.AnvilListener;
import su.nightexpress.excellentenchants.enchantment.listener.GenericListener;
import su.nightexpress.excellentenchants.enchantment.menu.EnchantsMenu;
import su.nightexpress.excellentenchants.lib.folialib.FoliaLibWrapper;
import su.nightexpress.excellentenchants.registry.EnchantRegistry;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.excellentenchants.util.EnchantedProjectile; 

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantManager extends AbstractManager<EnchantsPlugin> {

    private final Set<PassiveEnchant> passiveEnchants;
    private EnchantsMenu enchantsMenu;

    public EnchantManager(@NotNull EnchantsPlugin plugin) {
        super(plugin);
        this.passiveEnchants = new HashSet<>(EnchantRegistry.getEnchantments(PassiveEnchant.class));
    }

    protected void onLoad() {
        this.enchantsMenu = new EnchantsMenu(this.plugin);

        this.addListener(new GenericListener(this.plugin, this));
        this.addListener(new AnvilListener(this.plugin));

        plugin.getFoliaLib().runTimerAsync(task -> displayProjectileTrails(), Config.CORE_PROJECTILE_PARTICLE_INTERVAL.get(), Config.CORE_PROJECTILE_PARTICLE_INTERVAL.get());

        if (!this.passiveEnchants.isEmpty()) {
            plugin.getFoliaLib().runTimer(task -> updatePassiveEnchantEffects(), ConfigBridge.getEnchantsTickInterval(), ConfigBridge.getEnchantsTickInterval());
        }

        CustomEnchantment enchantment = EnchantRegistry.getById(EnchantmentID.FLAME_WALKER);
        if (enchantment instanceof FlameWalker flameWalker) {
            plugin.getFoliaLib().runTimer(task -> flameWalker.tickBlocks(), 1, 1);
        }
    }

    @Override
    protected void onShutdown() {
        CustomEnchantment enchantment = EnchantRegistry.getById(EnchantmentID.FLAME_WALKER);
        if (enchantment instanceof FlameWalker flameWalker) {
            flameWalker.removeBlocks();
        }

        if (this.enchantsMenu != null) this.enchantsMenu.clear();
    }

    public void openEnchantsMenu(@NotNull Player player) {
        this.enchantsMenu.open(player);
    }

    private void displayProjectileTrails() {
        // Create a copy of the projectiles to avoid concurrent modification
        Set<EnchantedProjectile> projectiles = new HashSet<>(EnchantUtils.getEnchantedProjectiles());
        
        for (EnchantedProjectile projectile : projectiles) {
            // Schedule validity check and particle display on the correct thread
            plugin.getFoliaLib().runAtEntity(projectile.getProjectile(), task -> {
                if (!projectile.isValid()) {
                    EnchantUtils.getEnchantedProjectiles().remove(projectile);
                    return;
                }
                projectile.playParticles();
            });
        }
    }

    private void updatePassiveEnchantEffects() {
        try {
            Set<PassiveEnchant> readyEnchants = this.passiveEnchants.stream()
                .peek(PeriodMeta::consumeTicks)
                .filter(PeriodMeta::isTriggerTime)
                .collect(Collectors.toSet());
            if (readyEnchants.isEmpty()) return;

            // Get entities but process them individually in their own regions
            Set<LivingEntity> entities = this.getPassiveEnchantEntities();
            for (LivingEntity entity : entities) {
                try {
                    plugin.getFoliaLib().runAtEntity(entity, task -> {
                        try {
                            for (PassiveEnchant enchant : readyEnchants) {
                                EnchantUtils.getEquipped(entity, enchant).forEach((item, level) -> {
                                    try {
                                        if (!enchant.isAvailableToUse(entity)) return;
                                        if (enchant.isOutOfCharges(item)) return;
                                        if (enchant.onTrigger(entity, item, level)) {
                                            enchant.consumeCharges(item, level);
                                        }
                                    }
                                    catch (Exception ex) {
                                        // Log error but continue processing other items
                                        this.plugin.error("Error processing enchanted item: " + ex.getMessage());
                                    }
                                });
                            }
                        }
                        catch (Exception ex) {
                            // Log error but continue processing other entities
                            this.plugin.error("Error processing entity enchants: " + ex.getMessage());
                        }
                    });
                }
                catch (Exception ex) {
                    // Log entity scheduling error but continue
                    this.plugin.error("Error scheduling entity task: " + ex.getMessage());
                }
            }

            // Update trigger time after processing all entities
            readyEnchants.forEach(PassiveEnchant::updateTriggerTime);
        }
        catch (Exception ex) {
            // Catch any other errors to prevent timer from stopping
            this.plugin.error("Error in passive enchant processing: " + ex.getMessage());
            if (plugin.getFoliaLib().isFolia()) {
                this.plugin.warn("This error may be related to Folia cross-region entity access.");
                this.plugin.warn("Consider disabling 'Core.Apply_Passive_Enchants_To_Mobs' in config.yml");
            }
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

