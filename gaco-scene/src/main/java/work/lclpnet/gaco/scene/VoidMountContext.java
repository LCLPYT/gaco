package work.lclpnet.gaco.scene;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.core.api.Resolvable;

public final class VoidMountContext implements MountContext {

    public static final VoidMountContext INSTANCE = new VoidMountContext();

    private VoidMountContext() {}

    @Override
    public ServerLevel world() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Entity> Resolvable<@Nullable T> spawn(@Nullable T entity, Object3d origin) {
        return Resolvable.none();
    }

    @Override
    public <T extends Entity> void remove(@Nullable T entity, Object3d origin) {}

    @Override
    public boolean onThreadOrDispatch(Runnable runnable) {
        return false;  // always assume on thread
    }
}
