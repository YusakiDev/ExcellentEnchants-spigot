package su.nightexpress.excellentenchants.nms.mc_1_21_7;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R5.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;
import org.bukkit.craftbukkit.v1_21_R5.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_21_R5.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_21_R5.util.CraftNamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.api.EnchantKeys;
import su.nightexpress.excellentenchants.api.config.DistributionConfig;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.item.ItemSet;
import su.nightexpress.excellentenchants.api.item.ItemSetRegistry;
import su.nightexpress.excellentenchants.api.wrapper.EnchantCost;
import su.nightexpress.excellentenchants.api.wrapper.EnchantDefinition;
import su.nightexpress.excellentenchants.api.wrapper.EnchantDistribution;
import su.nightexpress.excellentenchants.api.wrapper.TradeType;
import com.tcoded.folialib.FoliaLib;
import su.nightexpress.excellentenchants.nms.RegistryHack;
import su.nightexpress.nightcore.NightPlugin;
import su.nightexpress.nightcore.util.Reflex;
import su.nightexpress.nightcore.util.text.NightMessage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class RegistryHack_1_21_7 implements RegistryHack {

    private static final MinecraftServer             SERVER;
    private static final MappedRegistry<Enchantment> ENCHANTS;
    private static final MappedRegistry<Item>        ITEMS;

    // Spigot obfuscated field names
    private static final String REGISTRY_FROZEN_TAGS_FIELD_SPIGOT = "j"; // frozenTags
    private static final String REGISTRY_ALL_TAGS_FIELD_SPIGOT    = "k"; // allTags
    private static final String TAG_SET_MAP_FIELD_SPIGOT          = "a"; // val$map for Spigot
    
    // Folia/Paper actual field names (not obfuscated)
    private static final String REGISTRY_FROZEN_TAGS_FIELD_FOLIA = "frozenTags"; // frozenTags
    private static final String REGISTRY_ALL_TAGS_FIELD_FOLIA    = "allTags"; // allTags
    
    private static final String TAG_SET_UNBOUND_METHOD     = "a"; // .unbound()

    static {
        SERVER = ((CraftServer) Bukkit.getServer()).getServer();
        ENCHANTS = (MappedRegistry<Enchantment>) SERVER.registryAccess().lookup(Registries.ENCHANTMENT).orElseThrow();
        ITEMS = (MappedRegistry<Item>) SERVER.registryAccess().lookup(Registries.ITEM).orElseThrow();
    }

    private final NightPlugin plugin;
    private final boolean isFolia;

    public RegistryHack_1_21_7(@NotNull NightPlugin plugin) {
        this.plugin = plugin;
        this.isFolia = new FoliaLib(plugin).isFolia();
    }

    @NotNull
    private static ResourceLocation customResourceLocation(@NotNull String value) {
        return CraftNamespacedKey.toMinecraft(EnchantKeys.custom(value));
    }

    private static <T> TagKey<T> customTagKey(@NotNull Registry<T> registry, @NotNull String name) {
        return TagKey.create(registry.key(), customResourceLocation(name));
    }

    @NotNull
    private static ResourceKey<Enchantment> customEnchantKey(@NotNull String name) {
        ResourceLocation location = customResourceLocation(name);

        return ResourceKey.create(ENCHANTS.key(), location);
    }

    @NotNull
    private static ResourceKey<Enchantment> enchantKey(@NotNull String name) {
        ResourceLocation location = ResourceLocation.parse(name);

        return ResourceKey.create(ENCHANTS.key(), location);
    }

    private static TagKey<Item> customItemsTag(@NotNull String path) {
        return TagKey.create(ITEMS.key(), customResourceLocation(path));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> Map<TagKey<T>, HolderSet.Named<T>> getFrozenTags(@NotNull MappedRegistry<T> registry) {
        String fieldName = this.isFolia ? REGISTRY_FROZEN_TAGS_FIELD_FOLIA : REGISTRY_FROZEN_TAGS_FIELD_SPIGOT;
        return (Map<TagKey<T>, HolderSet.Named<T>>) Reflex.getFieldValue(registry, fieldName);
    }

    @NotNull
    private <T> Object getAllTags(@NotNull MappedRegistry<T> registry) {
        String fieldName = this.isFolia ? REGISTRY_ALL_TAGS_FIELD_FOLIA : REGISTRY_ALL_TAGS_FIELD_SPIGOT;
        return Reflex.getFieldValue(registry, fieldName);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> Map<TagKey<T>, HolderSet.Named<T>> getTagsMap(@NotNull Object tagSet, @NotNull MappedRegistry<T> registry) {
        if (this.isFolia) {
            // For Folia/Paper: TagSet is an interface with anonymous implementations
            // We can't access the map field directly, so we use the frozenTags from registry instead
            return new HashMap<>(this.getFrozenTags(registry));
        } else {
            // For Spigot: Use reflection to get the map field from TagSet
            Object mapObj = Reflex.getFieldValue(tagSet, TAG_SET_MAP_FIELD_SPIGOT);
            if (mapObj == null) {
                this.plugin.warn("Failed to get TagSet map field, using empty map as fallback");
                return new HashMap<>();
            }
            return new HashMap<>((Map<TagKey<T>, HolderSet.Named<T>>) mapObj);
        }
    }

    @Override
    public void unfreezeRegistry() {
        System.out.println("[DEBUG] Starting unfreezeRegistry()");
        this.unfreeze(ENCHANTS);
        this.unfreeze(ITEMS);

        System.out.println("[DEBUG] Creating item sets. Total sets: " + ItemSetRegistry.getByIdMap().size());
        ItemSetRegistry.getByIdMap().forEach((id, itemSet) -> {
            System.out.println("[DEBUG] Processing item set: " + id);
            this.createItemsSet(id, itemSet);
        });
        System.out.println("[DEBUG] Finished unfreezeRegistry()");
    }

    @Override
    public void freezeRegistry() {
        this.freeze(ITEMS);
        this.freeze(ENCHANTS);
        //this.displayTags();
    }

    private <T> void unfreeze(@NotNull MappedRegistry<T> registry) {
        if (this.isFolia) {
            // For Folia/Paper: use actual field names
            Reflex.setFieldValue(registry, "frozen", false);
            Reflex.setFieldValue(registry, "unregisteredIntrusiveHolders", new IdentityHashMap<>());
        } else {
            // For Spigot: use obfuscated field names
            Reflex.setFieldValue(registry, "l", false);             // MappedRegistry#frozen
            Reflex.setFieldValue(registry, "m", new IdentityHashMap<>()); // MappedRegistry#unregisteredIntrusiveHolders
        }
    }

    private <T> void freeze(@NotNull MappedRegistry<T> registry) {
        // Get original TagSet object of the registry before unbound.
        // We MUST keep original TagSet object and only modify an inner map object inside it.
        // Otherwise it will throw an Network Error on client join because of 'broken' tags that were bound to other TagSet object.
        Object tagSet = this.getAllTags(registry);

        // Get a copy of original TagSet's tags map.
        Map<TagKey<T>, HolderSet.Named<T>> tagsMap = this.getTagsMap(tagSet, registry);
        // Get 'frozenTags' map with all tags of the registry.
        Map<TagKey<T>, HolderSet.Named<T>> frozenTags = this.getFrozenTags(registry);

        // Here we add all registered and bound vanilla tags to the 'frozenTags' map for further freeze & bind.
        // For some reason 'frozenTags' map does not contain all the tags, so some of them will be absent if not added back here
        // and result in broken gameplay features.
        tagsMap.forEach(frozenTags::putIfAbsent);

        if (!this.isFolia) {
            // For Spigot: We MUST 'unbound' the registry tags to be able to call .freeze() method
            // Otherwise it will throw an error saying tags are not bound.
            this.unbound(registry);
        }
        // For Folia: Skip unbound step as the registry system works differently

        if (!this.isFolia) {
            // For Spigot: This method will register all tags from the 'frozenTags' map and assign a new TagSet object to the 'allTags' field of registry.
            // But we MUST replace the 'allTags' field value with the original (before unbound) TagSet object to prevent Network Error for clients.
            registry.freeze();
            
            this.updateTagSetForNetworkSync(registry, tagSet, tagsMap, frozenTags);
        } else {
            // For Folia: Skip manual freeze() call but update TagSet for network sync
            System.out.println("[DEBUG] Folia: Skipping registry.freeze() but updating TagSet for network sync");
            this.updateTagSetForNetworkSync(registry, tagSet, tagsMap, frozenTags);
        }
    }

    private <T> void updateTagSetForNetworkSync(@NotNull MappedRegistry<T> registry, 
                                               @NotNull Object tagSet,
                                               @NotNull Map<TagKey<T>, HolderSet.Named<T>> tagsMap,
                                               @NotNull Map<TagKey<T>, HolderSet.Named<T>> frozenTags) {
        if (!this.isFolia) {
            // For Spigot: Complex TagSet manipulation is needed
            // Here we need to put in 'tagsMap' map of TagSet object all new/custom registered tags.
            // Otherwise it will cause Network Error because custom tags are not present in the TagSet tags map.
            frozenTags.forEach(tagsMap::putIfAbsent);

            // Update inner tags map of the TagSet object
            Reflex.setFieldValue(tagSet, TAG_SET_MAP_FIELD_SPIGOT, tagsMap);
            
            // Assign original TagSet object with modified tags map to the 'allTags' field of the registry.
            Reflex.setFieldValue(registry, REGISTRY_ALL_TAGS_FIELD_SPIGOT, tagSet);
        } else {
            // For Folia: Don't replace TagSet at all - but let's test if our tags are actually resolvable
            String registryName = registry == ITEMS ? "ITEMS" : (registry == ENCHANTS ? "ENCHANTS" : "UNKNOWN");
            System.out.println("[DEBUG] Folia: Testing tag resolution for " + registryName + " registry. FrozenTags: " + frozenTags.size());
            
            // Count custom tags that are already present
            long customTagCount = frozenTags.keySet().stream()
                .filter(tag -> tag.location().getNamespace().equals("excellentenchants"))
                .count();
            
            System.out.println("[DEBUG] " + registryName + " found " + customTagCount + " custom tags in frozenTags");
            
            // Test if our custom tags are actually resolvable through the registry
            frozenTags.keySet().stream()
                .filter(tag -> tag.location().getNamespace().equals("excellentenchants"))
                .forEach(tag -> {
                    System.out.println("[DEBUG] " + registryName + " custom tag verified in frozenTags: " + tag.location());
                    
                    // Test if the tag is actually resolvable through the registry's get() method
                    try {
                        java.util.Optional<?> result = registry.get(tag);
                        if (result.isPresent()) {
                            System.out.println("[DEBUG] " + registryName + " tag RESOLVABLE through registry.get(): " + tag.location());
                        } else {
                            System.out.println("[DEBUG] " + registryName + " tag NOT RESOLVABLE through registry.get(): " + tag.location() + " - THIS IS THE PROBLEM!");
                        }
                    } catch (Exception e) {
                        System.out.println("[DEBUG] " + registryName + " tag resolution FAILED for " + tag.location() + ": " + e.getMessage());
                    }
                });
            
            System.out.println("[DEBUG] " + registryName + ": Using vanilla Folia registry sync for custom tags (no TagSet modification)");
        }
    }

    private <T> void unbound(@NotNull MappedRegistry<T> registry) {
        if (this.isFolia) {
            // For Folia/Paper: TagSet.unbound() is a static method
            // We'll create a new unbound TagSet using the static method
            try {
                Class<?> tagSetClass = Class.forName("net.minecraft.core.MappedRegistry$TagSet");
                Method unboundMethod = tagSetClass.getMethod("unbound");
                Object unboundTagSet = unboundMethod.invoke(null);
                Reflex.setFieldValue(registry, REGISTRY_ALL_TAGS_FIELD_FOLIA, unboundTagSet);
            } catch (Exception e) {
                this.plugin.error("Failed to unbound registry tags for Folia: " + e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            // For Spigot: use reflection with obfuscated names
            Class<?> tagSetClass = Reflex.getInnerClass(MappedRegistry.class.getName(), "a");  // TagSet for Spigot
            if (tagSetClass == null) throw new IllegalStateException("TagSet class not found!");

            Method unboundMethod = Reflex.getMethod(tagSetClass, TAG_SET_UNBOUND_METHOD);
            Object unboundTagSet = Reflex.invokeMethod(unboundMethod, registry);

            Reflex.setFieldValue(registry, REGISTRY_ALL_TAGS_FIELD_SPIGOT, unboundTagSet);
        }
    }

    @Override
    public void addExclusives(@NotNull CustomEnchantment customEnchantment) {
        ResourceKey<Enchantment> enchantKey = customEnchantKey(customEnchantment.getId());
        Enchantment enchantment = ENCHANTS.getValue(enchantKey);
        if (enchantment == null) {
            this.plugin.error(customEnchantment.getId() + ": Could not set exclusive item list. Enchantment is not registered.");
            return;
        }

        TagKey<Enchantment> exclusivesKey = customTagKey(ENCHANTS, "exclusive_set/" + customEnchantment.getId());

        customEnchantment.getDefinition().getExclusiveSet().forEach(enchantId -> {
            ResourceKey<Enchantment> conflictKey = enchantKey(enchantId);
            Holder.Reference<Enchantment> reference = ENCHANTS.get(conflictKey).orElse(null);
            if (reference == null) return;

            addInTag(exclusivesKey, reference);
        });
    }

    @Override
    @Nullable
    public org.bukkit.enchantments.Enchantment registerEnchantment(@NotNull CustomEnchantment customEnchantment) {
        EnchantDefinition definition = customEnchantment.getDefinition();

        String primaryId = definition.getPrimaryItemsId();
        String supportedId = definition.getSupportedItemsId();

        ItemSet primarySet = ItemSetRegistry.getById(primaryId);
        ItemSet supportedSet = ItemSetRegistry.getById(supportedId);

        if (primarySet == null || supportedSet == null) {
            this.plugin.error("Could not register enchantment '" + customEnchantment.getId() + "' due to invalid primary/supported items sets: " + primaryId + "/" + supportedId);
            return null;
        }

        HolderSet.Named<Item> supportedItems = this.getFrozenTags(ITEMS).get(customItemsTag(supportedId));
        HolderSet.Named<Item> primaryItems = this.getFrozenTags(ITEMS).get(customItemsTag(primaryId));

        // Debug null checks
        if (supportedItems == null) {
            this.plugin.error("Supported items is null for: " + supportedId);
            return null;
        }
        if (primaryItems == null) {
            this.plugin.error("Primary items is null for: " + primaryId);
            return null;
        }

        Component display = CraftChatMessage.fromJSON(NightMessage.asJson(customEnchantment.getDisplayName()));
        int weight = definition.getWeight();
        int maxLevel = definition.getMaxLevel();
        Enchantment.Cost minCost = nmsCost(definition.getMinCost());
        Enchantment.Cost maxCost = nmsCost(definition.getMaxCost());
        int anvilCost = definition.getAnvilCost();
        EquipmentSlotGroup[] slots = nmsSlots(supportedSet);

        if (minCost == null) {
            this.plugin.error("MinCost is null for enchantment: " + customEnchantment.getId());
            return null;
        }
        if (maxCost == null) {
            this.plugin.error("MaxCost is null for enchantment: " + customEnchantment.getId());
            return null;
        }
        if (slots == null) {
            this.plugin.error("Slots is null for enchantment: " + customEnchantment.getId());
            return null;
        }

        Enchantment.EnchantmentDefinition nmsDefinition = Enchantment.definition(supportedItems, primaryItems, weight, maxLevel, minCost, maxCost, anvilCost, slots);
        HolderSet<Enchantment> exclusiveSet = this.createExclusiveSet(customEnchantment);
        DataComponentMap.Builder builder = DataComponentMap.builder();

        Enchantment enchantment = new Enchantment(display, nmsDefinition, exclusiveSet, builder.build());

        // Create a new Holder for the custom enchantment.
        Holder.Reference<Enchantment> reference = ENCHANTS.createIntrusiveHolder(enchantment);

        // Add it into Registry.
        Registry.register(ENCHANTS, customEnchantKey(customEnchantment.getId()), enchantment);

        // Now it's possible to add/remove it from vanilla tags since we have a valid, registered Reference.
        this.setupDistribution(customEnchantment, reference);

        // Return the bukkit mirror.
        return CraftEnchantment.minecraftToBukkit(enchantment);
    }

    private void setupDistribution(@NotNull CustomEnchantment customEnchantment, @NotNull Holder.Reference<Enchantment> reference) {
        boolean experimentalTrades = SERVER.getWorldData().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE);
        EnchantDistribution distribution = customEnchantment.getDistribution();

        // Any enchantment can be treasure.
        if (distribution.isTreasure()) {
            addInTag(EnchantmentTags.TREASURE, reference);
            addInTag(EnchantmentTags.DOUBLE_TRADE_PRICE, reference);
        }
        else addInTag(EnchantmentTags.NON_TREASURE, reference);

        // Any enchantment can be on random loot.
        if (distribution.isOnRandomLoot() && DistributionConfig.DISTRIBUTION_RANDOM_LOOT.get()) {
            addInTag(EnchantmentTags.ON_RANDOM_LOOT, reference);
        }

        // Only non-treasure enchantments should be on mob equipment, traded equipment and non-rebalanced trades.
        if (!distribution.isTreasure()) {
            if (distribution.isOnMobSpawnEquipment() && DistributionConfig.DISTRIBUTION_MOB_EQUIPMENT.get()) {
                addInTag(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT, reference);
            }

            if (distribution.isOnTradedEquipment() && DistributionConfig.DISTRIBUTION_TRADE_EQUIPMENT.get()) {
                addInTag(EnchantmentTags.ON_TRADED_EQUIPMENT, reference);
            }
        }

        // Any enchantment can be tradable.
        if (experimentalTrades) {
            if (distribution.isTradable() && DistributionConfig.DISTRIBUTION_TRADING.get()) {
                distribution.getTrades().forEach(tradeType -> {
                    addInTag(getTradeKey(tradeType), reference);
                });
            }
        }
        else {
            if (distribution.isTradable() && DistributionConfig.DISTRIBUTION_TRADING.get()) {
                addInTag(EnchantmentTags.TRADEABLE, reference);
            }
            else removeFromTag(EnchantmentTags.TRADEABLE, reference);
        }

        if (customEnchantment.isCurse()) {
            addInTag(EnchantmentTags.CURSE, reference);
        }
        else {
            // Only non-curse and non-treasure enchantments should go in enchanting table.
            if (!distribution.isTreasure()) {
                if (distribution.isDiscoverable() && DistributionConfig.DISTRIBUTION_ENCHANTING.get()) {
                    addInTag(EnchantmentTags.IN_ENCHANTING_TABLE, reference);
                }
                else removeFromTag(EnchantmentTags.IN_ENCHANTING_TABLE, reference);
            }
        }
    }

    private void addInTag(@NotNull TagKey<Enchantment> tagKey, @NotNull Holder.Reference<Enchantment> reference) {
        modfiyTag(ENCHANTS, tagKey, reference, List::add);
    }

    private void removeFromTag(@NotNull TagKey<Enchantment> tagKey, @NotNull Holder.Reference<Enchantment> reference) {
        modfiyTag(ENCHANTS, tagKey, reference, List::remove);
    }

    private <T> void modfiyTag(@NotNull MappedRegistry<T> registry,
                               @NotNull TagKey<T> tagKey,
                               @NotNull Holder.Reference<T> reference,
                               @NotNull BiConsumer<List<Holder<T>>, Holder.Reference<T>> consumer) {

        HolderSet.Named<T> holders = registry.get(tagKey).orElse(null);
        if (holders == null) {
            this.plugin.warn(tagKey + ": Could not modify HolderSet. HolderSet is NULL.");
            return;
        }

        List<Holder<T>> contents = new ArrayList<>(holders.stream().toList());
        consumer.accept(contents, reference);

        registry.bindTag(tagKey, contents);
    }

    private void createItemsSet(@NotNull String id, @NotNull ItemSet category) {
        TagKey<Item> tag = customItemsTag(id);
        List<Holder<Item>> holders = new ArrayList<>();

        category.getMaterials().forEach(material -> {
            ResourceLocation location = ResourceLocation.withDefaultNamespace(material);// CraftNamespacedKey.toMinecraft(material.getKey());
            Holder.Reference<Item> holder = ITEMS.get(location).orElse(null);
            if (holder == null) {
                System.out.println("[DEBUG] Could not find item holder for: " + material);
                return;
            }

            holders.add(holder);
        });

        System.out.println("[DEBUG] Creating item set '" + id + "' with " + holders.size() + " items");
        
        // Creates new tag, puts it in the 'frozenTags' map and binds holders to it.
        ITEMS.bindTag(tag, holders);
        
        // Verify the tag was created properly
        HolderSet.Named<Item> retrievedTag = this.getFrozenTags(ITEMS).get(tag);
        if (retrievedTag != null) {
            System.out.println("[DEBUG] Item set '" + id + "' created successfully with " + retrievedTag.size() + " items");
        } else {
            System.out.println("[DEBUG] ERROR: Item set '" + id + "' could not be retrieved after creation!");
        }

        //return getFrozenTags(ITEMS).get(customKey);
    }

    @NotNull
    private HolderSet.Named<Enchantment> createExclusiveSet(@NotNull CustomEnchantment customEnchantment) {
        TagKey<Enchantment> customKey = customTagKey(ENCHANTS, "exclusive_set/" + customEnchantment.getId());
        List<Holder<Enchantment>> holders = new ArrayList<>();

        // Creates new tag, puts it in the 'frozenTags' map and binds holders to it.
        ENCHANTS.bindTag(customKey, holders);

        return this.getFrozenTags(ENCHANTS).get(customKey);
    }




    @NotNull
    private static TagKey<Enchantment> getTradeKey(@NotNull TradeType tradeType) {
        return switch (tradeType) {
            case DESERT_COMMON -> EnchantmentTags.TRADES_DESERT_COMMON;
            case DESERT_SPECIAL -> EnchantmentTags.TRADES_DESERT_SPECIAL;
            case PLAINS_COMMON -> EnchantmentTags.TRADES_PLAINS_COMMON;
            case PLAINS_SPECIAL -> EnchantmentTags.TRADES_PLAINS_SPECIAL;
            case SAVANNA_COMMON -> EnchantmentTags.TRADES_SAVANNA_COMMON;
            case SAVANNA_SPECIAL -> EnchantmentTags.TRADES_SAVANNA_SPECIAL;
            case JUNGLE_COMMON -> EnchantmentTags.TRADES_JUNGLE_COMMON;
            case JUNGLE_SPECIAL -> EnchantmentTags.TRADES_JUNGLE_SPECIAL;
            case SNOW_COMMON -> EnchantmentTags.TRADES_SNOW_COMMON;
            case SNOW_SPECIAL -> EnchantmentTags.TRADES_SNOW_SPECIAL;
            case SWAMP_COMMON -> EnchantmentTags.TRADES_SWAMP_COMMON;
            case SWAMP_SPECIAL -> EnchantmentTags.TRADES_SWAMP_SPECIAL;
            case TAIGA_COMMON -> EnchantmentTags.TRADES_TAIGA_COMMON;
            case TAIGA_SPECIAL -> EnchantmentTags.TRADES_TAIGA_SPECIAL;
        };
    }

    @NotNull
    private static Enchantment.Cost nmsCost(@NotNull EnchantCost cost) {
        return new Enchantment.Cost(cost.base(), cost.perLevel());
    }

    private static EquipmentSlotGroup[] nmsSlots(@NotNull ItemSet category) {
        EquipmentSlot[] slots = category.getSlots();
        EquipmentSlotGroup[] nmsSlots = new EquipmentSlotGroup[slots.length];

        for (int index = 0; index < nmsSlots.length; index++) {
            EquipmentSlot bukkitSlot = slots[index];
            nmsSlots[index] = CraftEquipmentSlot.getNMSGroup(bukkitSlot.getGroup());
        }

        return nmsSlots;
    }
}
