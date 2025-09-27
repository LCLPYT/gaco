package work.lclpnet.gaco.scene.object;

import net.minecraft.entity.Entity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
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

    private final Resolvable<ServerPlayerEntity> playerRef;
    private final WorldPosSync posSync = new WorldPosSync();

    public PlayerTextDisplayObject(Scene scene, Text text, ServerPlayerEntity player) {
        super(scene, text);

        UUID uuid = player.getUuid();
        PlayerManager manager = player.getWorld().getServer().getPlayerManager();

        this.playerRef = () -> manager.getPlayer(uuid);
    }

    @Override
    public void updateMatrixWorld(boolean withParent, boolean withChildren) {
        super.updateMatrixWorld(withParent, withChildren);

        posSync.update(matrixWorld);
    }

    @Override
    public Vec3d getPosition() {
        return posSync.mcWorldPos();
    }

    @Override
    public @Nullable Entity getEntity(ServerPlayerEntity player) {
        ServerPlayerEntity owner = playerRef.resolve();

        if (owner == null || owner != player) return null;

        return entityRef.resolve();
    }

    @Override
    public void cleanup(ServerPlayerEntity player) {
        // no need to clean anything, as the entity will be unreferenced when the object is dismounted
    }
}
