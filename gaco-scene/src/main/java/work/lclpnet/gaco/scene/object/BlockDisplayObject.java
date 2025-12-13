package work.lclpnet.gaco.scene.object;

import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import work.lclpnet.gaco.scene.MountContext;
import work.lclpnet.gaco.scene.Scene;

@Getter
public class BlockDisplayObject extends DisplayEntityObject<Display.BlockDisplay> {

    private BlockState blockState;

    public BlockDisplayObject(Scene scene, BlockState blockState) {
        super(scene);
        this.blockState = blockState;
    }

    @Override
    protected Display.BlockDisplay createDisplayEntity(MountContext ctx) {
        return new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, ctx.world());
    }

    @Override
    protected void configure(Display.BlockDisplay display) {
        super.configure(display);

        display.setBlockState(blockState);
    }

    @Override
    public BlockDisplayObject deepCopy(Scene scene) {
        var copy = new BlockDisplayObject(scene, blockState);

        copy.deepCopy(this);

        return copy;
    }

    public void setBlockState(BlockState state) {
        this.blockState = state;
        entityRef.optional().ifPresent(display -> display.setBlockState(state));
    }
}
