package su.nightexpress.excellentenchants.registry.wrapper;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;

public class WrappedEvent<E extends Event, T extends CustomEnchantment> implements Listener, EventExecutor {

    //private final ExcellentEnchants plugin;
    private final EventPriority     priority;
    private final Class<E>          eventClass;
    private final Class<T>          enchantClass;
    private final DataGather<E, T>  dataGather;

    public WrappedEvent(@NotNull EnchantsPlugin plugin,
                        @NotNull EventPriority priority,
                        @NotNull Class<E> eventClass,
                        @NotNull Class<T> enchantClass,
                        @NotNull DataGather<E, T> dataGather) {
        //this.plugin = plugin;
        this.priority = priority;
        this.eventClass = eventClass;
        this.enchantClass = enchantClass;
        this.dataGather = dataGather;
    }

    @Override
    public void execute(@NotNull Listener listener, @NotNull Event bukkitEvent) {
        if (!this.eventClass.isAssignableFrom(bukkitEvent.getClass())) return;

        E event = this.eventClass.cast(bukkitEvent);
        LivingEntity entity = this.dataGather.getEntity(event);
        if (entity == null) return;

        //Player player = entity instanceof Player user ? user : null;

        this.dataGather.getEnchants(event, this.enchantClass, entity).forEach((item, enchants) -> {
            enchants.forEach((enchant, level) -> {
                if (!this.dataGather.checkPriority(enchant, this.priority)) return;
                if (!enchant.isAvailableToUse(entity)) return;
                if (enchant.isOutOfCharges(item)) return;
                if (this.dataGather.useEnchant(event, entity, item, enchant, level)) {
                    enchant.consumeCharges(item, level);
                }
            });
        });
    }
}
