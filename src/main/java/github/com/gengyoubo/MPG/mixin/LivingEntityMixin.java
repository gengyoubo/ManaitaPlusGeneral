package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectFromDirectLethalHealthSet(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if ((health <= 0.0F || Float.isNaN(health)) && MPUtils.isProtectedFromForcedRemoval(entity)) {
            entity.setHealth(entity.getMaxHealth());
            entity.deathTime = 0;
            entity.hurtTime = 0;
            ci.cancel();
        }
    }
}
