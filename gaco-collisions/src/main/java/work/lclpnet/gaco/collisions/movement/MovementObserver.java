package work.lclpnet.gaco.collisions.movement;

import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.gaco.ds.Collider;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MovementObserver {

    void setRegionEnterListener(BiConsumer<ServerPlayer, Collider> onEnter);

    void setRegionLeaveListener(BiConsumer<ServerPlayer, Collider> onLeave);

    void whenEntering(Collider region, Consumer<ServerPlayer> action);

    void whenLeaving(Collider region, Consumer<ServerPlayer> action);

    void removeListeners(Collider region);

    void clear();
}
