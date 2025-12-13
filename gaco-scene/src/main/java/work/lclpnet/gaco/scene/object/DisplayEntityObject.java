package work.lclpnet.gaco.scene.object;

import lombok.Getter;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.core.api.Resolvable;
import work.lclpnet.gaco.scene.*;
import work.lclpnet.gaco.scene.animation.Interpolatable;
import work.lclpnet.gaco.scene.util.DisplayEntityTransformer;

import static work.lclpnet.gaco.core.util.ThreadUtil.executeOn;


public abstract class DisplayEntityObject<T extends Display> extends Object3d implements Mountable, Unmountable, Interpolatable {

    @Getter
    private final DisplayEntityTransformer transformer = new DisplayEntityTransformer();
    protected @NotNull Resolvable<T> entityRef = Resolvable.none();
    @Getter private boolean glowing = false;
    @Getter private int glowColorOverride = -1;
    @Getter private int interpolationDuration = 0;
    @Getter private int teleportDuration = 0;
    @Getter private Display.BillboardConstraints billboardMode = Display.BillboardConstraints.FIXED;

    public DisplayEntityObject(Scene scene) {
        super(scene);
    }

    protected abstract @Nullable T createDisplayEntity(MountContext ctx);

    @Override
    public void updateMatrixWorld(boolean withParent, boolean withChildren) {
        super.updateMatrixWorld(withParent, withChildren);

        entityRef.optional().ifPresent(display -> transformer.updateAndApply(display, matrixWorld));
    }

    @Override
    public void mount(MountContext ctx) {
        var display = createDisplayEntity(ctx);

        if (display == null) {
            entityRef = Resolvable.none();
            return;
        }

        configure(display);

        executeOn(ctx.world().getServer(), () -> entityRef = ctx.spawn(display, this));
    }

    protected void configure(T display) {
        transformer.updateAndApply(display, matrixWorld);

        display.setGlowColorOverride(glowColorOverride);
        display.setTransformationInterpolationDuration(interpolationDuration);
        display.setPosRotInterpolationDuration(teleportDuration);
        display.setBillboardConstraints(billboardMode);

        display.setGlowingTag(glowing);
    }

    @Override
    public void unmount(MountContext ctx) {
        removeDisplay(ctx);
    }

    @Override
    public void updateTickRate(int tickRate) {
        setInterpolationDuration(tickRate);
        setTeleportDuration(tickRate);
    }

    @Override
    protected void onDetached() {
        removeDisplay(scene.getMountContext());
    }

    public void setGlowColorOverride(int glowColorOverride) {
        this.glowColorOverride = glowColorOverride;
        entityRef.optional().ifPresent(display -> display.setGlowColorOverride(glowColorOverride));
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        entityRef.optional().ifPresent(display -> display.setGlowingTag(glowing));
    }

    public void setInterpolationDuration(int interpolationDuration) {
        this.interpolationDuration = interpolationDuration;
        entityRef.optional().ifPresent(display -> display.setTransformationInterpolationDuration(interpolationDuration));
    }

    public void setTeleportDuration(int teleportDuration) {
        this.teleportDuration = teleportDuration;
        entityRef.optional().ifPresent(display -> display.setPosRotInterpolationDuration(teleportDuration));
    }

    public void setBillboardMode(Display.BillboardConstraints billboardMode) {
        this.billboardMode = billboardMode;
        entityRef.optional().ifPresent(display -> display.setBillboardConstraints(billboardMode));
    }

    protected void removeDisplay(MountContext ctx) {
        entityRef.optional().ifPresent(entity -> ctx.remove(entity, this));
        entityRef = Resolvable.none();
    }
}
