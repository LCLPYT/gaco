package work.lclpnet.gaco.collisions.movement;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.gaco.collisions.CollisionDetector;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.scheduler.api.TaskScheduler;

import java.util.function.Predicate;

public class TickMovementObserver extends AbstractMovementObserver {

    public TickMovementObserver(CollisionDetector collisionDetector, Predicate<ServerPlayer> predicate) {
        super(collisionDetector, predicate, false, 0);
    }

    public TickMovementObserver(CollisionDetector collisionDetector, Predicate<ServerPlayer> predicate,
                                boolean useHitboxes, double hitboxMargin) {
        super(collisionDetector, predicate, useHitboxes, hitboxMargin);
    }

    public void init(TaskScheduler scheduler, HookRegistrar hooks, MinecraftServer server) {
        TickMovementDetector detector = new TickMovementDetector(() -> PlayerLookup.all(server));
        detector.register(player -> updateMovement(player, player.position()));
        detector.init(scheduler, hooks);

        for (ServerPlayer player : PlayerLookup.all(server)) {
            updateMovement(player, player.position());
        }
    }
}
