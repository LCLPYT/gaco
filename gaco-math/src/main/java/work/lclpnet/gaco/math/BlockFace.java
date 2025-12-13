package work.lclpnet.gaco.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record BlockFace(BlockPos pos, Direction face) {

    public static final Codec<BlockFace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(BlockFace::pos),
            Direction.CODEC.fieldOf("face").forGetter(BlockFace::face)
    ).apply(instance, BlockFace::new));
}
