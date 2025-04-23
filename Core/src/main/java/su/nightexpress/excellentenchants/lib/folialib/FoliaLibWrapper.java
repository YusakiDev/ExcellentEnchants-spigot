package su.nightexpress.excellentenchants.lib.folialib;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FoliaLibWrapper {
    private final FoliaLib foliaLib;
    private final JavaPlugin plugin;

    public FoliaLibWrapper(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.foliaLib = new FoliaLib(plugin);
    }

    /**
     * Checks if the server is running on Folia
     * @return true if running on Folia, false otherwise
     */
    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    public void runNextTick(@NotNull Consumer<WrappedTask> task) {
        foliaLib.getScheduler().runNextTick(task);
    }

    public void runAtEntity(@NotNull Entity entity, @NotNull Consumer<WrappedTask> task) {
        foliaLib.getScheduler().runAtEntity(entity, task);
    }

    public void runAtLocation(@NotNull Location location, @NotNull Consumer<WrappedTask> task) {
        foliaLib.getScheduler().runAtLocation(location, task);
    }

    public void runTimer(@NotNull Consumer<WrappedTask> task, long delay, long period) {
        foliaLib.getScheduler().runTimer(task, delay, period);
    }

    public void runTimerAsync(@NotNull Consumer<WrappedTask> task, long delay, long period) {
        foliaLib.getScheduler().runTimerAsync(task, delay, period);
    }

    public void runLater(@NotNull Consumer<WrappedTask> task, long delay) {
        foliaLib.getScheduler().runLater(task, delay);
    }

    public void runLaterAsync(@NotNull Consumer<WrappedTask> task, long delay) {
        foliaLib.getScheduler().runLaterAsync(task, delay);
    }
} 