package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.item.armor.MPGArmor;
import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @ModifyVariable(method = "clearOrCountMatchingItems", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Predicate<ItemStack> manaita_plus_general$protectManaitaArmorFromClear(Predicate<ItemStack> predicate) {
        if (MPUtils.isProtectedFromForcedRemoval(this.player)) {
            return stack -> false;
        }

        return stack -> !(stack.getItem() instanceof MPGArmor) && predicate.test(stack);
    }

    @Inject(method = "dropAll", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectInventoryFromForcedDrop(CallbackInfo ci) {
        if (MPUtils.isProtectedFromForcedRemoval(this.player)) {
            ci.cancel();
        }
    }
}
