package work.lclpnet.gaco.scene.physics;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import work.lclpnet.gaco.scene.*;
import work.lclpnet.gaco.scene.animation.Animatable;
import work.lclpnet.gaco.scene.animation.AnimationContext;
import work.lclpnet.gaco.scene.object.BlockDisplayObject;
import work.lclpnet.kibu.physics.impl.bullet.collision.body.shape.MinecraftShape;
import work.lclpnet.kibu.physics.impl.bullet.collision.space.MinecraftSpace;
import work.lclpnet.kibu.physics.impl.bullet.thread.PhysicsThread;
import work.lclpnet.kibu.physics.impl.util.Frame;
import work.lclpnet.kibu.physics.util.BlockPhysics;

import static work.lclpnet.gaco.core.util.ThreadUtil.executeOn;
import static work.lclpnet.gaco.core.util.ThreadUtil.forceThread;
import static work.lclpnet.kibu.physics.impl.bullet.math.Convert.toMinecraft;

public class PhysicsBlockDisplayObject extends Object3d
        implements ScenePhysicsElement, Mountable, Unmountable, Animatable {

    private final com.jme3.math.Vector3f storedPosition = new com.jme3.math.Vector3f();
    private final Quaternionf storedRotation = new Quaternionf();
    private final Quaternion storedJmeRotation = new Quaternion();
    protected final SceneRigidBody rigidBody;
    private final BlockDisplayObject blockDisplay;
    protected final ServerLevel world;

    public PhysicsBlockDisplayObject(Scene scene, BlockState state, ServerLevel world) {
        super(scene);
        this.world = world;

        blockDisplay = new BlockDisplayObject(scene, state);
        blockDisplay.position.set(-0.5f);
        addChild(blockDisplay);

        forcePhysicsThread();

        rigidBody = initRigidBody(world);
    }

    protected SceneRigidBody initRigidBody(ServerLevel world) {
        forcePhysicsThread();

        var rigidBody = new SceneRigidBody(this, world);

        updateRigidBody(rigidBody);

        return rigidBody;
    }

    public void updateRigidBody(SceneRigidBody rigidBody) {
        forcePhysicsThread();

        BlockState state = blockDisplay.getBlockState();

        rigidBody.setMass(BlockPhysics.getMass(state));
        rigidBody.setBuoyancyType(BlockPhysics.getBuoyancyType(state));
        rigidBody.setCollisionShape(this.createShape());
    }

    public final void forcePhysicsThread() {
        forceThread(PhysicsThread.get(world));
    }

    @Override
    public void mount(MountContext ctx) {
        addPhysics(ctx.world());
    }

    @Override
    public void unmount(MountContext ctx) {
        removePhysics(ctx.world());
    }

    public void addPhysics(ServerLevel world) {
        executePhysics(() -> MinecraftSpace.get(world).addCollisionObject(rigidBody));
    }

    public void removePhysics(ServerLevel world) {
        executePhysics(() -> MinecraftSpace.get(world).removeCollisionObject(rigidBody));
    }

    public void setBlockState(BlockState state) {
        blockDisplay.setBlockState(state);

        executePhysics(() -> updateRigidBody(rigidBody));
    }

    public final void executePhysics(Runnable runnable) {
        executeOn(PhysicsThread.get(world), runnable);
    }

    public BlockState getBlockState() {
        return blockDisplay.getBlockState();
    }

    @Override
    @NotNull
    public SceneRigidBody getRigidBody() {
        return rigidBody;
    }

    @Override
    public MinecraftShape.Convex createShape() {
        forcePhysicsThread();

        MinecraftShape.Convex shape = BlockPhysics.getShape(blockDisplay.getBlockState(), world);
        shape.setScale(new Vector3f((float) scale.x, (float) scale.y, (float) scale.z));

        return shape;
    }

    @Override
    public void updateAnimation(double dt, AnimationContext ctx) {
        Frame frame = rigidBody.getFrame();
        var pos = frame.getLocation(storedPosition, 1f);

        this.setWorldPosition(pos.x, pos.y, pos.z);
        rotation.set(toMinecraft(frame.getRotation(storedJmeRotation, 0f), storedRotation));
    }

    @Override
    protected void onDetached() {
        removePhysics(world);
    }
}
