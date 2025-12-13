package work.lclpnet.gaco.ds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Position;

public interface Collider {

    boolean collidesWith(double x, double y, double z);

    boolean collidesWith(AABB box);

    BlockPos min();

    BlockPos max();

    default boolean collidesWith(Position pos) {
        return collidesWith(pos.x(), pos.y(), pos.z());
    }

    default boolean collidesWith(BlockPos pos) {
        return collidesWith(pos.getX(), pos.getY(), pos.getZ());
    }
}
