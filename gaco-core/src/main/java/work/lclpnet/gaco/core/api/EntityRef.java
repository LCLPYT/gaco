package work.lclpnet.gaco.core.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record EntityRef<T extends Entity>(UUID uuid, Level world, Class<T> type) implements Resolvable<@Nullable T> {

    @SuppressWarnings("unchecked")
    public EntityRef(T entity) {
        this(entity.getUUID(), entity.level(), (Class<T>) entity.getClass());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T resolve() {
        Entity entity = world.getEntity(uuid);

        if (type.isInstance(entity)) {
            return (T) entity;
        }

        return null;
    }
}
