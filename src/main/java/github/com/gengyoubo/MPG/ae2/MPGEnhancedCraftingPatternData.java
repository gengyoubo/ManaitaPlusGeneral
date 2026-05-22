package github.com.gengyoubo.MPG.ae2;

import github.com.gengyoubo.MPG.MPG;
import github.com.gengyoubo.MPG.util.MPGNBTData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class MPGEnhancedCraftingPatternData {
    public static final String TAG_VERSION = "mpgEnhancedPatternVersion";
    public static final String TAG_RECIPE_ID = "mpgRecipeId";
    public static final String TAG_INPUTS = "mpgInputs";
    public static final String TAG_OUTPUT = "mpgOutput";
    public static final String TAG_INPUT_SLOT = "slot";
    public static final String TAG_INPUT_STACK = "stack";
    public static final int VERSION = 1;
    public static final int CRAFTING_GRID_SIZE = 9;

    private MPGEnhancedCraftingPatternData() {
    }

    public static boolean isEncoded(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && isEncoded(tag);
    }

    public static boolean isEncoded(CompoundTag tag) {
        return tag.getInt(TAG_VERSION) == VERSION && tag.contains(TAG_RECIPE_ID, Tag.TAG_STRING);
    }

    public static void write(ItemStack pattern, ResourceLocation recipeId, ItemStack[] inputs, ItemStack output) {
        CompoundTag tag = pattern.getOrCreateTag();
        tag.putInt(TAG_VERSION, VERSION);
        tag.putString(TAG_RECIPE_ID, recipeId.toString());

        ListTag inputList = new ListTag();
        for (int slot = 0; slot < Math.min(inputs.length, CRAFTING_GRID_SIZE); slot++) {
            ItemStack input = normalizeStack(inputs[slot]);
            if (input == null || input.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putByte(TAG_INPUT_SLOT, (byte) slot);
            entry.put(TAG_INPUT_STACK, input.save(new CompoundTag()));
            inputList.add(entry);
        }
        tag.put(TAG_INPUTS, inputList);
        tag.put(TAG_OUTPUT, normalizeStack(output).save(new CompoundTag()));
    }

    public static ResourceLocation readRecipeId(CompoundTag tag) {
        ResourceLocation recipeId = ResourceLocation.tryParse(tag.getString(TAG_RECIPE_ID));
        return recipeId == null ? new ResourceLocation(MPG.MODID, "missing") : recipeId;
    }

    public static ItemStack[] readInputs(CompoundTag tag) {
        ItemStack[] inputs = new ItemStack[CRAFTING_GRID_SIZE];
        ListTag inputList = tag.getList(TAG_INPUTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < inputList.size(); i++) {
            CompoundTag entry = inputList.getCompound(i);
            int slot = entry.getByte(TAG_INPUT_SLOT) & 255;
            if (slot >= 0 && slot < CRAFTING_GRID_SIZE) {
                inputs[slot] = normalizeStack(ItemStack.of(entry.getCompound(TAG_INPUT_STACK)));
            }
        }
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] == null) {
                inputs[i] = ItemStack.EMPTY;
            }
        }
        return inputs;
    }

    public static ItemStack readOutput(CompoundTag tag) {
        return normalizeStack(ItemStack.of(tag.getCompound(TAG_OUTPUT)));
    }

    public static ItemStack normalizeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack normalized = stack.copy();
        if (isTypedManaitaStack(normalized) && !normalized.getOrCreateTag().contains(MPGNBTData.ItemType)) {
            normalized.getOrCreateTag().putInt(MPGNBTData.ItemType, 0);
        }
        return normalized;
    }

    private static boolean isTypedManaitaStack(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!MPG.MODID.equals(itemId.getNamespace())) {
            return false;
        }
        return switch (itemId.getPath()) {
            case "block_crafting_manaita",
                    "block_furnace_manaita",
                    "block_brewing_manaita",
                    "block_hook_manaita",
                    "manaita_crafting_portable",
                    "manaita_furnace_portable",
                    "manaita_brewing_portable",
                    "manaita_crafting_ring",
                    "manaita_furnace_ring",
                    "manaita_brewing_ring" -> true;
            default -> false;
        };
    }
}
