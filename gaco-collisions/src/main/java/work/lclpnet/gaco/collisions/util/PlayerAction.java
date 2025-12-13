package work.lclpnet.gaco.collisions.util;

import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface PlayerAction {

    void act(ServerPlayer player);

    static Hook<PlayerAction> createHook() {
        return HookFactory.createArrayBacked(PlayerAction.class, callbacks -> player -> {
            for (PlayerAction callback : callbacks) {
                callback.act(player);
            }
        });
    }
}
