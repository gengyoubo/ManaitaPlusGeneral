package github.com.gengyoubo.MPG.mixin;

import github.com.gengyoubo.MPG.util.MPUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectFromKill(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (MPUtils.isProtectedFromForcedRemoval(entity)) {
            restoreProtectedPlayer(entity);
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void manaita_plus_general$protectFromRemove(Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if ((removalReason == Entity.RemovalReason.KILLED || removalReason == Entity.RemovalReason.DISCARDED)
                && entity instanceof Player
                && MPUtils.isProtectedFromForcedRemoval(entity)) {
            restoreProtectedPlayer(entity);
            ci.cancel();
        }
    }

    private static void restoreProtectedPlayer(Entity entity) {
        if (entity instanceof Player player) {
            player.setHealth(player.getMaxHealth());
            player.deathTime = 0;
            player.hurtTime = 0;
        }
    }
}
