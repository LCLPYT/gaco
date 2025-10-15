package work.lclpnet.gaco.collisions;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Position;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.gaco.ds.Collider;

import java.util.HashSet;
import java.util.Set;

public interface CollisionDetector {

    void add(Collider collider);

    void remove(Collider collider);

    void clear();

    /**
     * Finds all colliders that collide with a point.
     * @param pos The position to check collisions with.
     * @param info The {@link CollisionInfo} object that collisions will be written to.
     */
    void updateCollisions(Position pos, CollisionInfo info);

    /**
     * Finds all colliders that collide with a box.
     * @param box The box to check collisions with.
     * @param info The {@link CollisionInfo} object that collisions will be written to.
     */
    void updateCollisions(Box box, CollisionInfo info);

    @NotNull
    default Set<Collider> getCollisions(Position pos) {
        CollisionInfo info = new CollisionInfo(1);
        updateCollisions(pos, info);

        return getColliders(info);
    }

    @NotNull
    default Set<Collider> getCollisions(Box box) {
        CollisionInfo info = new CollisionInfo(1);
        updateCollisions(box, info);

        return getColliders(info);
    }

    private @NotNull Set<Collider> getColliders(CollisionInfo info) {
        Set<Collider> collisions = new HashSet<>(info.count());

        for (Collider collider : info) {
            collisions.add(collider);
        }

        return collisions;
    }
}
