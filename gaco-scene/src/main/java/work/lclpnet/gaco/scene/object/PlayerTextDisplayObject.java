package work.lclpnet.gaco.scene.object;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.core.api.Resolvable;
import work.lclpnet.gaco.dynamic_entities.DynamicEntity;
import work.lclpnet.gaco.scene.Scene;
import work.lclpnet.gaco.scene.util.WorldPosSync;

import java.util.UUID;

/**
 * A {@link TextDisplayObject} that is only visible to one player.
 */
public class PlayerTextDisplayObject extends TextDisplayObject implements DynamicEntity {

    private final Resolvable<ServerPlayer> playerRef;
    private final WorldPosSync posSync = new WorldPosSync();

    public PlayerTextDisplayObject(Scene scene, Component text, ServerPlayer player) {
        super(scene, text);

        UUID uuid = player.getUUID();
        PlayerList manager = player.level().getServer().getPlayerList();

        this.playerRef = () -> manager.getPlayer(uuid);
    }

    @Override
    public void updateMatrixWorld(boolean withParent, boolean withChildren) {
        super.updateMatrixWorld(withParent, withChildren);

        posSync.update(matrixWorld);
    }

    @Override
    public Vec3 getPosition() {
        return posSync.mcWorldPos();
    }

    @Override
    public @Nullable Entity getEntity(ServerPlayer player) {
        ServerPlayer owner = playerRef.resolve();

        if (owner == null || owner != player) return null;

        return entityRef.resolve();
    }

    @Override
    public void cleanup(ServerPlayer player) {
        // no need to clean anything, as the entity will be unreferenced when the object is dismounted
    }
}
