package work.lclpnet.gaco.scene;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.core.api.EntityRef;
import work.lclpnet.gaco.core.api.Resolvable;

public record ServerWorldMountContext(ServerLevel world) implements MountContext {

    @Override
    public <T extends Entity> Resolvable<@Nullable T> spawn(@Nullable T entity, Object3d origin) {
        if (entity == null || !world.addFreshEntity(entity)) {
            return Resolvable.none();
        }

        return new EntityRef<>(entity);
    }

    @Override
    public <T extends Entity> void remove(@Nullable T entity, Object3d origin) {
        if (entity != null) {
            entity.discard();
        }
    }
}
