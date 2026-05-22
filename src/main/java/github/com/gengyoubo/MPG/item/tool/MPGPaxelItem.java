package github.com.gengyoubo.MPG.item.tool;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import github.com.gengyoubo.MPG.item.tool.base.ManaitaPlusLegacyToolActionHelper;
import github.com.gengyoubo.MPG.item.tool.base.ManaitaPlusLegacyToolBase;
import github.com.gengyoubo.MPG.util.MPGEntityData;
import github.com.gengyoubo.MPG.util.MPText;
import github.com.gengyoubo.MPG.util.MPUtils;

import java.util.List;

public class MPGPaxelItem extends ManaitaPlusLegacyToolBase {
    public static final TagKey<Block> MINEABLE = BlockTags.create(new ResourceLocation("mineable"));

    public MPGPaxelItem() {
        super(MINEABLE);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (MPUtils.isProtectedFromForcedRemoval(entity)) {
            return true;
        }
        MPGEntityData.death.add(entity);
        entity.hurt(entity.damageSources().playerAttack(player), 10000);
        if (entity instanceof LivingEntity living) {
            living.setHealth(0F);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(MPText.manaita_infinity.formatting(I18n.get("info.attack"))));
    }

    @Override
    public void inventoryTick(ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        stack.setPopTime(0);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        int range = getRange(context.getItemInHand()) >> 1;
        boolean changed = ManaitaPlusLegacyToolActionHelper.applyInRange(context, range, (pos, state) ->
                ManaitaPlusLegacyToolActionHelper.applyAxeActions(context, pos, state)
                        | ManaitaPlusLegacyToolActionHelper.applyGrowPlantAction(context, pos, state)
                        | ManaitaPlusLegacyToolActionHelper.applyShovelAction(context, pos, state));
        return changed ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : InteractionResult.PASS;
    }
}
