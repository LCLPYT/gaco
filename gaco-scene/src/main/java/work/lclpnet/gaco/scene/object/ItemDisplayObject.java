package work.lclpnet.gaco.scene.object;

import lombok.Getter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import work.lclpnet.gaco.scene.MountContext;
import work.lclpnet.gaco.scene.Scene;

@Getter
public class ItemDisplayObject extends DisplayEntityObject<Display.ItemDisplay> {

    private ItemStack stack;
    private ItemDisplayContext itemDisplayContext = ItemDisplayContext.NONE;

    public ItemDisplayObject(Scene scene, ItemStack stack) {
        super(scene);
        this.stack = stack;
    }

    @Override
    protected Display.ItemDisplay createDisplayEntity(MountContext ctx) {
        return new Display.ItemDisplay(EntityType.ITEM_DISPLAY, ctx.world());
    }

    @Override
    protected void configure(Display.ItemDisplay display) {
        super.configure(display);

        display.setItemStack(stack);
        display.setItemTransform(itemDisplayContext);
    }

    @Override
    public ItemDisplayObject deepCopy(Scene scene) {
        var copy = new ItemDisplayObject(scene, stack);

        copy.deepCopy(this);

        return copy;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
        entityRef.optional().ifPresent(display -> display.setItemStack(stack));
    }

    public void setItemDisplayContext(ItemDisplayContext itemDisplayContext) {
        this.itemDisplayContext = itemDisplayContext;
        entityRef.optional().ifPresent(display -> display.setItemTransform(itemDisplayContext));
    }
}
