package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantData;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.type.MiningEnchant;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Lists;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TunnelEnchant extends GameEnchantment implements MiningEnchant {

<<<<<<< HEAD:Core/src/main/java/su/nightexpress/excellentenchants/enchantment/impl/tool/TunnelEnchant.java
    public static final String   ID                   = "power_mine";
    // X and Z oftunneleach block AoE mined
=======
    // X and Z offsets for each block AoE mined
>>>>>>> origin/master:Core/src/main/java/su/nightexpress/excellentenchants/enchantment/tool/TunnelEnchant.java
    private static final int[][] MINING_COORD_OFFSETS = new int[][]{{0, 0}, {0, -1}, {-1, 0}, {0, 1}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1},};
    private static final Set<Material> INTERACTABLE_BLOCKS = new HashSet<>();

    static {
        INTERACTABLE_BLOCKS.add(Material.REDSTONE_ORE);
        INTERACTABLE_BLOCKS.add(Material.DEEPSLATE_REDSTONE_ORE);
    }

    private boolean disableOnSneak;

<<<<<<< HEAD:Core/src/main/java/su/nightexpress/excellentenchants/enchantment/impl/tool/TunnelEnchant.java
    public TunnelEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.treasure(TradeType.SWAMP_SPECIAL));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            Lists.newList("Mines multiple blocks at once in a 3x3 shape. Higher levels increase mining depth."),
            EnchantRarity.MYTHIC,
            3,
            ItemCategories.TOOL,
            ItemCategories.PICKAXE,
            Lists.newSet(VeinminerEnchant.ID, BlastMiningEnchant.ID)
        );
=======
    public TunnelEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file, @NotNull EnchantData data) {
        super(plugin, file, data);
>>>>>>> origin/master:Core/src/main/java/su/nightexpress/excellentenchants/enchantment/tool/TunnelEnchant.java
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.disableOnSneak = ConfigValue.create("Tunnel.Disable_On_Crouch",
            true,
            "Sets whether or not enchantment will have no effect when crouching.").read(config);
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!(entity instanceof Player player)) return false;
        if (EnchantUtils.isBusy()) return false;
        if (this.disableOnSneak && player.isSneaking()) return false;

        Block block = event.getBlock();
        //if (block.getType().isInteractable() && !INTERACTABLE_BLOCKS.contains(block.getType())) return false;
        //if (block.getDrops(item).isEmpty()) return false;

        final List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(Lists.newSet(Material.AIR, Material.WATER, Material.LAVA), 10);
        if (lastTwoTargetBlocks.size() != 2) {
            return false;
        }
        final Block targetBlock = lastTwoTargetBlocks.get(1);
        final Block adjacentBlock = lastTwoTargetBlocks.get(0);
        final BlockFace dir = adjacentBlock.getFace(targetBlock);
        boolean isZ = dir == BlockFace.EAST || dir == BlockFace.WEST;

        // Calculate depth based on enchantment level
        int depthMultiplier = level;
        
        // Always mine in 3x3 pattern
        int blocksBroken = 9;
        
        //player.sendMessage("§aTunnel Level: " + level + ", Depth: " + depthMultiplier + ", Direction: " + dir);

        // Mine blocks in a 3x3 pattern with depth based on level
        for (int depth = 0; depth < depthMultiplier; depth++) {
            for (int i = 0; i < blocksBroken; i++) {
                if (item.getType().isAir()) break;

                int xAdd = MINING_COORD_OFFSETS[i][0];
                int zAdd = MINING_COORD_OFFSETS[i][1];
                
                Block blockAdd;
                if (dir == BlockFace.UP || dir == BlockFace.DOWN) {
                    blockAdd = block.getLocation().clone().add(xAdd, 0, zAdd).getBlock();
                    // Apply depth in the direction player is looking
                    if (depth > 0) {
                        blockAdd = blockAdd.getRelative(dir, depth);
                    }
                } else {
                    blockAdd = block.getLocation().clone().add(isZ ? 0 : xAdd, zAdd, isZ ? xAdd : 0).getBlock();
                    // Apply depth in the direction player is looking
                    if (depth > 0) {
                        blockAdd = blockAdd.getRelative(dir, depth);
                    }
                }

                // Skip blocks that should not be mined
                if (blockAdd.equals(block) && depth == 0) continue;
                if (blockAdd.isLiquid()) continue;

                Material addType = blockAdd.getType();

                // Skip blocks harder than the initial block
                if (blockAdd.getType().getHardness() > block.getType().getHardness()) continue;

                // Some extra block checks.
                //if (addType.isInteractable() && !INTERACTABLE_BLOCKS.contains(addType)) continue;
                if (addType == Material.BEDROCK || addType == Material.END_PORTAL || addType == Material.END_PORTAL_FRAME) continue;
                if (addType == Material.OBSIDIAN && addType != block.getType()) continue;

                //player.sendMessage("§7Mining: " + blockAdd.getType() + " at depth " + depth + " coords " + blockAdd.getX() + "," + blockAdd.getY() + "," + blockAdd.getZ());
                EnchantUtils.safeBusyBreak(player, blockAdd);
            }
        }
        return true;
    }
}
