package su.nightexpress.excellentenchants.enchantment.impl.universal;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.meta.Period;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.io.File;

import static su.nightexpress.excellentenchants.Placeholders.GENERIC_AMOUNT;

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

    public SelfRepairingEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.treasure(TradeType.SAVANNA_SPECIAL));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            "Repairs " + GENERIC_AMOUNT + " durability points over time.",
            EnchantRarity.RARE,
            3,
            ItemCategories.BREAKABLE
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.meta.setPeriod(Period.create(config));

        this.repairAmount = Modifier.read(config, "Settings.Repair.Amount",
            Modifier.add(1, 1, 1, 10),
            "Amount of durability points to be repaired per trigger."
        );

        String modeStr = ConfigValue.create("Settings.Repair_Mode",
            "BOTH",
            "Sets where items will be repaired:",
            "HOLDING - Only repair items being held or worn as armor",
            "INVENTORY - Only repair items in inventory (not being held or worn)",
            "BOTH - Repair items regardless of where they are"
        ).read(config);
        
        this.repairMode = RepairMode.fromString(modeStr);

        this.repairArmor = ConfigValue.create("Settings.Repair_Armor",
            true,
            "When set to 'true', will also repair armor items when worn."
        ).read(config);

        this.addPlaceholder(GENERIC_AMOUNT, level -> NumberUtil.format(this.getRepairAmount(level)));
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

            // Filter based on the mode. Manager calls us for both equipped and inventory items (when fixed).
            switch (this.repairMode) {
                case HOLDING:
                    if (!isEquipped) return false; // Only repair equipped
                    break;
                case INVENTORY:
                    if (isEquipped) return false; // Only repair non-equipped (in inventory)
                    break;
                case BOTH:
                    // No filtering needed for BOTH mode, manager handles iteration
                    break;
            }
        }
        // Note: For non-player entities, it implicitly behaves like 'HOLDING' because
        // the default EnchantManager only iterates equipped items.

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