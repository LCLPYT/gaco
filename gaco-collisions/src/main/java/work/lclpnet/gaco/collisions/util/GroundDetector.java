package work.lclpnet.gaco.collisions.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.Collection;

public class GroundDetector {

    private final ServerLevel world;
    private final double margin;

    public GroundDetector(ServerLevel world, double margin) {
        this.world = world;
        this.margin = margin;
    }

    public void collectBlocksBelow(ServerPlayer player, Collection<BlockPos> list) {
        double x = player.getX(), y = player.getY(), z = player.getZ();
        var pos = new BlockPos.MutableBlockPos();

        for (double dy = -1; dy < 0; dy += 0.5) {
            for (int dx = -1; dx < 2; dx++) {
                for (int dz = -1; dz < 2; dz++) {
                    pos.set(x + margin * dx, y + dy, z + margin * dz);

                    if (world.isEmptyBlock(pos)) continue;

                    list.add(pos.immutable());
                }
            }
        }
    }
}
