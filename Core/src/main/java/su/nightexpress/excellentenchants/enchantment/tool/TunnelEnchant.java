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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TunnelEnchant extends GameEnchantment implements MiningEnchant {

    // X and Z offsets for each block AoE mined
    private static final int[][] MINING_COORD_OFFSETS = new int[][]{{0, 0}, {0, -1}, {-1, 0}, {0, 1}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1},};
    private static final Set<Material> INTERACTABLE_BLOCKS = new HashSet<>();
    private static boolean isProcessingTunnel = false;

    static {
        INTERACTABLE_BLOCKS.add(Material.REDSTONE_ORE);
        INTERACTABLE_BLOCKS.add(Material.DEEPSLATE_REDSTONE_ORE);
    }

    private boolean disableOnSneak;

    public TunnelEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file, @NotNull EnchantData data) {
        super(plugin, file, data);
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
        if (isProcessingTunnel) return false;

        Block block = event.getBlock();
        
        // Get the block face that was hit by analyzing player's looking direction
        BlockFace hitFace = getHitBlockFace(player, block);
        if (hitFace == null) return false;
        
        // Debug: Show which face was detected

        // Calculate depth based on enchantment level
        int depthMultiplier = level;
        
        // Collect all blocks to be mined first
        Set<Block> blocksToMine = new LinkedHashSet<>();
        
        // Always mine in 3x3 pattern
        int blocksBroken = 9;

        // Collect blocks in a 3x3 pattern with depth based on level
        for (int depth = 0; depth < depthMultiplier; depth++) {
            for (int i = 0; i < blocksBroken; i++) {
                if (item.getType().isAir()) break;

                int xOffset = MINING_COORD_OFFSETS[i][0];
                int zOffset = MINING_COORD_OFFSETS[i][1];
                
                Block blockAdd = get3x3BlockPosition(block, hitFace, xOffset, zOffset, depth);
                if (blockAdd == null) continue;


                // Skip blocks that should not be mined
                if (blockAdd.equals(block) && depth == 0) continue;
                if (blockAdd.isLiquid()) continue;

                Material addType = blockAdd.getType();
                
                // Skip air blocks
                if (addType == Material.AIR) continue;

                // Skip blocks harder than the initial block
                if (blockAdd.getType().getHardness() > block.getType().getHardness()) continue;

                // Some extra block checks.
                if (addType == Material.BEDROCK || addType == Material.END_PORTAL || addType == Material.END_PORTAL_FRAME) continue;
                if (addType == Material.OBSIDIAN && addType != block.getType()) continue;

                blocksToMine.add(blockAdd);
            }
        }

        // Break all collected blocks using the plugin's async scheduler
        EnchantsPlugin plugin = EnchantsPlugin.getPlugin(EnchantsPlugin.class);
        plugin.getFoliaLib().getScheduler().runAtEntity(player, (task) -> {
            isProcessingTunnel = true;
            try {
                blocksToMine.forEach(blockToMine -> {
                    player.breakBlock(blockToMine);
                });
            } finally {
                isProcessingTunnel = false;
            }
        });
        return true;
    }
    
    private BlockFace getHitBlockFace(Player player, Block block) {
        // Get the direction the player is looking
        var location = player.getEyeLocation();
        var direction = location.getDirection();
        
        // Calculate which face was most likely hit based on player's look direction
        double x = direction.getX();
        double y = direction.getY();
        double z = direction.getZ();
        
        // Find the axis with the largest absolute value
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);
        
        if (absY > absX && absY > absZ) {
            return y > 0 ? BlockFace.UP : BlockFace.DOWN;
        } else if (absX > absZ) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }
    
    private Block get3x3BlockPosition(Block centerBlock, BlockFace hitFace, int xOffset, int zOffset, int depth) {
        var location = centerBlock.getLocation();
        
        switch (hitFace) {
            case UP:
            case DOWN:
                // For vertical faces, 3x3 is in the horizontal plane (X-Z)
                location.add(xOffset, 0, zOffset);
                // Apply depth in the Y direction
                if (depth > 0) {
                    location.add(0, hitFace == BlockFace.UP ? depth : -depth, 0);
                }
                break;
                
            case NORTH:
            case SOUTH:
                // For north/south faces, 3x3 is in the X-Y plane
                location.add(xOffset, zOffset, 0);
                // Apply depth in the Z direction
                if (depth > 0) {
                    location.add(0, 0, hitFace == BlockFace.NORTH ? -depth : depth);
                }
                break;
                
            case EAST:
            case WEST:
                // For east/west faces, 3x3 is in the Z-Y plane
                location.add(0, zOffset, xOffset);
                // Apply depth in the X direction
                if (depth > 0) {
                    location.add(hitFace == BlockFace.EAST ? depth : -depth, 0, 0);
                }
                break;
                
            default:
                return null;
        }
        
        return location.getBlock();
    }
}
