package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.ae2.MPGStorageCellEnhancement;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.me.cells.BasicCellInventory", remap = false)
public class BasicCellInventoryMixin {
    @Shadow
    private int maxItemTypes;

    @Shadow
    private ItemStack i;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mpg$enhanceTypeLimit(CallbackInfo ci) {
        maxItemTypes = Math.max(maxItemTypes, MPGStorageCellEnhancement.getEnhancedTypes(i, maxItemTypes));
    }

    @Inject(method = "getTotalBytes", at = @At("RETURN"), cancellable = true)
    private void mpg$enhanceTotalBytes(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(MPGStorageCellEnhancement.getEnhancedBytesLong(i, cir.getReturnValue()));
    }
}
