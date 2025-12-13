package work.lclpnet.gaco.collisions.movement;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.gaco.collisions.CollisionDetector;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.hook.player.PlayerMoveCallback;

import java.util.function.Predicate;

public class PlayerMovementObserver extends AbstractMovementObserver {

    public PlayerMovementObserver(CollisionDetector collisionDetector, Predicate<ServerPlayer> predicate) {
        super(collisionDetector, predicate, false, 0);
    }

    public PlayerMovementObserver(CollisionDetector collisionDetector, Predicate<ServerPlayer> predicate,
                                  boolean useHitboxes, double hitboxMargin) {
        super(collisionDetector, predicate, useHitboxes, hitboxMargin);
    }

    public void init(HookRegistrar registrar, MinecraftServer server) {
        registrar.registerHook(PlayerMoveCallback.HOOK, (player, from, to) -> {
            updateMovement(player, to);
            return false;
        });

        for (ServerPlayer player : PlayerLookup.all(server)) {
            updateMovement(player, player.position());
        }
    }
}
