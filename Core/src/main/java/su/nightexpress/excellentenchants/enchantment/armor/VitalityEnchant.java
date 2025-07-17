package su.nightexpress.excellentenchants.enchantment.armor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantData;
import su.nightexpress.excellentenchants.api.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VitalityEnchant extends GameEnchantment implements PassiveEnchant, Listener {

    private boolean visualEffects;
    private Modifier healthPerLevel;
    private Modifier baseHealth;
    
    // Track active attributes by player and item slot
    private final Map<UUID, Map<EquipmentSlot, AttributeModifier>> activeModifiers = new ConcurrentHashMap<>();

    public VitalityEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file, @NotNull EnchantData data) {
        super(plugin, file, data);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.baseHealth = Modifier.load(config, "Settings.Base_Health", 
            Modifier.addictive(2.0), 
            "Base health boost amount."
        );
        
        this.healthPerLevel = Modifier.load(config, "Settings.Health_Per_Level", 
            Modifier.addictive(2.0), 
            "Additional health per enchantment level."
        );
        
        this.visualEffects = config.getBoolean("Settings.Visual_Effects", false);
        
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT, level -> NumberUtil.format(this.getHealthBoost(level)));
    }

    
    public double getHealthBoost(int level) {
        return this.baseHealth.getValue(level) + (this.healthPerLevel.getValue(level) * (level - 1));
    }
    
    @Override
    public boolean onTrigger(@NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!(entity instanceof Player player)) return false;
        
        // Process all equipment when passive enchant ticks
        this.plugin.runAtEntity(player, task -> processAllEquipment(player));
        return true;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Process player's equipment when they join
        this.plugin.runAtEntity(event.getPlayer(), 
            task -> processAllEquipment(event.getPlayer()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up when player leaves
        this.cleanupPlayer(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Process equipment after inventory changes
        this.plugin.runAtEntity(player, 
            task -> processAllEquipment(player));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemBreak(PlayerItemBreakEvent event) {
        // Process when an item breaks
        this.plugin.runAtEntity(event.getPlayer(), 
            task -> processAllEquipment(event.getPlayer()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Process after picking up items that might be equippable
        this.plugin.runAtEntity(player, 
            task -> processAllEquipment(player));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if this is a right-click action that might equip armor
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || 
            event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && isArmor(item.getType())) {
                // Schedule a task to check equipment after potential armor equip
                this.plugin.runAtEntity(player, 
                    task -> processAllEquipment(player));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Remove all modifiers when player dies
        cleanupPlayer(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Process equipment after respawn
        this.plugin.runAtEntity(player, 
            task -> processAllEquipment(player));
    }
    
    private void processAllEquipment(Player player) {
        if (!player.isOnline()) return;
        
        // First clean up all existing modifiers to refresh
        this.cleanupPlayer(player);
        
        // Process each armor piece
        ItemStack helmet = player.getInventory().getHelmet();
        processArmorPiece(player, helmet, EquipmentSlot.HEAD);
        
        ItemStack chestplate = player.getInventory().getChestplate();
        processArmorPiece(player, chestplate, EquipmentSlot.CHEST);
        
        ItemStack leggings = player.getInventory().getLeggings();
        processArmorPiece(player, leggings, EquipmentSlot.LEGS);
        
        ItemStack boots = player.getInventory().getBoots();
        processArmorPiece(player, boots, EquipmentSlot.FEET);
    }
    
    private void processArmorPiece(Player player, ItemStack item, EquipmentSlot slot) {
        if (item == null || !EnchantUtils.isEquipment(item)) return;
        
        // Check if item has this enchantment
        int level = EnchantUtils.getLevel(item, this.getBukkitEnchantment());
        if (level <= 0) return;
        
        // Calculate health boost amount
        double healthBoost = this.getHealthBoost(level);
        
        // Apply the modifier
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            NamespacedKey modifierKey = new NamespacedKey(this.plugin, "vitality_" + slot.name().toLowerCase());
            AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                healthBoost,
                AttributeModifier.Operation.ADD_NUMBER
            );
            
            maxHealthAttribute.addModifier(modifier);
            
            // Store the modifier for later cleanup
            this.activeModifiers.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(slot, modifier);
            
            if (this.visualEffects) {
                UniParticle.of(Particle.HEART).play(player.getEyeLocation(), 0.5, 0.25, 2);
            }
        }
    }
    
    /**
     * Clean up all modifiers for a specific player
     */
    public void cleanupPlayer(@NotNull Player player) {
        Map<EquipmentSlot, AttributeModifier> playerModifiers = this.activeModifiers.remove(player.getUniqueId());
        if (playerModifiers == null) return;
        
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            for (AttributeModifier modifier : playerModifiers.values()) {
                maxHealthAttribute.removeModifier(modifier);
            }
        }
    }
    
    /**
     * Clean up all modifiers - called on plugin disable
     */
    public void cleanupAll() {
        for (Map.Entry<UUID, Map<EquipmentSlot, AttributeModifier>> entry : this.activeModifiers.entrySet()) {
            Player player = this.plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) {
                for (AttributeModifier modifier : entry.getValue().values()) {
                    maxHealthAttribute.removeModifier(modifier);
                }
            }
        }
        this.activeModifiers.clear();
    }
    
    /**
     * Check if a material is armor
     */
    private boolean isArmor(Material material) {
        return material.name().contains("HELMET") || 
               material.name().contains("CHESTPLATE") || 
               material.name().contains("LEGGINGS") || 
               material.name().contains("BOOTS");
    }
}