package work.lclpnet.gaco.dynamic_entities;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerSpecificDynamicEntity<T extends Entity> implements DynamicEntity {

    @Getter
    private final T entity;
    @Getter
    private final UUID viewerUuid;

    public PlayerSpecificDynamicEntity(T entity, UUID viewerUuid) {
        this.entity = entity;
        this.viewerUuid = viewerUuid;
    }

    @Override
    public Vec3 getPosition() {
        return entity.position();
    }

    @Override
    public @Nullable T getEntity(ServerPlayer player) {
        return player.getUUID().equals(viewerUuid) ? entity : null;
    }

    @Override
    public void cleanup(ServerPlayer player) {}
}
