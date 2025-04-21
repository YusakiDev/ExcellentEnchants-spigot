package su.nightexpress.excellentenchants.enchantment.impl.universal;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
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
        // For players, check repair mode first
        if (entity instanceof Player player) {
            boolean isHeldItem = false;
            boolean isArmorItem = false;
            
            // Check if this is a held item
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (item.equals(mainHand) || item.equals(offHand)) {
                isHeldItem = true;
            }
            
            // Check if this is an armor item
            if (this.repairArmor) {
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    if (item.equals(armor)) {
                        isArmorItem = true;
                        break;
                    }
                }
            }
            
            // Determine if we should repair based on the mode
            boolean shouldRepair = switch (repairMode) {
                case HOLDING -> isHeldItem || isArmorItem;
                case INVENTORY -> !isHeldItem && !isArmorItem;
                case BOTH -> true;
            };
            
            if (!shouldRepair) {
                return false;
            }
        }

        // Check if item is repairable
        if (!item.getType().isItem() || item.getType().getMaxDurability() <= 0) {
            return false;
        }

        if (!(item.getItemMeta() instanceof Damageable damageable)) {
            return false;
        }

        // Check if item needs repair
        int damage = damageable.getDamage();
        if (damage <= 0) {
            return false;
        }

        // Calculate repair amount
        int repairAmount = this.getRepairAmount(level);
        int newDamage = Math.max(0, damage - repairAmount);
        
        // Apply repair
        damageable.setDamage(newDamage);
        item.setItemMeta(damageable);
        
        return true;
    }
    
    private boolean isArmorPiece(ItemStack item) {
        Material type = item.getType();
        return type.name().endsWith("_HELMET") || 
               type.name().endsWith("_CHESTPLATE") || 
               type.name().endsWith("_LEGGINGS") || 
               type.name().endsWith("_BOOTS");
    }
} 