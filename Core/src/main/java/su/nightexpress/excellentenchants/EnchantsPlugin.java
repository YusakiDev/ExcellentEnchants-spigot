package su.nightexpress.excellentenchants;

import org.bukkit.entity.Item;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.api.ConfigBridge;
import su.nightexpress.excellentenchants.command.BaseCommands;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.config.Keys;
import su.nightexpress.excellentenchants.config.Lang;
import su.nightexpress.excellentenchants.config.Perms;
import su.nightexpress.excellentenchants.enchantment.EnchantManager;
import su.nightexpress.excellentenchants.hook.HookPlugin;
import su.nightexpress.excellentenchants.hook.impl.PacketEventsHook;
import su.nightexpress.excellentenchants.hook.impl.PlaceholderHook;
import su.nightexpress.excellentenchants.hook.impl.ProtocolLibHook;
import su.nightexpress.excellentenchants.lib.folialib.FoliaLibWrapper;
import su.nightexpress.excellentenchants.nms.EnchantNMS;
import su.nightexpress.excellentenchants.nms.Internal_1_21_3;
import su.nightexpress.excellentenchants.nms.Internal_1_21_4;
import su.nightexpress.excellentenchants.rarity.RarityManager;
import su.nightexpress.excellentenchants.registry.EnchantRegistry;
import su.nightexpress.nightcore.NightPlugin;
import su.nightexpress.nightcore.command.experimental.ImprovedCommands;
import su.nightexpress.nightcore.config.PluginDetails;
import su.nightexpress.nightcore.util.Plugins;
import su.nightexpress.nightcore.util.Version;

public class EnchantsPlugin extends NightPlugin implements ImprovedCommands {

    private EnchantRegistry registry;
    private RarityManager   rarityManager;
    private EnchantManager  enchantManager;
    private EnchantNMS      enchantNMS;
    private FoliaLibWrapper foliaLib;

    @Override
    public void onLoad() {
        super.onLoad();
        this.registry = new EnchantRegistry(this);
        this.foliaLib = new FoliaLibWrapper(this);
    }

    @Override
    @NotNull
    protected PluginDetails getDefaultDetails() {
        return PluginDetails.create("Enchants", new String[]{"eenchants", "excellentenchants"})
            .setConfigClass(Config.class)
            .setLangClass(Lang.class)
            .setPermissionsClass(Perms.class);
    }

    @Override
    public void enable() {
        if (!this.loadInternals()) {
            this.error("Unsupported server version!");
            this.getPluginManager().disablePlugin(this);
            return;
        }

        this.loadAPI();
        this.loadCommands();

        this.rarityManager = new RarityManager(this);
        this.rarityManager.setup();

        this.registry.setup();

        this.enchantManager = new EnchantManager(this);
        this.enchantManager.setup();

        this.loadHooks();
    }

    @Override
    public void disable() {
        if (Plugins.hasPlaceholderAPI()) {
            PlaceholderHook.shutdown();
        }

        if (this.enchantManager != null) this.enchantManager.shutdown();
        if (this.rarityManager != null) this.rarityManager.shutdown();

        this.registry.shutdown();
        Keys.clear();
    }

    private boolean loadInternals() {
        this.enchantNMS = switch (Version.getCurrent()) {
            case MC_1_21 -> new Internal_1_21(this);
            case MC_1_21_3 -> new Internal_1_21_3(this);
            case MC_1_21_4 -> new Internal_1_21_4(this);
            default -> null;
        };

        return this.enchantNMS != null;
    }

    private void loadCommands() {
        BaseCommands.load(this);
    }

    private void loadAPI() {
        EnchantsAPI.load(this);
        Keys.loadKeys(this);

        ConfigBridge.setEnchantsTickInterval(Config.CORE_PASSIVE_ENCHANTS_TRIGGER_INTERVAL.get().intValue());
        ConfigBridge.setGlobalDistEnchanting(Config.ENCHANTMENTS_GLOBAL_DIST_ENCHANTING.get());
        ConfigBridge.setGlobalDistTrading(Config.ENCHANTMENTS_GLOBAL_DIST_TRADING.get());
        ConfigBridge.setGlobalDistMobEquipment(Config.ENCHANTMENTS_GLOBAL_DIST_MOB_EQUIPMENT.get());
        ConfigBridge.setGlobalDistTradeEquipment(Config.ENCHANTMENTS_GLOBAL_DIST_TRADE_EQUIPMENT.get());
        ConfigBridge.setGlobalDistRandomLoot(Config.ENCHANTMENTS_GLOBAL_DIST_RANDOM_LOOT.get());
    }

    private void loadHooks() {
        if (Config.isDescriptionEnabled()) {
            if (Plugins.isInstalled(HookPlugin.PACKET_EVENTS)) {
                PacketEventsHook.setup(this);
            }
            else if (Plugins.isInstalled(HookPlugin.PROTOCOL_LIB)) {
                ProtocolLibHook.setup(this);
            }
            else {
                this.warn("You need to install " + HookPlugin.PACKET_EVENTS + " or " + HookPlugin.PROTOCOL_LIB + " for enchantment description to work.");
            }
        }

        if (Plugins.hasPlaceholderAPI()) {
            PlaceholderHook.setup(this);
        }
    }

    public void populateResource(@NotNull BlockDropItemEvent event, @NotNull ItemStack itemStack) {
        Item item = this.getEnchantNMS().popResource(event.getBlock(), itemStack);
        event.getItems().add(item);
    }

    @NotNull
    public EnchantRegistry getRegistry() {
        return registry;
    }

    @NotNull
    public RarityManager getRarityManager() {
        return rarityManager;
    }

    @NotNull
    public EnchantManager getEnchantManager() {
        return this.enchantManager;
    }

    @NotNull
    public EnchantNMS getEnchantNMS() {
        return enchantNMS;
    }

    @NotNull
    public FoliaLibWrapper getFoliaLib() {
        return this.foliaLib;
    }
}
