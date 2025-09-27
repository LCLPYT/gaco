package work.lclpnet.gaco.scene.physics;

import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.scene.Object3d;
import work.lclpnet.kibu.physics.api.PhysicsElement;

public interface ScenePhysicsElement extends PhysicsElement<Object3d> {

    @Override
    @Nullable SceneRigidBody getRigidBody();
}
