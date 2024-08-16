package minds3t.lumberjack.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = LJMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LJConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue CREATIVE_MOD_ENABLED = BUILDER
            .comment("Whether to break the whole tree, when applicable, in creative mode")
            .define("creativeEnabled", true);

    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING_ENABLED = BUILDER
            .comment("Whether to print extra info from Lumberjack to the console as it destroys trees")
            .define("debugLogging", false);

    private static final ModConfigSpec.IntValue MAX_RANGE = BUILDER
            .comment("How far in blocks to allow auto-breaking from ignoring the Y axis?")
            .defineInRange("maxRange", 16, 0, 64);

    private static final ModConfigSpec.IntValue MAX_BLOCKS_BROKEN = BUILDER
            .comment("How many blocks in total can be broken from a single tree?")
            .defineInRange("maxBlocksBroken", 1024, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue MAX_TREE_HEIGHT = BUILDER
            .comment("How many blocks above the source break can the tree be to trigger the effect?")
            .defineInRange("maxTreeHeight", 100, 0, 312);

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_BLACKLIST = BUILDER
            .comment("A blacklist of blocks to not auto-destroy.")
            .defineListAllowEmpty("blockBlacklist", List.of(), LJConfig::validateBlockName);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_WHITELIST = BUILDER
            .comment("A whitelist of blocks to specify the only blocks allowed to be auto-broken. If this is not empty, this list will take precedence over the blacklist.")
            .defineListAllowEmpty("blockWhitelist", List.of(), LJConfig::validateBlockName);

    static final ModConfigSpec SPEC = BUILDER.build();
    public static boolean creativeEnabled;
    public static boolean debugLogging;
    public static int maxRange;
    public static int maxBlocksBroken;
    public static int maxTreeHeight;
    public static Set<Block> blockBlacklist;
    public static Set<Block> blockWhitelist;

    private static boolean validateBlockName(final Object obj)
    {
        return obj instanceof String itemName && BuiltInRegistries.BLOCK.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        debugLogging = DEBUG_LOGGING_ENABLED.get();
        creativeEnabled = CREATIVE_MOD_ENABLED.get();
        maxRange = MAX_RANGE.get();
        maxBlocksBroken = MAX_BLOCKS_BROKEN.get();
        maxTreeHeight = MAX_TREE_HEIGHT.get();

        blockBlacklist = BLOCK_BLACKLIST.get().stream()
                .map(itemName -> BuiltInRegistries.BLOCK.get(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());

        blockWhitelist = BLOCK_WHITELIST.get().stream()
                .map(itemName -> BuiltInRegistries.BLOCK.get(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());
        LJMod.log("Loaded config values!",true);
    }
}
