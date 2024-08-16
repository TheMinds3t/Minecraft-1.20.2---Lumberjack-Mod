package minds3t.lumberjack.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class LJUtil
{
    /**
     * checks for the next air block above the specified position.
     * @param pos the starting position to check
     * @param level the level to search in
     * @return the {@link BlockPos} of the first air block above the specified position.
     */
    public static BlockPos getClosestAirY(BlockPos pos, LevelAccessor level){
        return findBlockFrom(pos,level,true,(state) ->{ return state.isAir();});
    }

    /**
     *
     * A recursive search for the specified predicate starting from the position.
     *
     * @param pos start position in block coordinates to search from
     * @param level level to check in
     * @param moveUp whether to go up (true) or down (false) on the Y axis to search
     * @param validator the way to stop looking
     * @return the first air or non-air block position found above or below the specified position.
     */
    public static BlockPos findBlockFrom(BlockPos pos, LevelAccessor level, boolean moveUp, Predicate<BlockState> validator){
        if(pos.getY() >= 255 || pos.getY() <= 64) //if we are at the top or bottom of the world we can't go further
        {
            return null;
        }

        BlockPos next = moveUp ? pos.above() : pos.below();
        BlockState nextState = level.getBlockState(pos);

        return validator.test(level.getBlockState(pos)) ? pos : findBlockFrom(next, level, moveUp, validator);
    }

}
