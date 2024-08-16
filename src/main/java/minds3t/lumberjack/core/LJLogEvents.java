package minds3t.lumberjack.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@Mod.EventBusSubscriber
public class LJLogEvents
{
    public static final Predicate<BlockState> LOG_SELECTOR = (state) -> { return state.getTags().anyMatch((tag)->{return tag == BlockTags.LOGS;}); };
    public static final Predicate<BlockState> LEAF_SELECTOR = (state) -> { return state.getTags().anyMatch((tag)->{return tag == BlockTags.LEAVES;}); };

    //an additional predicate to make sure the block is valid per config settings
    public static final Predicate<Block> CONFIG_VALIDATOR = (block) -> {
        return LJConfig.blockWhitelist == null && LJConfig.blockBlacklist == null || (LJConfig.blockWhitelist.isEmpty() ?
                LJConfig.blockBlacklist.isEmpty() || !LJConfig.blockBlacklist.contains(block) :
                LJConfig.blockWhitelist.contains(block));
    };

    public static final Predicate<Player> LOG_BREAK_AVAILABLE = (player) -> {
        return player.getAbilities().mayBuild && (LJConfig.creativeEnabled ? true : !player.getAbilities().instabuild);
    };

    private static final int[][] LOG_NEIGHBORS = new int[9][3];

    static {
        //populate with 3x3 grid above the block for offsets
        for(int x = -1; x < 2; ++x)
        {
            for(int z = -1; z < 2; ++z)
            {
                int ind = (x + 1) * 3 + (z + 1);
                LOG_NEIGHBORS[ind][0] = x;
                LOG_NEIGHBORS[ind][1] = 1;
                LOG_NEIGHBORS[ind][2] = z;
            }
        }
    }

    /**
     * Retrieves a list of BlockPos coordinates that are valid tree blocks. This has an inherent rule that if the first block with air above it above the @pos parameter is not leaves it will not function.
     * @param pos the block position to start the tree search at. If the block below the next air block above this is not leaves, this function will not do anything.
     * @param level the level the block is being handled by
     * @return a list of all block positions that are leaves and logs connected to the specified block position.
     */
    public static ArrayList<BlockPos> getValidTree(BlockPos pos, LevelAccessor level) {
        ArrayList<BlockPos> blockQueue = new ArrayList<BlockPos>();
        ArrayList<BlockPos> retBlocks = new ArrayList<BlockPos>();

        //if the block touching air above this one is NOT leaves then don't count it as a tree and stop process
        BlockPos airBlockPos = LJUtil.getClosestAirY(pos, level);

        //add a check in case the blocks go straight up to the limit, if this is not null then check blocks above source break
        if(airBlockPos != null)
        {
            airBlockPos = airBlockPos.below();
            BlockState airBlockState = level.getBlockState(airBlockPos);

            //checking the second non-air block above this air block (for cases like TF Mangroves where the top of the wood is exposed but the tree splits above)
            BlockPos airBlockPos2 = LJUtil.findBlockFrom(airBlockPos.above(), level, true, (state)->{return !state.isAir();});

            if(airBlockPos2 != null)
            {
                BlockState airBlockState2 = level.getBlockState(airBlockPos2);

                //if the blocks above the root of this tree do not end with leaves on or above the top we don't consider it a tree.
                if (!(LEAF_SELECTOR.test(airBlockState2) || CONFIG_VALIDATOR.test(airBlockState2.getBlock())))
                {
                    return retBlocks;
                }
                else if(airBlockPos2.getY() - pos.getY() > LJConfig.maxTreeHeight) {
                    return retBlocks;
                }
            }
            else if(airBlockPos.getY() - pos.getY() > LJConfig.maxTreeHeight) {
                return retBlocks;
            }

            //if the blocks above the root of this tree do not end with leaves on or above the top we don't consider it a tree.
            if (!(LEAF_SELECTOR.test(airBlockState) || CONFIG_VALIDATOR.test(airBlockState.getBlock())))
            {
                return retBlocks;
            }
        }

        blockQueue.add(pos);

        //expand the base of the tree to see if more logs are nearby in cardinal directions
        for(int x = -1; x < 2; x += 2)
        {
            for(int layer = 0; layer < 2; ++layer)
            {
                BlockPos basePos = pos.offset(layer == 0 ? x : 0, 0, layer == 1 ? x : 0);
                BlockState baseState = level.getBlockState(basePos);
                if(LOG_SELECTOR.test(baseState) && CONFIG_VALIDATOR.test(baseState.getBlock()))
                {
                    blockQueue.add(basePos);
                }
            }
        }

        LJMod.log("starting block queue of "+blockQueue+":");

        while(!blockQueue.isEmpty())
        {
            BlockPos cur = blockQueue.remove(0);

            if(new BlockPos(cur.getX(),pos.getY(),cur.getZ()).closerThan(pos,LJConfig.maxRange))
            {
                if(!retBlocks.contains(cur))
                {
                    retBlocks.add(cur);
                }

                LJMod.log("\ttraversing "+cur+"...");

                // check all valid offsets for logs
                for(int[] off : LOG_NEIGHBORS)
                {
                    BlockPos newPos = cur.offset(off[0], off[1], off[2]);
                    BlockState newState = level.getBlockState(newPos);

                    //traverse through leaves and logs, filter leaves out at the end
                    if(!retBlocks.contains(newPos) && CONFIG_VALIDATOR.test(newState.getBlock()) && (LOG_SELECTOR.test(newState) || LEAF_SELECTOR.test(newState))) {
                        if(!blockQueue.contains(newPos)) {
                            blockQueue.add(newPos);
                        }

                        if(!retBlocks.contains(newPos))
                        {
                            retBlocks.add(newPos);
                        }
                    }

                    //check immediate neighbors for logs only to add and investigate in case of sideways branches
                    BlockState belowNew = level.getBlockState(newPos.below());
                    if((LOG_SELECTOR.test(belowNew) || LEAF_SELECTOR.test(belowNew)) //either log or leaf to the sides of the current block
                       && CONFIG_VALIDATOR.test(belowNew.getBlock())) //make sure blacklist/whitelist are respected if existant
                    {
                        if(!blockQueue.contains(newPos)) {
                            blockQueue.add(newPos);
                        }

                        if(!retBlocks.contains(newPos))
                        {
                            retBlocks.add(newPos.below());
                        }
                    }
                }
            }
        }

        //add all the branched paths that aren't leaves
        Set<BlockPos> set = new HashSet<>(retBlocks);
        retBlocks.clear();
        retBlocks.addAll(set);
        return retBlocks;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        BlockState state = event.getState();
        LevelAccessor level = event.getLevel();
        Player player = event.getPlayer();

        if(LOG_SELECTOR.test(state) && LOG_BREAK_AVAILABLE.test(player))
        {
            ArrayList<BlockPos> tree = getValidTree(event.getPos(),level);

            if(tree.size() <= LJConfig.maxBlocksBroken)
            {
                for(BlockPos newBlock : tree)
                {
                    LJMod.log("\tbreaking pos="+newBlock.toString());
                    level.destroyBlock(newBlock,!player.getAbilities().instabuild,player,Block.UPDATE_ALL_IMMEDIATE);
                }

                LJMod.log("broke " + tree.size() + " blocks in tree (<" + LJConfig.maxBlocksBroken + ", height<"+LJConfig.maxTreeHeight+")",true);
            }
            else {
                LJMod.log("found tree that is too large (" + tree.size() + " blocks) to break per config limit ("+LJConfig.maxBlocksBroken+")",true);
            }
        }
    }
}
