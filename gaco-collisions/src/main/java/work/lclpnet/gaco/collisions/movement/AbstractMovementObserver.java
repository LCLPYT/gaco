package work.lclpnet.gaco.collisions.movement;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.gaco.collisions.CollisionDetector;
import work.lclpnet.gaco.collisions.CollisionInfo;
import work.lclpnet.gaco.ds.Collider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.max;

public class AbstractMovementObserver implements MovementObserver {

    private final CollisionDetector collisionDetector;
    private final Predicate<ServerPlayerEntity> predicate;
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<Collider, Consumer<ServerPlayerEntity>> regionEnter = new HashMap<>();
    private final Map<Collider, Consumer<ServerPlayerEntity>> regionLeave = new HashMap<>();
    protected final boolean useHitboxes;
    protected final double hitboxMargin;
    private BiConsumer<ServerPlayerEntity, Collider> onEnter = null, onLeave = null;

    public AbstractMovementObserver(CollisionDetector collisionDetector, Predicate<ServerPlayerEntity> predicate,
                                    boolean useHitboxes, double hitboxMargin) {
        this.collisionDetector = collisionDetector;
        this.predicate = predicate;
        this.useHitboxes = useHitboxes;
        this.hitboxMargin = max(0, hitboxMargin);
    }

    @Override
    public void setRegionEnterListener(BiConsumer<ServerPlayerEntity, Collider> onEnter) {
        this.onEnter = onEnter;
    }

    @Override
    public void setRegionLeaveListener(BiConsumer<ServerPlayerEntity, Collider> onLeave) {
        this.onLeave = onLeave;
    }

    @Override
    public void whenEntering(Collider region, Consumer<ServerPlayerEntity> action) {
        Objects.requireNonNull(region);
        Objects.requireNonNull(action);

        // make sure the collision detector knows about the region
        collisionDetector.add(region);

        regionEnter.put(region, action);
    }

    @Override
    public void whenLeaving(Collider region, Consumer<ServerPlayerEntity> action) {
        Objects.requireNonNull(region);
        Objects.requireNonNull(action);

        // make sure the collision detector knows about the region
        collisionDetector.add(region);

        regionLeave.put(region, action);
    }

    @Override
    public void removeListeners(Collider region) {
        collisionDetector.remove(region);
        regionEnter.remove(region);
        regionLeave.remove(region);
    }

    @Override
    public void clear() {
        collisionDetector.clear();
        entries.clear();
        regionEnter.clear();
        regionLeave.clear();
        onEnter = null;
        onLeave = null;
    }

    protected void updateMovement(ServerPlayerEntity player, Position pos) {
        if (useHitboxes) {
            EntityDimensions dimensions = player.getDimensions(player.getPose());
            Box box = dimensions.getBoxAt(pos.getX(), pos.getY(), pos.getZ());
            onMove(player, box.expand(hitboxMargin));
        } else {
            onMove(player, pos);
        }
    }

    private void onMove(ServerPlayerEntity player, Position pos) {
        if (!predicate.test(player)) return;

        Entry entry = entries.computeIfAbsent(player.getUuid(), uuid -> new Entry());

        collisionDetector.updateCollisions(pos, entry.current);

        processCollisions(player, entry);
    }

    private void onMove(ServerPlayerEntity player, Box box) {
        if (!predicate.test(player)) return;

        Entry entry = entries.computeIfAbsent(player.getUuid(), uuid -> new Entry());

        collisionDetector.updateCollisions(box, entry.current);

        processCollisions(player, entry);
    }

    private void processCollisions(ServerPlayerEntity player, Entry entry) {
        if (entry.last.equals(entry.current)) return;

        for (var left : entry.last.diff(entry.current)) {
            onLeave(player, left);
        }

        for (var entered : entry.current.diff(entry.last)) {
            onEnter(player, entered);
        }

        entry.last.set(entry.current);
    }

    private void onEnter(ServerPlayerEntity player, @NotNull Collider region) {
        if (onEnter != null) {
            onEnter.accept(player, region);
        }

        var action = regionEnter.get(region);

        if (action != null) {
            action.accept(player);
        }
    }

    private void onLeave(ServerPlayerEntity player, @NotNull Collider region) {
        if (onLeave != null) {
            onLeave.accept(player, region);
        }

        var action = regionLeave.get(region);

        if (action != null) {
            action.accept(player);
        }
    }

    private static class Entry {
        final CollisionInfo current = new CollisionInfo(1), last = new CollisionInfo(1);
    }
}
