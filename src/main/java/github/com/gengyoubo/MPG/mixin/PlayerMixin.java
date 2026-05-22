package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "getSlot", at = @At("RETURN"), cancellable = true)
    private void manaita_plus_general$protectCommandSlotAccess(int slot, CallbackInfoReturnable<SlotAccess> cir) {
        SlotAccess original = cir.getReturnValue();
        if (original == SlotAccess.NULL) {
            return;
        }

        Player player = (Player) (Object) this;
        cir.setReturnValue(new SlotAccess() {
            @Override
            public ItemStack get() {
                return original.get();
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!MPUtils.isProtectedFromForcedRemoval(player)) {
                    return original.set(stack);
                }

                return original.get().isEmpty() && !stack.isEmpty() && original.set(stack);
            }
        });
    }
}
