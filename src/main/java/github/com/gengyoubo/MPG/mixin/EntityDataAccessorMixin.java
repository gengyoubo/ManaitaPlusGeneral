package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityDataAccessor.class)
public class EntityDataAccessorMixin {
    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "setData", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectEntityData(CompoundTag tag, CallbackInfo ci) {
        if (MPUtils.isProtectedFromForcedRemoval(this.entity)) {
            ci.cancel();
        }
    }
}
