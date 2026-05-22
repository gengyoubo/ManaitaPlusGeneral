package github.com.gengyoubo.MPG.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import github.com.gengyoubo.MPG.MPG;
import github.com.gengyoubo.MPG.ae2.MPGEnhancedCraftingPattern;
import github.com.gengyoubo.MPG.ae2.MPGEnhancedCraftingPatternData;
import github.com.gengyoubo.MPG.core.MPGItemCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MPGEnhancedCraftingPatternItem extends EncodedPatternItem {
    private static final ResourceLocation SOURCE_RECIPE_ID = new ResourceLocation(MPG.MODID, "source_manaita");

    public MPGEnhancedCraftingPatternItem() {
        super(new Properties().stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);
        }

        if (player.isShiftKeyDown() && MPGEnhancedCraftingPatternData.isEncoded(held)) {
            ItemStack blank = new ItemStack(this, held.getCount());
            player.setItemInHand(hand, blank);
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.cleared"));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, blank);
        }

        if (MPGEnhancedCraftingPatternData.isEncoded(held)) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.already_encoded"));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);
        }

        ItemStack target = player.getOffhandItem();
        if (hand == InteractionHand.OFF_HAND || target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.no_target"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);
        }
        if (target.is(MPGItemCore.ManaitaSource.get())) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.invalid_target"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);
        }

        ItemStack source = findSource(player.getInventory());
        if (source.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.no_source"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);
        }

        Optional<CraftingRecipe> recipe = MPGEnhancedCraftingPattern.findRecipe(level, SOURCE_RECIPE_ID);
        if (recipe.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.no_recipe"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);
        }

        ItemStack[] inputs = createSourceCopyInputs(source, target);
        CraftingContainer container = MPGEnhancedCraftingPattern.createCraftingContainer();
        for (int slot = 0; slot < inputs.length; slot++) {
            container.setItem(slot, inputs[slot].copy());
        }

        CraftingRecipe craftingRecipe = recipe.get();
        if (!craftingRecipe.matches(container, level)) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.invalid_recipe"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);
        }

        ItemStack output = craftingRecipe.assemble(container, level.registryAccess());
        if (output.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.manaita_plus_general.enhanced_pattern.invalid_recipe"));
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);
        }

        ItemStack encoded = new ItemStack(this);
        MPGEnhancedCraftingPatternData.write(encoded, craftingRecipe.getId(), inputs, output);
        if (held.getCount() == 1) {
            player.setItemInHand(hand, encoded);
        } else {
            held.shrink(1);
            if (!player.getInventory().add(encoded)) {
                player.drop(encoded, false);
            }
        }

        player.sendSystemMessage(Component.translatable(
                "message.manaita_plus_general.enhanced_pattern.encoded",
                output.getCount(),
                output.getHoverName()
        ));
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (MPGEnhancedCraftingPatternData.isEncoded(stack)) {
            super.appendHoverText(stack, level, tooltip, flag);
            tooltip.add(Component.translatable("tooltip.manaita_plus_general.enhanced_pattern.clear")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.manaita_plus_general.enhanced_pattern.blank.1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.manaita_plus_general.enhanced_pattern.blank.2")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        if (!MPGEnhancedCraftingPatternData.isEncoded(stack)) {
            return null;
        }
        if (!level.isClientSide) {
            MPG.LOGGER.info("[MPG AE2 DEBUG] decode ItemStack pattern tryRecovery={} stack={}", tryRecovery, stack);
        }
        try {
            return new MPGEnhancedCraftingPattern(AEItemKey.of(stack), level);
        } catch (RuntimeException exception) {
            if (!level.isClientSide) {
                MPG.LOGGER.warn("[MPG AE2 DEBUG] failed to decode ItemStack enhanced pattern", exception);
            }
            return null;
        }
    }

    @Override
    public IPatternDetails decode(AEItemKey definition, Level level) {
        if (!level.isClientSide) {
            MPG.LOGGER.info("[MPG AE2 DEBUG] decode AEItemKey pattern hasTag={}", definition.getTag() != null);
        }
        try {
            return new MPGEnhancedCraftingPattern(definition, level);
        } catch (RuntimeException exception) {
            if (!level.isClientSide) {
                MPG.LOGGER.warn("[MPG AE2 DEBUG] failed to decode AEItemKey enhanced pattern", exception);
            }
            return null;
        }
    }

    private static ItemStack[] createSourceCopyInputs(ItemStack source, ItemStack target) {
        ItemStack[] inputs = new ItemStack[MPGEnhancedCraftingPatternData.CRAFTING_GRID_SIZE];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = ItemStack.EMPTY;
        }

        ItemStack sourceInput = source.copy();
        sourceInput.setCount(1);
        inputs[0] = sourceInput;

        ItemStack targetInput = target.copy();
        targetInput.setCount(1);
        inputs[1] = targetInput;
        return inputs;
    }

    private static ItemStack findSource(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(MPGItemCore.ManaitaSource.get())) {
                ItemStack source = stack.copy();
                source.setCount(1);
                return source;
            }
        }
        return ItemStack.EMPTY;
    }
}
