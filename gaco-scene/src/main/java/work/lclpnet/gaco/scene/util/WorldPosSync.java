package work.lclpnet.gaco.scene.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Vector3d;

public class WorldPosSync {

    private final Vector3d worldPos = new Vector3d(0);
    private Vec3 mcWorldPos = Vec3.ZERO;

    public Vec3 mcWorldPos() {
        return mcWorldPos;
    }

    public void update(Matrix4dc matrixWorld) {
        matrixWorld.transformPosition(worldPos.zero());

        if (worldPos.x != mcWorldPos.x || worldPos.y != mcWorldPos.y || worldPos.z != mcWorldPos.z) {
            mcWorldPos = new Vec3(worldPos.x, worldPos.y, worldPos.z);
        }
    }
}
