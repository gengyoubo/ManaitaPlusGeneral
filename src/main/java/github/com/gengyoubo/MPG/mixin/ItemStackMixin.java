package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.ae2.MPGStorageCellEnhancementData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void mpg$enhancedStorageCellName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        cir.setReturnValue(MPGStorageCellEnhancementData.applyNamePrefix(stack, cir.getReturnValue()));
    }
}
