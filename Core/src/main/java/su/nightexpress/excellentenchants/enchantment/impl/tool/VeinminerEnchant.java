package su.nightexpress.excellentenchants.enchantment.impl.tool;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.TradeType;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockBreakEnchant;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDefinition;
import su.nightexpress.excellentenchants.enchantment.impl.EnchantDistribution;
import su.nightexpress.excellentenchants.enchantment.impl.GameEnchantment;
import su.nightexpress.excellentenchants.rarity.EnchantRarity;
import su.nightexpress.excellentenchants.util.ItemCategories;
import su.nightexpress.excellentenchants.util.EnchantUtils;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.Lists;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static su.nightexpress.excellentenchants.Placeholders.GENERIC_AMOUNT;

public class VeinminerEnchant extends GameEnchantment implements BlockBreakEnchant {

    public static final String ID = "vein_miner";

    private static final BlockFace[] AREA = {
        BlockFace.UP, BlockFace.DOWN, BlockFace.EAST,
        BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH
    };

    private Modifier      blocksLimit;
    private Set<Material> affectedBlocks;
    private boolean       disableOnCrouch;

    public VeinminerEnchant(@NotNull EnchantsPlugin plugin, @NotNull File file) {
        super(plugin, file, definition(), EnchantDistribution.regular(TradeType.PLAINS_SPECIAL));
    }

    @NotNull
    private static EnchantDefinition definition() {
        return EnchantDefinition.create(
            Lists.newList("Mines up to " + GENERIC_AMOUNT + " blocks of the ore vein at once."),
            EnchantRarity.LEGENDARY,
            3,
            ItemCategories.TOOL,
            ItemCategories.PICKAXE,
            Lists.newSet(BlastMiningEnchant.ID, TunnelEnchant.ID)
        );
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.disableOnCrouch = ConfigValue.create("Settings.Disable_On_Crouch",
            true,
            "Sets whether or not enchantment will have no effect when crouching."
        ).read(config);

        this.blocksLimit = Modifier.read(config, "Settings.Blocks.Limit",
            Modifier.add(4, 1, 1, 16),
            "Max. possible amount of blocks to be mined at the same time.");

        this.affectedBlocks = ConfigValue.forSet("Settings.Blocks.List",
            BukkitThing::getMaterial,
            (cfg, path, set) -> cfg.set(path, set.stream().map(Enum::name).toList()),
            () -> {
                Set<Material> set = new HashSet<>();
                set.addAll(Tag.COAL_ORES.getValues());
                set.addAll(Tag.COPPER_ORES.getValues());
                set.addAll(Tag.DIAMOND_ORES.getValues());
                set.addAll(Tag.EMERALD_ORES.getValues());
                set.addAll(Tag.GOLD_ORES.getValues());
                set.addAll(Tag.IRON_ORES.getValues());
                set.addAll(Tag.LAPIS_ORES.getValues());
                set.addAll(Tag.REDSTONE_ORES.getValues());
                set.add(Material.NETHER_GOLD_ORE);
                set.add(Material.NETHER_QUARTZ_ORE);
                return set;
            },
            "List of blocks affected by this enchantment."
        ).read(config);

        this.addPlaceholder(GENERIC_AMOUNT, level -> String.valueOf(this.getBlocksLimit(level)));
    }

    @NotNull
    public Set<Material> getAffectedBlocks() {
        return this.affectedBlocks;
    }

    public int getBlocksLimit(int level) {
        return (int) this.blocksLimit.getValue(level);
    }

    @NotNull
    private Set<Block> getNearby(@NotNull Block block) {
        return Stream.of(AREA).map(block::getRelative)
            .filter(blockAdded -> blockAdded.getType() == block.getType()).collect(Collectors.toSet());
    }

    private void vein(@NotNull Player player, @NotNull Block source, int level) {
        Set<Block> ores = new HashSet<>();
        Set<Block> prepare = new HashSet<>(this.getNearby(source));

        int limit = Math.min(this.getBlocksLimit(level), 30);
        if (limit < 0) return;

        while (ores.addAll(prepare) && ores.size() < limit) {
            Set<Block> nearby = new HashSet<>();
            prepare.forEach(prepared -> nearby.addAll(this.getNearby(prepared)));
            prepare.clear();
            prepare.addAll(nearby);
        }
        ores.remove(source);
        ores.forEach(ore -> EnchantUtils.safeBusyBreak(player, ore));
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity entity, @NotNull ItemStack tool, int level) {
        if (!(entity instanceof Player player)) return false;
        if (EnchantUtils.isBusy()) return false;
        if (this.disableOnCrouch && player.isSneaking()) return false;

        Block block = event.getBlock();
        if (block.getDrops(tool, player).isEmpty()) return false;
        if (!this.getAffectedBlocks().contains(block.getType())) return false;

        this.vein(player, block, level);
        return true;
    }
}
