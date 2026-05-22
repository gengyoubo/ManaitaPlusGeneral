package github.com.gengyoubo.MPG.ae2;

import github.com.gengyoubo.MPG.MPGConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class MPGStorageCellEnhancementData {
    public static final String TAG_LEVEL = "mpgAe2StorageCellEnhancement";
    private static final String TAG_BASE_BYTES = "mpgAe2StorageCellBaseBytes";
    private static final String TAG_BASE_TYPES = "mpgAe2StorageCellBaseTypes";

    private MPGStorageCellEnhancementData() {
    }

    public static int getLevel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(TAG_LEVEL));
    }

    public static void increaseLevel(ItemStack stack) {
        int level = getLevel(stack);
        stack.getOrCreateTag().putInt(TAG_LEVEL, level + 1);
    }

    public static void initializeBaseStats(ItemStack stack, int baseBytes, int baseTypes) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_BASE_BYTES)) {
            tag.putInt(TAG_BASE_BYTES, Math.max(1, baseBytes));
        }
        if (!tag.contains(TAG_BASE_TYPES)) {
            tag.putInt(TAG_BASE_TYPES, Math.max(1, baseTypes));
        }
    }

    public static int getBaseBytes(ItemStack stack, int fallback) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_BASE_BYTES)) {
            return Math.max(1, tag.getInt(TAG_BASE_BYTES));
        }
        return Math.max(1, fallback);
    }

    public static int getBaseTypes(ItemStack stack, int fallback) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_BASE_TYPES)) {
            return Math.max(1, tag.getInt(TAG_BASE_TYPES));
        }
        return Math.max(1, fallback);
    }

    public static Component applyNamePrefix(ItemStack stack, Component originalName) {
        int level = getLevel(stack);
        if (level <= 0) {
            return originalName;
        }
        return Component.literal(createNamePrefix(level)).append(originalName.copy());
    }

    public static String describeMultiplier(int level) {
        long multiplier = 1L;
        int perLevel = Math.max(1, MPGConfig.source_doubling_value);
        for (int i = 0; i < level; i++) {
            if (multiplier > Long.MAX_VALUE / perLevel) {
                return ">=x" + Long.MAX_VALUE;
            }
            multiplier *= perLevel;
        }
        return "x" + multiplier;
    }

    public static int getMultiplierPerLevel() {
        return Math.max(1, MPGConfig.source_doubling_value);
    }

    private static String createNamePrefix(int level) {
        StringBuilder prefix = new StringBuilder();
        int perLevel = Math.max(1, MPGConfig.source_doubling_value);
        for (int i = 0; i < level; i++) {
            prefix.append(perLevel).append('*');
        }
        return prefix.toString();
    }
}
