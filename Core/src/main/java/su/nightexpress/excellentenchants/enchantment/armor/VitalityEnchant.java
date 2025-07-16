package su.nightexpress.excellentenchants.enchantment.armor;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantData;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Period;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VitalityEnchant extends GameEnchantment implements PassiveEnchant {
    
    private double healthPerLevel;
    private double baseHealth;
    private boolean visualEffects;
    
    // Track active modifiers by entity
    private final Map<UUID, AttributeModifier> activeModifiers = new ConcurrentHashMap<>();

    public VitalityEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file, @NotNull EnchantData data) {
        super(plugin, file, data);
        this.addComponent(EnchantComponent.PERIODIC, Period.ofSeconds(1));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.baseHealth = config.getDouble("Settings.Base_Health", 2.0);
        this.healthPerLevel = config.getDouble("Settings.Health_Per_Level", 2.0);
        this.visualEffects = config.getBoolean("Settings.Visual_Effects", false);
    }

    @Override
    public boolean onTrigger(@NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!(entity instanceof Player player)) return false;
        
        // Calculate total health boost for all equipped items with this enchantment
        double totalHealthBoost = calculateTotalHealthBoost(player);
        
        // Apply or update the health modifier
        updateHealthModifier(player, totalHealthBoost);
        
        if (this.visualEffects && totalHealthBoost > 0) {
            UniParticle.of(Particle.HEART).play(player.getEyeLocation(), 0.3, 0.2, 1);
        }
        
        return true;
    }
    
    private double calculateTotalHealthBoost(@NotNull Player player) {
        double totalBoost = 0.0;
        
        // Check all armor pieces for this enchantment
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armor : armorContents) {
            if (armor == null || !armor.hasItemMeta()) continue;
            
            if (armor.getItemMeta().hasEnchant(this.getBukkitEnchantment())) {
                int level = armor.getItemMeta().getEnchantLevel(this.getBukkitEnchantment());
                totalBoost += this.baseHealth + (this.healthPerLevel * (level - 1));
            }
        }
        
        return totalBoost;
    }
    
    private void updateHealthModifier(@NotNull Player player, double healthBoost) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null) return;
        
        UUID playerId = player.getUniqueId();
        
        // Remove existing modifier if present
        AttributeModifier existingModifier = this.activeModifiers.get(playerId);
        if (existingModifier != null) {
            maxHealthAttribute.removeModifier(existingModifier);
            this.activeModifiers.remove(playerId);
        }
        
        // Add new modifier if health boost > 0
        if (healthBoost > 0) {
            NamespacedKey key = new NamespacedKey(this.plugin, "vitality_" + playerId);
            AttributeModifier modifier = new AttributeModifier(
                key,
                healthBoost,
                AttributeModifier.Operation.ADD_NUMBER
            );
            
            maxHealthAttribute.addModifier(modifier);
            this.activeModifiers.put(playerId, modifier);
        }
    }
    
    public void cleanupPlayer(@NotNull Player player) {
        AttributeModifier modifier = this.activeModifiers.remove(player.getUniqueId());
        if (modifier != null) {
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) {
                maxHealthAttribute.removeModifier(modifier);
            }
        }
    }
}