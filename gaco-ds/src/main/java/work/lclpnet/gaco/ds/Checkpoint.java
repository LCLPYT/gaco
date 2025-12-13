package work.lclpnet.gaco.ds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import work.lclpnet.gaco.math.AffineIntMatrix;

public record Checkpoint(Vec3 pos, float yaw, float pitch, BlockBox bounds) {

    public static final Codec<Checkpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.fieldOf("pos").forGetter(Checkpoint::pos),
            Codec.FLOAT.fieldOf("yaw").forGetter(Checkpoint::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Checkpoint::pitch),
            BlockBox.CODEC.fieldOf("bounds").forGetter(Checkpoint::bounds)
    ).apply(instance, Checkpoint::new));

    public Checkpoint relativize(Vec3 origin) {
        BlockBox relativeBounds = new BlockBox(bounds.min().subtract(BlockPos.containing(origin)), bounds.max().subtract(BlockPos.containing(origin)));
        return new Checkpoint(pos.subtract(origin), yaw, pitch, relativeBounds);
    }

    public Checkpoint transform(AffineIntMatrix mat4) {
        double rad = Math.toRadians(yaw);
        Vec3 vec = mat4.transformVector(Math.sin(-rad), 0d, Math.cos(rad));
        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        BlockBox rotatedBounds = bounds.transform(mat4);

        return new Checkpoint(mat4.transform(pos), yaw, pitch, rotatedBounds);
    }
}
