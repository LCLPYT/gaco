package work.lclpnet.gaco.scene.physics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.gaco.core.api.EntityRef;
import work.lclpnet.kibu.physics.api.PhysicsElement;
import work.lclpnet.kibu.physics.impl.bullet.collision.body.shape.MinecraftShape;

public class EntityRefPhysicsElement implements PhysicsElement<EntityRef<?>> {

    private final EntityRef<?> ref;
    private final EntityRefRigidBody rigidBody;

    public EntityRefPhysicsElement(EntityRef<?> ref) {
        this.ref = ref;
        rigidBody = new EntityRefRigidBody(this, ref.require().level());
    }

    @Override
    public @NotNull EntityRefRigidBody getRigidBody() {
        return rigidBody;
    }

    @Override
    public MinecraftShape.Convex createShape() {
        Entity entity = ref.require();
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());

        final AABB box = dimensions.makeBoundingBox(Vec3.ZERO);

        return MinecraftShape.convex(box);
    }

    @Override
    public EntityRef<?> cast() {
        return ref;
    }
}
