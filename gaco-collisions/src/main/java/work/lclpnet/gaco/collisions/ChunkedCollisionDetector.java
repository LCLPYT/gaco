package work.lclpnet.gaco.collisions;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.gaco.ds.Collider;
import work.lclpnet.gaco.math.Vec2i;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class ChunkedCollisionDetector implements CollisionDetector {

    protected final Long2ObjectMap<Region> regions = new Long2ObjectArrayMap<>();

    @Override
    public void add(Collider collider) {
        Objects.requireNonNull(collider);

        for (Vec2i reg : iterateRegions(collider)) {
            var region = regions.computeIfAbsent(hashRegion(reg.x(), reg.z()), l -> Region.create());

            region.colliders.add(collider);
        }
    }

    @Override
    public void remove(Collider collider) {
        if (collider == null) return;

        for (Vec2i reg : iterateRegions(collider)) {
            var region = regions.computeIfAbsent(hashRegion(reg.x(), reg.z()), l -> Region.create());

            region.colliders.remove(collider);
        }
    }

    @Override
    public void clear() {
        regions.clear();
    }

    @Override
    public void updateCollisions(Position pos, CollisionInfo info) {
        info.reset();

        Region region = regions.get(hashPos(pos.getX(), pos.getZ()));

        if (region == null) return;

        region.updateCollisions(pos, info);
    }

    @Override
    public void updateCollisions(Box box, CollisionInfo info) {
        info.reset();

        BlockPos min = BlockPos.ofFloored(box.getMinPos());
        BlockPos max = BlockPos.ofFloored(box.getMaxPos());

        for (Vec2i reg : iterateRegions(min, max)) {
            Region region = regions.get(hashRegion(reg.x(), reg.z()));

            if (region == null) continue;

            region.updateCollisions(box, info);
        }
    }

    private static long hashPos(double x, double z) {
        return hashRegion(
                ChunkSectionPos.getSectionCoord(x),
                ChunkSectionPos.getSectionCoord(z)
        );
    }

    private static long hashRegion(int rx, int rz) {
        return ChunkPos.toLong(rx, rz);
    }

    public static Iterable<Vec2i> iterateRegions(Collider collider) {
        BlockPos min = collider.min(), max = collider.max();

        return iterateRegions(min, max);
    }

    public static @NotNull Iterable<Vec2i> iterateRegions(BlockPos min, BlockPos max) {
        var realMin = BlockPos.min(min, max);
        var realMax = BlockPos.max(min, max);

        int minRx = ChunkSectionPos.getSectionCoord(realMin.getX());
        int minRz = ChunkSectionPos.getSectionCoord(realMin.getZ());

        int maxRx = ChunkSectionPos.getSectionCoord(realMax.getX());
        int maxRz = ChunkSectionPos.getSectionCoord(realMax.getZ());

        Vec2i.Mutable pos = new Vec2i.Mutable(minRx, minRz);

        return () -> new Iterator<>() {
            private int rx = minRx, rz = minRz;

            @Override
            public boolean hasNext() {
                return rx <= maxRx && rz <= maxRz;
            }

            @Override
            public Vec2i next() {
                pos.set(rx, rz);

                rz++;

                if (rz > maxRz) {
                    rx++;
                    rz = minRz;
                }

                return pos;
            }
        };
    }

    protected record Region(Set<Collider> colliders) {

        public void updateCollisions(Position pos, CollisionInfo info) {
            for (Collider collider : colliders) {
                if (collider.collidesWith(pos)) {
                    info.add(collider);
                }
            }
        }

        public void updateCollisions(Box box, CollisionInfo info) {
            for (Collider collider : colliders) {
                if (collider.collidesWith(box)) {
                    info.add(collider);
                }
            }
        }

        static Region create() {
            return new Region(new ObjectArraySet<>(4));
        }
    }
}
