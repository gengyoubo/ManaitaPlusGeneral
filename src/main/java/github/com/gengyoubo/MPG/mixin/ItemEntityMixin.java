package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void manaita_plus_general$bounceManaitaArmorOnLava(CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (!MPUtils.isProtectedItemEntity(itemEntity)
                || !itemEntity.isInLava()
                || itemEntity.getFluidHeight(FluidTags.LAVA) <= 0.0D) {
            return;
        }

        Vec3 movement = itemEntity.getDeltaMovement();
        itemEntity.setDeltaMovement(movement.x * 0.8D, Math.max(movement.y, 0.35D), movement.z * 0.8D);
        itemEntity.hasImpulse = true;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectFromItemDamage(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (MPUtils.isProtectedItemEntity((ItemEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectFromFire(CallbackInfoReturnable<Boolean> cir) {
        if (MPUtils.isProtectedItemEntity((ItemEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
