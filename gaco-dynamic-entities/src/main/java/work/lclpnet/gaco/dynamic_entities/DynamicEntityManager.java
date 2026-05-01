package work.lclpnet.gaco.dynamic_entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.scheduler.api.TaskScheduler;
import work.lclpnet.kibu.translate.hook.LanguageChangedCallback;

import java.util.*;
import java.util.function.Predicate;

/**
 * Manages {@link DynamicEntity} tracking for online players, so that they are only visible when nearby, just like real entities.
 * When tracking a {@link DynamicEntity}, each player can possibly see a different entity.
 * The actual "real" entity that is shown to a player in range depends on the {@link DynamicEntity} implementation.
 * @implNote This class doesn't do anything until {@link #init(TaskScheduler, HookRegistrar)} is called.
 */
public class DynamicEntityManager {

    private final Map<DynamicEntity, Tracker> entities = new HashMap<>();
    private final ServerLevel world;
    private final int serverViewDistance;

    public DynamicEntityManager(ServerLevel world) {
        this.world = world;

        MinecraftServer server = world.getServer();

        if (server instanceof DedicatedServer dedicatedServer) {
            serverViewDistance = dedicatedServer.getProperties().viewDistance.get();
        } else {
            serverViewDistance = 10;
        }
    }

    /**
     * Initializes entity tracking.
     * @param scheduler The scheduler
     * @param hooks The hook registrar
     */
    public void init(TaskScheduler scheduler, HookRegistrar hooks) {
        Set<ServerGamePacketListenerImpl> invalid = new HashSet<>();

        scheduler.interval(() -> tick(invalid), 1);

        hooks.registerHook(LanguageChangedCallback.HOOK, (player, _, _) -> update(player));
    }

    public synchronized void add(DynamicEntity entity) {
        Objects.requireNonNull(entity, "Dynamic entity cannot be null");
        entities.computeIfAbsent(entity, Tracker::new);
    }

    public synchronized void remove(DynamicEntity entity) {
        var tracker = entities.remove(entity);

        if (tracker == null) return;

        tracker.destroy();
    }

    public synchronized void clear() {
        var it = entities.entrySet().iterator();

        while (it.hasNext()) {
            var entry = it.next();

            it.remove();

            entry.getValue().destroy();
        }
    }

    private synchronized void tick(Set<ServerGamePacketListenerImpl> invalid) {
        for (var entry : entities.entrySet()) {  // maybe optimize in the future, e.g. make chunk-based to iterate less
            DynamicEntity dynamic = entry.getKey();
            var tracker = entry.getValue();

            tracker.tick();

            // mark all listeners as invalid initially
            invalid.addAll(tracker.byPlayer.keySet());

            // update player tracking status
            for (ServerPlayer player : world.players()) {
                invalid.remove(player.connection);

                double viewDistanceSquared = Math.pow(getViewDistance(player) * 16, 2);

                Vec3 position = dynamic.getPosition();
                int chunkX = SectionPos.posToSectionCoord(position.x());
                int chunkZ = SectionPos.posToSectionCoord(position.z());

                boolean inRange = player.distanceToSqr(position) <= viewDistanceSquared
                        && isChunkTrackedBy(player, chunkX, chunkZ);

                if (inRange) {
                    tracker.add(player);
                } else {
                    tracker.remove(player);
                }
            }

            // cleanup invalid trackers that are no longer in the world
            for (ServerGamePacketListenerImpl listener : invalid) {
                ServerPlayer player = listener.getPlayer();

                if (player != null) {
                    tracker.remove(player);
                }
            }
        }
    }

    private synchronized void update(ServerPlayer player) {
        for (Tracker tracker : entities.values()) {
            tracker.update(player);
        }
    }

    private int getViewDistance(ServerPlayer player) {
        return Mth.clamp(player.requestedViewDistance(), 2, serverViewDistance);
    }

    // see ServerChunkLoadingManager
    private boolean isChunkTrackedBy(ServerPlayer player, int chunkX, int chunkZ) {
        return player.getChunkTrackingView().contains(chunkX, chunkZ) && !player.connection.chunkSender.isPending(ChunkPos.pack(chunkX, chunkZ));
    }

    /** A per-player tracking handler for a DynamicEntity instance */
    private class Tracker {

        private final DynamicEntity dynamic;

        /** Buffer for players who no longer track the entity and should be removed by ::tick */
        private final List<ServerPlayer> removal = new ArrayList<>();

        /** Each player who tracks the dynamic entity is assigned a possibly shared Entity */
        private final Map<ServerGamePacketListenerImpl, Entity> byPlayer = new HashMap<>();

        /** Each real entity is tracked by at least one player. Used to determine the players to which the associated tracker entry sends data. */
        private final Map<Entity, Set<ServerGamePacketListenerImpl>> byEntity = new HashMap<>();

        /** Each entity has an associated tracker that gets updated during ::tick */
        private final Map<Entity, ServerEntity> trackerEntries = new HashMap<>();

        private Tracker(DynamicEntity dynamic) {
            this.dynamic = dynamic;
        }

        public void tick() {
            // gather entries where the entity was removed
            for (var entry : byPlayer.entrySet()) {
                Entity entity = entry.getValue();

                if (entity.isRemoved()) {
                    removal.add(entry.getKey().player);
                }
            }

            // actually remove players from this tracker
            for (var player : removal) {
                remove(player);
            }

            removal.clear();

            // tick entries
            for (ServerEntity entry : trackerEntries.values()) {
                entry.sendChanges();
            }
        }

        public synchronized void destroy() {
            for (var entry : byPlayer.entrySet()) {
                ServerPlayer player = entry.getKey().getPlayer();

                if (player == null) continue;

                removeEntityForPlayer(player, entry.getValue());
            }

            byPlayer.clear();
            byEntity.clear();
            trackerEntries.clear();
        }

        public synchronized void add(ServerPlayer player) {
            if (byPlayer.containsKey(player.connection)) return;

            Entity entity = dynamic.getEntity(player);

            if (entity == null || entity.isRemoved()) return;

            ServerEntity trackerEntry = getTrackerEntry(player, entity);

            byPlayer.put(player.connection, entity);

            trackerEntry.addPairing(player);
        }

        public synchronized void remove(ServerPlayer player) {
            var entity = byPlayer.remove(player.connection);

            if (entity == null) return;

            removeEntityForPlayer(player, entity);
            removeListener(player, entity);
        }

        private void removeEntityForPlayer(ServerPlayer player, Entity entity) {
            if (!player.hasDisconnected() && player.level() == entity.level()) {
                ServerEntity trackerEntry = trackerEntries.get(entity);

                if (trackerEntry != null) {
                    trackerEntry.removePairing(player);
                }
            }

            dynamic.cleanup(player);
        }

        private void removeListener(ServerPlayer player, Entity entity) {
            var listeners = byEntity.get(entity);

            if (listeners == null) return;

            listeners.remove(player.connection);

            if (!listeners.isEmpty()) return;

            // nobody tracks the entity; cleanup
            trackerEntries.remove(entity);
            byEntity.remove(entity);
        }

        public synchronized void update(ServerPlayer player) {
            Entity current = byPlayer.get(player.connection);

            if (current == null) return;

            Entity entity = dynamic.getEntity(player);

            if (Objects.equals(current, entity)) return;

            remove(player);
            add(player);
        }

        private ServerEntity getTrackerEntry(ServerPlayer player, Entity entity) {
            var listeners = byEntity.computeIfAbsent(entity, e -> new HashSet<>());
            listeners.add(player.connection);

            return trackerEntries.computeIfAbsent(entity, e -> {
                var type = e.getType();

                // use internal Minecraft class that is normally used for syncing the entity
                var sender = new ServerEntity.Synchronizer() {
                    @Override
                    public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
                        for (ServerGamePacketListenerImpl tracker : listeners) {
                            tracker.send(packet);
                        }
                    }

                    @Override
                    public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
                        sendToTrackingPlayers(packet);
                    }

                    @Override
                    public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) {
                        for (ServerGamePacketListenerImpl tracker : listeners) {
                            ServerPlayer p = tracker.getPlayer();

                            if (p != null && predicate.test(p)) {
                                tracker.send(packet);
                            }
                        }
                    }
                };

                return new ServerEntity(world, e, type.updateInterval(), type.trackDeltas(), sender);
            });
        }
    }
}
