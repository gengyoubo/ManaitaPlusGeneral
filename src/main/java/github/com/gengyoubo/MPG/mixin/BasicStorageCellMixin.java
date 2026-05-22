package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.ae2.MPGStorageCellEnhancement;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.items.storage.BasicStorageCell", remap = false)
public class BasicStorageCellMixin {
    @Inject(method = "getBytes", at = @At("RETURN"), cancellable = true)
    private void mpg$enhanceBytes(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(MPGStorageCellEnhancement.getEnhancedBytes(stack, cir.getReturnValue()));
    }
}
