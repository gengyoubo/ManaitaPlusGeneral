package github.com.gengyoubo.MPG.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import github.com.gengyoubo.MPG.entity.MPGLightningBolt;
import github.com.gengyoubo.MPG.item.data.IMPGDoubling;
import github.com.gengyoubo.MPG.item.data.IMPGKey;
import github.com.gengyoubo.MPG.item.tier.MPGToolTier;
import github.com.gengyoubo.MPG.network.Networking;
import github.com.gengyoubo.MPG.network.server.ChangeEntityDataPacket;
import github.com.gengyoubo.MPG.util.MPGEntityData;
import github.com.gengyoubo.MPG.util.MPGNBTData;
import github.com.gengyoubo.MPG.util.MPText;
import github.com.gengyoubo.MPG.util.MPUtils;

import java.util.List;
import java.util.Random;

import static github.com.gengyoubo.MPG.core.MPGEntityCore.ManaitaLightningBolt;

public class MPGGodSwordItem extends SwordItem implements IMPGKey, IMPGDoubling {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    public MPGGodSwordItem() {
        super(new MPGToolTier(), 0, 0, new Item.Properties().fireResistant());
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }




    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        MPGEntityData.manaita.remove(player);
        Networking.sendToSameLevelPlayers(player.level(), new ChangeEntityDataPacket(player.getId(), -MPGEntityData.manaita.getFlag()));
        return super.onDroppedByPlayer(item, player);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack p_41404_, @NotNull Level p_41405_, @NotNull Entity p_41406_, int p_41407_, boolean p_41408_) {
        if (p_41406_ instanceof  Player player) {
            if (!p_41405_.isClientSide) {
                player.getAbilities().mayfly = true;
                player.setHealth(player.getMaxHealth());
                MPGEntityData.manaita.add(player);
            }
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            MPUtils.godKill(player,isRemove(stack),player.isShiftKeyDown());
        }
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        MPUtils.attack(entity, player,isRemove(stack));
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level p_41432_, Player player, @NotNull InteractionHand p_41434_) {
        ItemStack itemstack = player.getItemInHand(p_41434_);
        player.startUsingItem(p_41434_);
        if (!p_41432_.isClientSide) {
            Random random = new Random();
            Vec3 position = player.position();
            for (int i = 0; i < 100; i++) {
                MPGLightningBolt bolt = ManaitaLightningBolt.get().create(p_41432_);
                if (bolt != null) {
                    float angle = random.nextFloat() * 62.831852F;
                    double distance = random.nextGaussian() * 100.0D;
                    double x = Mth.sin(angle) * distance + position.x;
                    double z = Mth.cos(angle) * distance + position.z;

                    int y = p_41432_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, (int) x, (int) z);

                    bolt.setPos(x, y, z);
                    p_41432_.addFreshEntity(bolt);
                }
            }
        }
        MPUtils.godKill(player,isRemove(itemstack),player.isShiftKeyDown());
        return InteractionResultHolder.pass(itemstack);
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot p_43274_) {
        return p_43274_ == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(p_43274_);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack p_41421_, @Nullable Level p_41422_, @NotNull List<Component> p_41423_, @NotNull TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
        p_41423_.add(Component.literal(MPText.manaita_mode.formatting(I18n.get("mode.doubling") + ":" + (isDoubling(p_41421_) ? I18n.get("info.on") : I18n.get("info.off")))));
        p_41423_.add(Component.literal(MPText.manaita_mode.formatting(I18n.get("mode.remove.name") + ":" + (isRemove(p_41421_) ? I18n.get("info.on") : I18n.get("info.off")))));
        p_41423_.add(Component.empty());
        p_41423_.add(Component.literal(MPText.manaita_enchantment.formatting(I18n.get("info.item.manaita_sword_god.1"))));
    }

    @Override
    public @NotNull Object getRenderPropertiesInternal() {
        return new IClientItemExtensions() {
            @Nullable
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                if (entityLiving.getUsedItemHand() == hand && entityLiving.getUseItemRemainingTicks() > 0)
                    return HumanoidModel.ArmPose.BLOCK;
                return null;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == (arm == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)) {
                        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                        double f = Mth.sin(swingProcess * swingProcess * Mth.PI);
                        double f1 = Mth.sin(Mth.sqrt(swingProcess) * Mth.PI);
                        poseStack.translate(side * 0.56, -0.52 + equipProcess * -0.6, -0.72);
                        poseStack.translate(side * -0.1414214, 0.08, 0.1414214);
                        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-102.25F - f1 * 80.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees((float) (side * 13.365F - f * 20.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (side * 78.050003F - f1 * 20.0F)));
                        return true;
                }
                return false;
            }
        };
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack p_41458_) {
        return Component.literal(MPText.manaita_infinity.formatting(I18n.get("item.manaita_sword_god.name")));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack p_41454_) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack p_41452_) {
        return UseAnim.CUSTOM;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack p_41453_) {
        return true;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack p_41456_) {
        return true;
    }

// --注释掉检查 START (2026/4/24 23:35):
//    public void onManaitaKeyPress(ItemStack itemStack, Player player) {
//        if (player.isShiftKeyDown()) {
//            boolean remove = !isRemove(itemStack);
//            setRemove(itemStack, remove);
//        } else {
//            boolean doubling = !isDoubling(itemStack);
//            setDoubling(itemStack, doubling);
//        }
//    }
// --注释掉检查 STOP (2026/4/24 23:35)

    @Override
    public void onManaitaKeyPress(ItemStack itemStack) {
        toggleDoubling(itemStack);
    }

    @Override
    public void onManaitaKeyPress(ItemStack itemStack, Player player) {
        if (player.isShiftKeyDown()) {
            setRemove(itemStack, !isRemove(itemStack));
        } else {
            toggleDoubling(itemStack);
        }
    }

    @Override
    public void onManaitaKeyPressOnClient(ItemStack itemStack, Player player) {
        if (player.isShiftKeyDown()) {
            boolean remove = !isRemove(itemStack);
            setRemove(itemStack, remove);
            MPUtils.chat(player, Component.literal(MPText.manaita_mode.formatting(String.format("[%s] %s: %s", I18n.get("item.manaita_sword_god.name"), I18n.get("mode.remove.name"), (remove ? I18n.get("info.on") : I18n.get("info.off"))))));
        } else {
            boolean doubling = toggleDoubling(itemStack);
            MPUtils.chat(player, Component.literal(MPText.manaita_mode.formatting(String.format("[%s] %s: %s", I18n.get("item.manaita_sword_god.name"), I18n.get("mode.doubling"), (doubling ? I18n.get("info.on") : I18n.get("info.off"))))));
        }
    }


    public static boolean isRemove(ItemStack itemStack) {
        if (!itemStack.hasTag()) return false;
        assert itemStack.getTag() != null;
        return itemStack.getTag().getBoolean(MPGNBTData.Remove);
    }

    public static void setRemove(ItemStack itemStack,boolean remove) {
        itemStack.getOrCreateTag().putBoolean(MPGNBTData.Remove, remove);
    }
}
