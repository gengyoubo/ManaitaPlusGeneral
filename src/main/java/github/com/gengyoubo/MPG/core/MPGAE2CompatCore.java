package github.com.gengyoubo.MPG.core;

import github.com.gengyoubo.MPG.item.MPGEnhancedCraftingPatternItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import static github.com.gengyoubo.MPG.MPG.ITEMS;

public class MPGAE2CompatCore {
    public static RegistryObject<Item> registerEnhancedCraftingPattern() {
        return ITEMS.register("enhanced_crafting_pattern", MPGEnhancedCraftingPatternItem::new);
    }
}
