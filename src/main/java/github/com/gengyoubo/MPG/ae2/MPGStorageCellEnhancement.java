package github.com.gengyoubo.MPG.ae2;

import appeng.api.storage.cells.IBasicCellItem;
import github.com.gengyoubo.MPG.core.MPGItemCore;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static github.com.gengyoubo.MPG.MPGConfig.source_doubling_value;

public final class MPGStorageCellEnhancement {
    private static final String ENTITY_FUSION_TICKS = "mpgAe2StorageCellFusionTicks";
    private static final int REQUIRED_TICKS = 100;

    private MPGStorageCellEnhancement() {
    }

    public static void tick(ServerLevel level) {
        List<ItemEntity> cells = new ArrayList<>();
        List<ItemEntity> sources = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (isEnhanceableCell(stack)) {
                cells.add(itemEntity);
            } else if (isSource(stack)) {
                sources.add(itemEntity);
            }
        }

        for (ItemEntity cellEntity : cells) {
            ItemEntity sourceEntity = findSourceInSameBlock(cellEntity, sources);
            if (sourceEntity == null) {
                cellEntity.getPersistentData().remove(ENTITY_FUSION_TICKS);
                continue;
            }

            ItemStack cellStack = cellEntity.getItem();
            if (!canEnhance(cellStack)) {
                cellEntity.getPersistentData().remove(ENTITY_FUSION_TICKS);
                continue;
            }

            CompoundTag entityData = cellEntity.getPersistentData();
            int ticks = entityData.getInt(ENTITY_FUSION_TICKS) + 1;
            if (ticks < REQUIRED_TICKS) {
                entityData.putInt(ENTITY_FUSION_TICKS, ticks);
                continue;
            }

            entityData.remove(ENTITY_FUSION_TICKS);
            enhanceOneCell(level, cellEntity, sourceEntity);
        }
    }

    public static int getEnhancedBytes(ItemStack stack, int baseBytes) {
        int level = MPGStorageCellEnhancementData.getLevel(stack);
        int perLevel = Math.max(1, source_doubling_value);
        long bytes = baseBytes;
        for (int i = 0; i < level; i++) {
            if (bytes >= Integer.MAX_VALUE / perLevel) {
                return Integer.MAX_VALUE;
            }
            bytes *= perLevel;
        }
        return (int) Math.min(Integer.MAX_VALUE, bytes);
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        if (!isCell(stack)) {
            return;
        }

        int level = MPGStorageCellEnhancementData.getLevel(stack);
        if (level <= 0) {
            return;
        }

        tooltip.add(Component.translatable(
                "tooltip.manaita_plus_general.ae2_storage_cell_enhanced",
                level,
                MPGStorageCellEnhancementData.describeMultiplier(level)
        ).withStyle(ChatFormatting.AQUA));
    }

    private static void enhanceOneCell(ServerLevel level, ItemEntity cellEntity, ItemEntity sourceEntity) {
        ItemStack original = cellEntity.getItem();
        ItemStack enhanced = original.copy();
        enhanced.setCount(1);
        MPGStorageCellEnhancementData.increaseLevel(enhanced);

        if (original.getCount() == 1) {
            cellEntity.setItem(enhanced);
        } else {
            original.shrink(1);
            ItemEntity enhancedEntity = new ItemEntity(level, cellEntity.getX(), cellEntity.getY(), cellEntity.getZ(), enhanced);
            enhancedEntity.setDeltaMovement(cellEntity.getDeltaMovement());
            level.addFreshEntity(enhancedEntity);
        }

        sourceEntity.getItem().shrink(1);
        if (sourceEntity.getItem().isEmpty()) {
            sourceEntity.discard();
        }

        level.sendParticles(ParticleTypes.EXPLOSION, cellEntity.getX(), cellEntity.getY() + 0.25D, cellEntity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, cellEntity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static ItemEntity findSourceInSameBlock(ItemEntity cellEntity, List<ItemEntity> sources) {
        for (ItemEntity source : sources) {
            if (source.isAlive() && source.blockPosition().equals(cellEntity.blockPosition())) {
                return source;
            }
        }
        return null;
    }

    private static boolean canEnhance(ItemStack stack) {
        if (!isEnhanceableCell(stack)) {
            return false;
        }
        int currentBytes = ((IBasicCellItem) stack.getItem()).getBytes(stack);
        return currentBytes < Integer.MAX_VALUE;
    }

    private static boolean isEnhanceableCell(ItemStack stack) {
        return isCell(stack) && stack.getCount() > 0;
    }

    private static boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IBasicCellItem cellItem && cellItem.isStorageCell(stack);
    }

    private static boolean isSource(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MPGItemCore.ManaitaSource.get());
    }

}
