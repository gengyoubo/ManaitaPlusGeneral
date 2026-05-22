package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GiveCommand.class)
public class GiveCommandMixin {
    @Redirect(
            method = "giveItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private static boolean manaita_plus_general$protectFromGiveCommand(Inventory inventory, ItemStack stack) {
        if (MPUtils.isProtectedFromForcedRemoval(inventory.player)) {
            return MPUtils.addToEmptyInventorySlot(inventory, stack);
        }

        return inventory.add(stack);
    }
}
