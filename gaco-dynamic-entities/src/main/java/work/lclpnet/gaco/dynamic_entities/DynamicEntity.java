package work.lclpnet.gaco.dynamic_entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A dynamic entity is an entity at a shared position, that can be different for individual players.
 * E.g. a text display may exist at a certain position, but it shows a different text for each player.
 */
public interface DynamicEntity {

    Vec3 getPosition();

    /**
     * Return the entity that should be shown for a given player.
     * @param player The player
     * @return The entity, or null if the entity shouldn't exist for the player.
     */
    @Nullable Entity getEntity(ServerPlayer player);

    void cleanup(ServerPlayer player);
}
