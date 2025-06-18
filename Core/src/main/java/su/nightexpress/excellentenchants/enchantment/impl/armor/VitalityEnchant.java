package su.nightexpress.excellentenchants.enchantment.impl.armor;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.GenericEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.SimpeListener;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VitalityEnchant extends GameEnchantment implements GenericEnchant, SimpeListener {

    public static final String ID = "vitality";
    
    private boolean visualEffects;
    private double healthPerLevel;
    private double baseHealth;
    
    // Track active attributes by player and item slot
    private final Map<UUID, Map<EquipmentSlot, AttributeModifier>> activeModifiers = new ConcurrentHashMap<>();

    public VitalityEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.PLAINS_SPECIAL));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Grants +%value% max health while equipped.",
            EnchantRarity.MYTHIC,
            3,
            ItemCategories.ARMOR
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.baseHealth = config.getDouble("Settings.Base_Health", 2.0);
        this.healthPerLevel = config.getDouble("Settings.Health_Per_Level", 2.0);
        this.visualEffects = config.getBoolean("Settings.Visual_Effects", false);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Process player's equipment when they join
        this.plugin.getFoliaLib().runAtLocation(event.getPlayer().getLocation(), 
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
        this.plugin.getFoliaLib().runAtLocation(player.getLocation(), 
            task -> processAllEquipment(player));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemBreak(PlayerItemBreakEvent event) {
        // Process when an item breaks
        this.plugin.getFoliaLib().runAtLocation(event.getPlayer().getLocation(), 
            task -> processAllEquipment(event.getPlayer()));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Process after picking up items that might be equippable
        this.plugin.getFoliaLib().runAtLocation(player.getLocation(), 
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
                this.plugin.getFoliaLib().runAtLocation(player.getLocation(), 
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
        this.plugin.getFoliaLib().runAtLocation(player.getLocation(), 
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
        if (item == null || !this.isSupportedItem(item)) return;
        
        // Check if item has this enchantment
        if (!item.getItemMeta().hasEnchant(this.getBukkitEnchantment())) return;
        
        int level = item.getItemMeta().getEnchantLevel(this.getBukkitEnchantment());
        
        // Calculate health boost amount
        double healthBoost = this.baseHealth + (this.healthPerLevel * (level - 1));
        
        // Create a unique ID for this modifier
        String uniqueKey = this.getId() + ":" + player.getUniqueId() + ":" + slot.name();
        UUID attributeUUID = UUID.nameUUIDFromBytes(uniqueKey.getBytes());
        
        // Apply the modifier
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            
            // Remove existing modifier if present
            maxHealthAttribute.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(attributeUUID))
                .findFirst()
                .ifPresent(maxHealthAttribute::removeModifier);
            
            AttributeModifier modifier = new AttributeModifier(
                attributeUUID,
                "vitality_enchant_" + level + "_" + slot.name(),
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
        
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            for (AttributeModifier modifier : playerModifiers.values()) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
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
            
            if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                for (AttributeModifier modifier : entry.getValue().values()) {
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(modifier);
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