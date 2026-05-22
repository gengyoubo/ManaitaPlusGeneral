package github.com.gengyoubo.MPG;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = MPG.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MPGConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue creative_range_destroy = BUILDER
            .comment("This determines whether the creative can use range mining")
            .define("creative_range_destroy", false);

    private static final ForgeConfigSpec.BooleanValue easy_mode = BUILDER
            .comment("Easy mode: simplify recipes (for example, use diamond block instead of cobblestone)")
            .define("easy_mode", false);

    private static final ForgeConfigSpec.IntValue experience_drops_doubling = BUILDER
            .comment("ExperienceDropsDoubling")
            .defineInRange("experience_drops_doubling_value", 4, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue item_drops_doubling = BUILDER
            .comment("ItemDropsDoubling")
            .defineInRange("item_drops_doubling_value", 4, 1, Integer.MAX_VALUE);
    
    private static final ForgeConfigSpec.IntValue crafting_doubling = BUILDER
            .comment("CraftingDoubling")
            .defineInRange("crafting_doubling_value", 64, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue furnace_doubling = BUILDER
            .comment("FurnaceDoubling")
            .defineInRange("furnace_doubling_value", 64, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue brewing_doubling = BUILDER
            .comment("BrewingDoubling")
            .defineInRange("brewing_doubling_value", 64, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue destroy_doubling = BUILDER
            .comment("DestroyDoubling")
            .defineInRange("destroy_doubling_value", 4, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue source_doubling = BUILDER
            .comment("SourceDoubling")
            .defineInRange("source_doubling_value", 64, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean creative_range_destroy_value = false;
    public static boolean easy_mode_value = false;
    public static int item_drops_doubling_value = 4;
    public static int experience_drops_doubling_value = 4;
    public static int crafting_doubling_value = 64;
    public static int furnace_doubling_value = 64;
    public static int brewing_doubling_value = 64;
    public static int destroy_doubling_value = 4;
    public static int source_doubling_value = 64;
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        creative_range_destroy_value = creative_range_destroy.get();
        easy_mode_value = easy_mode.get();
        item_drops_doubling_value = item_drops_doubling.get();
        experience_drops_doubling_value = experience_drops_doubling.get();
        crafting_doubling_value = crafting_doubling.get();
        furnace_doubling_value = furnace_doubling.get();
        brewing_doubling_value = brewing_doubling.get();
        destroy_doubling_value = destroy_doubling.get();
        source_doubling_value = source_doubling.get();
    }
}
