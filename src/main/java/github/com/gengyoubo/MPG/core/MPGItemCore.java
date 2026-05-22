package github.com.gengyoubo.MPG.core;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import github.com.gengyoubo.MPG.item.*;
import github.com.gengyoubo.MPG.item.armor.MPGArmor;
import github.com.gengyoubo.MPG.item.portable.MPGBrewingPortable;
import github.com.gengyoubo.MPG.item.portable.MPGCraftingPortable;
import github.com.gengyoubo.MPG.item.portable.MPGFurnacePortable;
import github.com.gengyoubo.MPG.item.ring.MPGBrewingRing;
import github.com.gengyoubo.MPG.item.ring.MPGCraftingRing;
import github.com.gengyoubo.MPG.item.ring.MPGFurnaceRing;
import github.com.gengyoubo.MPG.item.tool.*;
import net.minecraftforge.fml.ModList;

import static github.com.gengyoubo.MPG.MPG.ITEMS;

public class MPGItemCore {
    public static final RegistryObject<Item> ManaitaSwordGod = ITEMS.register("manaita_sword_god", MPGGodSwordItem::new);
    public static final RegistryObject<Item> ManaitaSword = ITEMS.register("manaita_sword", MPGSwordItem::new);
    public static final RegistryObject<Item> ManaitaBow = ITEMS.register("manaita_bow", MPGBowItem::new);
    public static final RegistryObject<Item> ManaitaShovel = ITEMS.register("manaita_shovel", MPGShovelItem::new);
    public static final RegistryObject<Item> ManaitaPickaxe = ITEMS.register("manaita_pickaxe", MPGPickaxeItem::new);
    public static final RegistryObject<Item> ManaitaAxe = ITEMS.register("manaita_axe", MPGAxeItem::new);
    public static final RegistryObject<Item> ManaitaPaxel = ITEMS.register("manaita_paxel", MPGPaxelItem::new);
    public static final RegistryObject<Item> ManaitaHoe = ITEMS.register("manaita_hoe", MPGHoeItem::new);
    public static final RegistryObject<Item> ManaitaShears = ITEMS.register("manaita_shears", MPGShearsItem::new);
    public static final RegistryObject<Item> ManaitaHook = ITEMS.register("manaita_hook", MPGHookItem::new);
    public static final RegistryObject<Item> ManaitaHelmet = ITEMS.register("manaita_helmet", MPGArmor.Helmet::new);
    public static final RegistryObject<Item> ManaitaChestplate = ITEMS.register("manaita_chestplate", MPGArmor.Chestplate::new);
    public static final RegistryObject<Item> ManaitaLeggings = ITEMS.register("manaita_leggings", MPGArmor.Leggings::new);
    public static final RegistryObject<Item> ManaitaBoots = ITEMS.register("manaita_boots", MPGArmor.Boots::new);
    public static final RegistryObject<Item> ManaitaSource = ITEMS.register("manaita_source", MPGSourceItem::new);
    public static RegistryObject<Item> EnhancedCraftingPattern;
    public static final RegistryObject<Item> ManaitaCraftingPortable = ITEMS.register("manaita_crafting_portable", MPGCraftingPortable::new);
    public static final RegistryObject<Item> ManaitaFurnacePortable = ITEMS.register("manaita_furnace_portable", MPGFurnacePortable::new);
    public static final RegistryObject<Item> ManaitaBrewingPortable = ITEMS.register("manaita_brewing_portable", MPGBrewingPortable::new);
    public static final RegistryObject<Item> ManaitaCraftingRing = ITEMS.register("manaita_crafting_ring", MPGCraftingRing::new);
    public static final RegistryObject<Item> ManaitaFurnaceRing = ITEMS.register("manaita_furnace_ring", MPGFurnaceRing::new);
    public static final RegistryObject<Item> ManaitaBrewingRing = ITEMS.register("manaita_brewing_ring", MPGBrewingRing::new);

    public static void init() {
        if (ModList.get().isLoaded("ae2") && EnhancedCraftingPattern == null) {
            EnhancedCraftingPattern = MPGAE2CompatCore.registerEnhancedCraftingPattern();
        }
    }

}

