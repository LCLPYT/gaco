package work.lclpnet.gaco.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record OrientedBlockPos(BlockPos pos, Direction direction) {

    public static final Codec<OrientedBlockPos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(OrientedBlockPos::pos),
            Direction.CODEC.fieldOf("direction").forGetter(OrientedBlockPos::direction)
    ).apply(instance, OrientedBlockPos::new));
}
