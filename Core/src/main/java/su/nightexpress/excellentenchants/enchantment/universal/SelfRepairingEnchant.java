package su.nightexpress.excellentenchants.enchantment.universal;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantData;
import su.nightexpress.excellentenchants.api.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Period;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.io.File;

public class SelfRepairingEnchant extends GameEnchantment implements PassiveEnchant {

    public static final String ID = "repairing";
    
    public enum RepairMode {
        HOLDING("Only repair items being held or worn"),
        INVENTORY("Only repair items in inventory, not held or worn"), 
        BOTH("Repair all items regardless of where they are");
        
        private final String description;
        
        RepairMode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static RepairMode fromString(String name) {
            try {
                return valueOf(name.toUpperCase());
            }
            catch (Exception e) {
                return BOTH;
            }
        }
    }

    private Modifier repairAmount;
    private RepairMode repairMode;
    private boolean repairArmor;

    public SelfRepairingEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file, @NotNull EnchantData data) {
        super(plugin, file, data);
        this.addComponent(EnchantComponent.PERIODIC, Period.ofSeconds(30));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.repairAmount = Modifier.load(config, "Settings.Repair.Amount",
            Modifier.addictive(1).perLevel(1).capacity(10),
            "Amount of durability points to be repaired per trigger."
        );

        String modeStr = config.getString("Settings.Repair_Mode", "BOTH");
        this.repairMode = RepairMode.fromString(modeStr);
        
        // Add configuration comments
        config.addMissing("Settings.Repair_Mode", "BOTH");
        config.setComments("Settings.Repair_Mode", 
            "Sets where items will be repaired:",
            "HOLDING - Only repair items being held or worn as armor",
            "INVENTORY - Only repair items in inventory (not being held or worn)",
            "BOTH - Repair items regardless of where they are"
        );

        this.repairArmor = config.getBoolean("Settings.Repair_Armor", true);
        config.setComments("Settings.Repair_Armor", 
            "When set to 'true', will also repair armor items when worn."
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT, level -> NumberUtil.format(this.getRepairAmount(level)));
    }

    public int getRepairAmount(int level) {
        return (int) this.repairAmount.getValue(level);
    }

    @Override
    public boolean onTrigger(@NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        // Basic durability check - if item has no durability, don't bother
        if (item.getType().getMaxDurability() <= 0) {
            return false;
        }

        // Skip non-damageable items
        if (!(item.getItemMeta() instanceof Damageable damageable)) {
            return false;
        }

        // Skip if already fully repaired
        int damage = damageable.getDamage();
        if (damage <= 0) {
            return false;
        }

        // Skip based on repair mode if the entity is a player
        if (entity instanceof Player player) {
            boolean isEquipped = isItemEquipped(player, item);

            // Filter based on the mode
            switch (this.repairMode) {
                case HOLDING:
                    if (!isEquipped) return false; // Only repair equipped
                    break;
                case INVENTORY:
                    if (isEquipped) return false; // Only repair non-equipped (in inventory)
                    break;
                case BOTH:
                    // No filtering needed for BOTH mode
                    break;
            }
        }

        // Calculate and apply repair
        int repairAmount = this.getRepairAmount(level);
        int newDamage = Math.max(0, damage - repairAmount);
        
        // Only make the change if we actually repaired something
        if (newDamage < damage) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable metaDamageable) {
                metaDamageable.setDamage(newDamage);
                item.setItemMeta(meta);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isItemEquipped(Player player, ItemStack item) {
        // Check hands first
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (item.isSimilar(mainHand) || item.isSimilar(offHand)) {
            return true;
        }

        // Check armor if armor repair is enabled
        if (this.repairArmor) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack piece : armor) {
                if (piece != null && item.isSimilar(piece)) {
                    return true;
                }
            }
        }

        return false;
    }
}