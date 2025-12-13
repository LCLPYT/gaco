package work.lclpnet.gaco.scene;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.core.api.Resolvable;
import work.lclpnet.gaco.core.util.ThreadUtil;

public interface MountContext {

    ServerLevel world();

    <T extends Entity> Resolvable<@Nullable T> spawn(@Nullable T entity, Object3d origin);

    <T extends Entity> void remove(@Nullable T entity, Object3d origin);

    /**
     * Checks whether currently running on the given {@link BlockableEventLoop} thread, in which case false is returned.
     * Otherwise, the given action is dispatched to the given {@link BlockableEventLoop}, in which case true is returned and the caller should prevent further execution.
     * @param runnable The (reference to an) action to be executed.
     * @return True, if currently running off-thread and whether the runnable was thereby dispatched to the given {@link BlockableEventLoop}, false if running of thread and nothing was dispatched.
     */
    default boolean onThreadOrDispatch(Runnable runnable) {
        return ThreadUtil.onThreadOrDispatch(world().getServer(), runnable);
    }
}
