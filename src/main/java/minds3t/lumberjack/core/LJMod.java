package minds3t.lumberjack.core;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LJMod.MODID)
public class LJMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "lumberjack";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // In the main mod file with a ModConfigSpec CONFIG
    public LJMod(ModContainer container) {
        container.addConfig(new ModConfig(ModConfig.Type.CLIENT, LJConfig.SPEC, container));
        NeoForge.EVENT_BUS.register(LJLogEvents.class);
        log("Initialized mod!", true);
    }

    public static void log(String msg) {
        log(msg,false);
    }

    public static void log(String msg, boolean bypassConfig) {
        if(LJConfig.debugLogging || bypassConfig)
        {
            LOGGER.info(String.format("[%s]: %s",MODID,msg));
        }
    }
}
