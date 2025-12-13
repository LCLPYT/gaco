package work.lclpnet.gaco.scene.object;

import lombok.Getter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.gaco.scene.MountContext;
import work.lclpnet.gaco.scene.Scene;

@Getter
public class TextDisplayObject extends DisplayEntityObject<Display.TextDisplay> {

    private Component text;
    private int background = 1073741824;

    public TextDisplayObject(Scene scene, Component text) {
        super(scene);
        this.text = text;
    }

    @Override
    protected @Nullable Display.TextDisplay createDisplayEntity(MountContext ctx) {
        return new Display.TextDisplay(EntityType.TEXT_DISPLAY, ctx.world());
    }

    @Override
    protected void configure(Display.TextDisplay display) {
        super.configure(display);

        display.setText(text);
        display.setBackgroundColor(background);
    }

    @Override
    public TextDisplayObject deepCopy(Scene scene) {
        var copy = new TextDisplayObject(scene, text);

        copy.deepCopy(this);

        return copy;
    }

    public void setText(Component text) {
        this.text = text;
        entityRef.optional().ifPresent(display -> display.setText(text));
    }

    public void setBackground(int background) {
        this.background = background;
        entityRef.optional().ifPresent(display -> display.setBackgroundColor(background));
    }
}
